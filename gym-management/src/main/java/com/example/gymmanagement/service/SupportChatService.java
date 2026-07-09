package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.response.SupportMessageResponse;
import com.example.gymmanagement.dto.response.SupportSessionResponse;
import com.example.gymmanagement.entity.SupportMessage;
import com.example.gymmanagement.entity.SupportSession;
import com.example.gymmanagement.entity.User;
import com.example.gymmanagement.enums.SupportStatus;
import com.example.gymmanagement.repository.SupportMessageRepository;
import com.example.gymmanagement.repository.SupportSessionRepository;
import com.example.gymmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Quản lý chat 1:1 giữa user và admin: user gửi yêu cầu → admin chấp nhận /
 * từ chối → chat 1:1 → kết thúc phiên.
 */
@Service
@RequiredArgsConstructor
public class SupportChatService {

    private final SupportSessionRepository sessionRepository;
    private final SupportMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    private static final List<SupportStatus> OPEN = Arrays.asList(SupportStatus.PENDING, SupportStatus.ACTIVE);

    // ══════════════════════════════════════════════
    //  USER
    // ══════════════════════════════════════════════

    /** Số cuộc hội thoại đang mở tối đa mỗi user (chống spam). */
    private static final int MAX_OPEN_PER_USER = 5;

    /** User tạo một cuộc hội thoại mới với admin (cho phép nhiều cuộc về các vấn đề riêng). */
    @Transactional
    public SupportSessionResponse requestChat(String email, String subject, String content, MultipartFile file) {
        User user = getUser(email);
        long openCount = sessionRepository
                .findByUserIdAndStatusInOrderByCreatedAtDesc(user.getId(), OPEN).size();
        if (openCount >= MAX_OPEN_PER_USER) {
            throw new RuntimeException("Bạn đang mở quá nhiều cuộc hội thoại. Hãy kết thúc bớt trước khi tạo mới.");
        }
        String subj = (subject == null || subject.trim().isEmpty()) ? "Hỗ trợ chung" : subject.trim();
        SupportSession session = SupportSession.builder()
                .user(user)
                .subject(subj)
                .status(SupportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        sessionRepository.save(session);
        // Nội dung user trình bày → lưu làm tin nhắn mở đầu (admin thấy ngay trong thông báo)
        if (content != null && !content.trim().isEmpty()) {
            saveMessage(session, "USER", content);
        }
        // File đính kèm (nếu có) — lưu ngay lúc gửi yêu cầu, kể cả khi phiên còn PENDING
        if (file != null && !file.isEmpty()) {
            saveAttachment(session, "USER", file, null);
        }
        return toSessionResponse(session);
    }

    /** Danh sách các cuộc hội thoại đang mở của user (PENDING/ACTIVE). */
    public List<SupportSessionResponse> getMySessions(String email) {
        User user = getUser(email);
        return sessionRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(user.getId(), OPEN)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    public List<SupportMessageResponse> getMyMessages(String email, Long sessionId) {
        SupportSession session = getOwnedSession(email, sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public SupportMessageResponse sendUserMessage(String email, Long sessionId, String content) {
        SupportSession session = getOwnedSession(email, sessionId);
        if (session.getStatus() != SupportStatus.ACTIVE) {
            throw new RuntimeException("Đang chờ admin xác nhận, chưa thể gửi tin nhắn");
        }
        return saveMessage(session, "USER", content);
    }

    /** User gửi file đính kèm (kèm chú thích tùy chọn). */
    @Transactional
    public SupportMessageResponse sendUserAttachment(String email, Long sessionId, MultipartFile file, String caption) {
        SupportSession session = getOwnedSession(email, sessionId);
        if (session.getStatus() != SupportStatus.ACTIVE) {
            throw new RuntimeException("Đang chờ admin xác nhận, chưa thể gửi tin nhắn");
        }
        return saveAttachment(session, "USER", file, caption);
    }

    /** User tự kết thúc một cuộc hội thoại của mình. */
    @Transactional
    public void closeByUser(String email, Long sessionId) {
        SupportSession session = getOwnedSession(email, sessionId);
        session.setStatus(SupportStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    /** Lấy phiên và đảm bảo nó thuộc về user hiện tại. */
    private SupportSession getOwnedSession(String email, Long sessionId) {
        User user = getUser(email);
        SupportSession session = getSession(sessionId);
        if (session.getUser() == null || !session.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Không tìm thấy phiên chat");
        }
        return session;
    }

    // ══════════════════════════════════════════════
    //  ADMIN
    // ══════════════════════════════════════════════

    /** Danh sách các phiên đang chờ + đang hoạt động (cho admin). */
    public List<SupportSessionResponse> listOpenSessions() {
        return sessionRepository.findByStatusInOrderByCreatedAtDesc(OPEN)
                .stream()
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SupportSessionResponse accept(String adminEmail, Long sessionId) {
        User admin = getUser(adminEmail);
        SupportSession session = getSession(sessionId);
        if (session.getStatus() != SupportStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }
        session.setStatus(SupportStatus.ACTIVE);
        session.setAdmin(admin);
        session.setAcceptedAt(LocalDateTime.now());
        sessionRepository.save(session);
        // Tin nhắn chào mừng — hiển thị TRÊN nội dung user trình bày:
        // gán createdAt sớm hơn tin nhắn đầu tiên 1 giây để nó luôn sắp lên đầu.
        LocalDateTime firstMsg = firstMessageTime(session.getId());
        LocalDateTime greetingTime = (firstMsg != null ? firstMsg : session.getCreatedAt()).minusSeconds(1);
        saveMessageAt(session, "ADMIN", "Xin chào! Admin đã tham gia cuộc trò chuyện, mình có thể giúp gì cho bạn?", greetingTime);
        return toSessionResponse(session);
    }

    @Transactional
    public void reject(Long sessionId) {
        SupportSession session = getSession(sessionId);
        if (session.getStatus() != SupportStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }
        session.setStatus(SupportStatus.REJECTED);
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void close(Long sessionId) {
        SupportSession session = getSession(sessionId);
        session.setStatus(SupportStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    public List<SupportMessageResponse> getSessionMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Transactional
    public SupportMessageResponse sendAdminMessage(Long sessionId, String content) {
        SupportSession session = getSession(sessionId);
        if (session.getStatus() != SupportStatus.ACTIVE) {
            throw new RuntimeException("Phiên chat không còn hoạt động");
        }
        return saveMessage(session, "ADMIN", content);
    }

    /** Admin gửi file đính kèm (kèm chú thích tùy chọn). */
    @Transactional
    public SupportMessageResponse sendAdminAttachment(Long sessionId, MultipartFile file, String caption) {
        SupportSession session = getSession(sessionId);
        if (session.getStatus() != SupportStatus.ACTIVE) {
            throw new RuntimeException("Phiên chat không còn hoạt động");
        }
        return saveAttachment(session, "ADMIN", file, caption);
    }

    // ══════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════

    private SupportMessageResponse saveMessage(SupportSession session, String role, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Tin nhắn không được để trống");
        }
        return saveMessageAt(session, role, content, LocalDateTime.now());
    }

    /** Lưu tin nhắn với thời điểm chỉ định (dùng để ép thứ tự hiển thị). */
    private SupportMessageResponse saveMessageAt(SupportSession session, String role, String content, LocalDateTime createdAt) {
        SupportMessage msg = SupportMessage.builder()
                .session(session)
                .senderRole(role)
                .content(content.trim())
                .createdAt(createdAt)
                .build();
        messageRepository.save(msg);
        return toMessageResponse(msg);
    }

    /** Thời điểm tin nhắn sớm nhất của phiên (null nếu chưa có tin nhắn nào). */
    private LocalDateTime firstMessageTime(Long sessionId) {
        List<SupportMessage> list = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return list.isEmpty() ? null : list.get(0).getCreatedAt();
    }

    private SupportMessageResponse saveAttachment(SupportSession session, String role, MultipartFile file, String caption) {
        FileStorageService.Stored stored = fileStorageService.store(file, "support");
        SupportMessage msg = SupportMessage.builder()
                .session(session)
                .senderRole(role)
                .content(caption != null && !caption.trim().isEmpty() ? caption.trim() : null)
                .attachmentUrl(stored.getUrl())
                .attachmentName(stored.getName())
                .attachmentType(stored.getType())
                .attachmentSize(stored.getSize())
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(msg);
        return toMessageResponse(msg);
    }

    /** Tin nhắn cuối cùng của phiên (null nếu chưa có). */
    private SupportMessage lastMessageEntity(Long sessionId) {
        List<SupportMessage> list = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /** Rút gọn tin nhắn cuối thành text để hiển thị trong danh sách / thông báo. */
    private String lastMessageText(SupportMessage last) {
        if (last == null) return null;
        if (last.getContent() != null && !last.getContent().isEmpty()) return last.getContent();
        if (last.getAttachmentUrl() != null) return "📎 " + (last.getAttachmentName() != null ? last.getAttachmentName() : "Tệp đính kèm");
        return null;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private SupportSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên chat"));
    }

    private SupportSessionResponse toSessionResponse(SupportSession s) {
        SupportMessage last = lastMessageEntity(s.getId());
        return SupportSessionResponse.builder()
                .id(s.getId())
                .userId(s.getUser() != null ? s.getUser().getId() : null)
                .userName(s.getUser() != null ? s.getUser().getFullName() : null)
                .userEmail(s.getUser() != null ? s.getUser().getEmail() : null)
                .subject(s.getSubject())
                .adminName(s.getAdmin() != null ? s.getAdmin().getFullName() : null)
                .status(s.getStatus().name())
                .createdAt(s.getCreatedAt())
                .acceptedAt(s.getAcceptedAt())
                .lastMessage(lastMessageText(last))
                .lastMessageAt(last != null ? last.getCreatedAt() : null)
                .lastMessageRole(last != null ? last.getSenderRole() : null)
                .build();
    }

    private SupportMessageResponse toMessageResponse(SupportMessage m) {
        return SupportMessageResponse.builder()
                .id(m.getId())
                .senderRole(m.getSenderRole())
                .content(m.getContent())
                .attachmentUrl(m.getAttachmentUrl())
                .attachmentName(m.getAttachmentName())
                .attachmentType(m.getAttachmentType())
                .attachmentSize(m.getAttachmentSize())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
