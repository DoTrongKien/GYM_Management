package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.RatingRequest;
import com.example.gymmanagement.dto.response.RatingResponse;
import com.example.gymmanagement.entity.ServiceRating;
import com.example.gymmanagement.entity.User;
import com.example.gymmanagement.repository.ServiceRatingRepository;
import com.example.gymmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final ServiceRatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    // ─────────────────────────────────────────────
    // USER ADD RATING
    // ─────────────────────────────────────────────
    public RatingResponse addRating(String email, RatingRequest request, MultipartFile file) {

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Số sao phải từ 1 đến 5");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ServiceRating rating = ServiceRating.builder()
                .user(user)
                .rating(request.getRating())
                .title(blankToNull(request.getTitle()))
                .comment(blankToNull(request.getComment()))
                .serviceType(blankToNull(request.getServiceType()))
                .isPublic(
                        request.getIsPublic() != null
                                ? request.getIsPublic()
                                : true
                )
                .createdAt(LocalDateTime.now())
                .build();

        if (file != null && !file.isEmpty()) {
            applyAttachment(rating, fileStorageService.store(file, "ratings"));
        }

        ratingRepository.save(rating);
        notifyAdminsNewRating(rating);
        return toResponse(rating);
    }

    /** Báo cho mọi admin biết vừa có đánh giá mới. */
    private void notifyAdminsNewRating(ServiceRating rating) {
        String name = rating.getUser().getFullName() != null
                ? rating.getUser().getFullName() : rating.getUser().getEmail();

        String title = "⭐ Có đánh giá mới";
        StringBuilder msg = new StringBuilder(name)
                .append(" vừa đánh giá ").append(serviceLabel(rating.getServiceType()))
                .append(" ").append(rating.getRating()).append("/5 sao");
        // Ưu tiên tiêu đề user đặt, không có thì lấy nhận xét
        String preview = rating.getTitle() != null ? rating.getTitle() : rating.getComment();
        if (preview != null && !preview.isBlank()) {
            msg.append(": \"").append(shorten(preview, 80)).append("\"");
        } else if (rating.getAttachmentUrl() != null) {
            msg.append(" 📎 ").append(rating.getAttachmentName() != null
                    ? rating.getAttachmentName() : "Tệp đính kèm");
        }

        userRepository.findAllActiveAdmins().forEach(admin ->
                notificationService.sendToUser(admin.getId(), title, msg.toString(), "SYSTEM",
                        "RATING", rating.getId()));
    }

    /** Tên tiếng Việt của loại dịch vụ, dùng trong thông báo. */
    private String serviceLabel(String type) {
        if (type == null) return "dịch vụ";
        switch (type) {
            case "WORKOUT_PLAN": return "Giáo án";
            case "NUTRITION":    return "Dinh dưỡng";
            case "FACILITY":     return "Cơ sở vật chất";
            case "TRAINER":      return "Huấn luyện viên";
            default:             return type;
        }
    }

    /** Cắt bớt nhận xét dài để thông báo không quá dài. */
    private String shorten(String text, int max) {
        String s = text.trim();
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    /** Form multipart luôn gửi chuỗi rỗng thay vì bỏ trống → quy về null cho các trường tùy chọn. */
    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Gán thông tin file vừa lưu vào đánh giá. */
    private void applyAttachment(ServiceRating rating, FileStorageService.Stored stored) {
        rating.setAttachmentUrl(stored.getUrl());
        rating.setAttachmentName(stored.getName());
        rating.setAttachmentType(stored.getType());
        rating.setAttachmentSize(stored.getSize());
    }

    /** Gỡ file đính kèm khỏi đánh giá. */
    private void clearAttachment(ServiceRating rating) {
        rating.setAttachmentUrl(null);
        rating.setAttachmentName(null);
        rating.setAttachmentType(null);
        rating.setAttachmentSize(null);
    }

    // ─────────────────────────────────────────────
    // USER UPDATE RATING (chỉ đánh giá của chính mình)
    // ─────────────────────────────────────────────
    public RatingResponse updateRating(String email, Long ratingId, RatingRequest request,
                                       MultipartFile file, boolean removeAttachment) {

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Số sao phải từ 1 đến 5");
        }

        ServiceRating rating = getOwnedRating(email, ratingId);

        rating.setRating(request.getRating());
        rating.setTitle(blankToNull(request.getTitle()));
        rating.setComment(blankToNull(request.getComment()));
        rating.setServiceType(blankToNull(request.getServiceType()));   // cho phép bỏ chọn dịch vụ
        if (request.getIsPublic() != null) rating.setIsPublic(request.getIsPublic());
        rating.setUpdatedAt(LocalDateTime.now());

        // File mới thay file cũ; nếu không có file mới thì mới xét việc gỡ
        if (file != null && !file.isEmpty()) {
            applyAttachment(rating, fileStorageService.store(file, "ratings"));
        } else if (removeAttachment) {
            clearAttachment(rating);
        }

        return toResponse(ratingRepository.save(rating));
    }

    // ─────────────────────────────────────────────
    // USER DELETE RATING (chỉ đánh giá của chính mình)
    // ─────────────────────────────────────────────
    public void deleteRating(String email, Long ratingId) {
        ratingRepository.delete(getOwnedRating(email, ratingId));
    }

    /** Lấy đánh giá và kiểm tra nó thuộc về user đang đăng nhập. */
    private ServiceRating getOwnedRating(String email, Long ratingId) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ServiceRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        if (rating.getUser() == null || !rating.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền với đánh giá này");
        }
        return rating;
    }

    // ─────────────────────────────────────────────
    // PUBLIC RATINGS
    // ─────────────────────────────────────────────
    public List<RatingResponse> getPublicRatings() {

        return ratingRepository
                .findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // MY RATINGS
    // ─────────────────────────────────────────────
    public List<RatingResponse> getMyRatings(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ratingRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // ADMIN GET ALL RATINGS
    // ─────────────────────────────────────────────
    public List<RatingResponse> getAllRatings() {

        return ratingRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                ServiceRating::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // ADMIN REPLY (kèm file đính kèm tùy chọn)
    // ─────────────────────────────────────────────
    public RatingResponse adminReply(Long ratingId, String reply, MultipartFile file, boolean removeAttachment) {

        ServiceRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        String text = reply != null ? reply.trim() : "";
        boolean hasNewFile = file != null && !file.isEmpty();
        // File cũ chỉ còn lại nếu admin không gỡ và cũng không thay bằng file mới
        boolean keepsOldFile = rating.getReplyAttachmentUrl() != null && !removeAttachment;

        // Phản hồi phải có nội dung, hoặc ít nhất một file đính kèm
        if (text.isEmpty() && !hasNewFile && !keepsOldFile) {
            throw new RuntimeException("Phản hồi không được để trống");
        }

        rating.setAdminReply(text.isEmpty() ? null : text);
        rating.setRepliedAt(LocalDateTime.now());

        if (hasNewFile) {
            FileStorageService.Stored stored = fileStorageService.store(file, "ratings");
            rating.setReplyAttachmentUrl(stored.getUrl());
            rating.setReplyAttachmentName(stored.getName());
            rating.setReplyAttachmentType(stored.getType());
            rating.setReplyAttachmentSize(stored.getSize());
        } else if (removeAttachment) {
            rating.setReplyAttachmentUrl(null);
            rating.setReplyAttachmentName(null);
            rating.setReplyAttachmentType(null);
            rating.setReplyAttachmentSize(null);
        }

        ratingRepository.save(rating);

        // gửi notification
        notificationService.sendToUser(
                rating.getUser().getId(),
                "💬 Admin đã phản hồi đánh giá của bạn",
                !text.isEmpty() ? text
                        : "📎 " + (rating.getReplyAttachmentName() != null
                                ? rating.getReplyAttachmentName() : "Tệp đính kèm"),
                "SYSTEM",
                "RATING", rating.getId()
        );

        return toResponse(rating);
    }

    // ─────────────────────────────────────────────
    // ADMIN DELETE RATING
    // ─────────────────────────────────────────────
    public void adminDeleteRating(Long ratingId) {
        ServiceRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
        ratingRepository.delete(rating);
    }

    // ─────────────────────────────────────────────
    // AVERAGE RATINGS
    // ─────────────────────────────────────────────
    public Map<String, Double> getAverageRatings() {

        return Map.of(
                "WORKOUT_PLAN", getAvg("WORKOUT_PLAN"),
                "NUTRITION", getAvg("NUTRITION"),
                "FACILITY", getAvg("FACILITY"),
                "TRAINER", getAvg("TRAINER")
        );
    }

    private double getAvg(String type) {

        Double avg = ratingRepository.getAverageRatingByType(type);

        return avg != null
                ? Math.round(avg * 10.0) / 10.0
                : 0.0;
    }

    // ─────────────────────────────────────────────
    // CONVERT ENTITY -> RESPONSE
    // ─────────────────────────────────────────────
    public RatingResponse toResponse(ServiceRating r) {

        return RatingResponse.builder()
                .id(r.getId())

                .userId(
                        r.getUser() != null
                                ? r.getUser().getId()
                                : null
                )

                .userName(
                        r.getUser() != null
                                ? r.getUser().getFullName()
                                : "Ẩn danh"
                )

                .userEmail(
                        r.getUser() != null
                                ? r.getUser().getEmail()
                                : ""
                )

                .rating(r.getRating())
                .title(r.getTitle())
                .comment(r.getComment())
                .serviceType(r.getServiceType())
                .isPublic(r.getIsPublic())

                .attachmentUrl(r.getAttachmentUrl())
                .attachmentName(r.getAttachmentName())
                .attachmentType(r.getAttachmentType())
                .attachmentSize(r.getAttachmentSize())

                .adminReply(r.getAdminReply())
                .repliedAt(r.getRepliedAt())

                .replyAttachmentUrl(r.getReplyAttachmentUrl())
                .replyAttachmentName(r.getReplyAttachmentName())
                .replyAttachmentType(r.getReplyAttachmentType())
                .replyAttachmentSize(r.getReplyAttachmentSize())

                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())

                .build();
    }
}