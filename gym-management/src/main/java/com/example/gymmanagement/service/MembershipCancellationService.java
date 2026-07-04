package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.response.CancellationRequestResponse;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipCancellationService {

    @Value("${app.membership.cancel-window-days:7}")
    private int cancelWindowDays;

    private final MembershipCancellationRequestRepository cancellationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // ─── TH3 (phía User): gửi yêu cầu hủy gói đã thanh toán ──────────────
    @Transactional
    public CancellationRequestResponse requestCancellation(String email, Long membershipId, String reason) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));

        if (!membership.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền với gói tập này");
        }
        if (membership.getPaymentStatus() != PaymentStatus.PAID) {
            throw new RuntimeException("Chỉ có thể yêu cầu hủy gói đã thanh toán thành công");
        }
        cancellationRepository.findByMembershipIdAndStatus(membershipId, CancellationStatus.PENDING_REVIEW)
                .ifPresent(r -> { throw new RuntimeException("Gói này đã có 1 yêu cầu hủy đang chờ admin xử lý"); });

        MembershipCancellationRequest request = MembershipCancellationRequest.builder()
                .membership(membership)
                .user(user)
                .reason(reason)
                .status(CancellationStatus.PENDING_REVIEW)
                .build();
        cancellationRepository.save(request);

        // Thông báo cho toàn bộ admin để xử lý
        userRepository.findAllAdmins().forEach(admin ->
                notificationService.sendToUser(admin.getId(),
                        "Yêu cầu hủy gói tập mới",
                        user.getFullName() + " yêu cầu hủy gói " + membership.getMembershipType() +
                                " (mã #" + membership.getId() + "). Lý do: " + (reason == null || reason.isBlank() ? "(không có)" : reason),
                        "SYSTEM"));

        return buildResponse(request);
    }

    // ─── Kiểm tra điều kiện được phép hủy (dùng cả khi hiển thị cho admin lẫn khi duyệt) ─
    private Optional<String> checkIneligibleReason(Membership membership) {
        if (membership.getPaidAt() == null) {
            return Optional.of("Gói không có thời điểm thanh toán hợp lệ");
        }
        LocalDateTime deadline = membership.getPaidAt().plusDays(cancelWindowDays);
        if (LocalDateTime.now().isAfter(deadline)) {
            return Optional.of("Đã quá thời hạn cho phép hủy (" + cancelWindowDays + " ngày kể từ khi thanh toán)");
        }

        boolean hasCheckedInSession = workoutSessionRepository.findByUserIdOrderBySessionDateDesc(membership.getUser().getId())
                .stream().anyMatch(s -> s.getCheckInTime() != null && s.getCheckInTime().isAfter(membership.getPaidAt()));
        if (hasCheckedInSession) {
            return Optional.of("User đã check-in ít nhất 1 buổi tập kể từ khi mua gói");
        }

        boolean hasCreatedPlan = workoutPlanRepository.findByUserIdOrderByCreatedAtDesc(membership.getUser().getId())
                .stream().anyMatch(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(membership.getPaidAt()));
        if (hasCreatedPlan) {
            return Optional.of("User đã tạo giáo án tập luyện kể từ khi mua gói");
        }

        return Optional.empty();
    }

    // ─── Admin TH1: Hủy gói thành công ────────────────────────────────────
    @Transactional
    public CancellationRequestResponse approve(Long requestId, String adminEmail, String adminNote) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        MembershipCancellationRequest request = cancellationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Cancellation request not found"));

        if (request.getStatus() != CancellationStatus.PENDING_REVIEW) {
            throw new RuntimeException("Yêu cầu này đã được xử lý rồi");
        }

        Membership membership = request.getMembership();

        // Chặn duyệt nếu vi phạm điều kiện (TH2) - admin nên REJECT thay vì APPROVE ở đây
        Optional<String> ineligible = checkIneligibleReason(membership);
        if (ineligible.isPresent()) {
            throw new RuntimeException("Không thể duyệt hủy: " + ineligible.get() + ". Vui lòng từ chối yêu cầu này thay vào đó.");
        }

        // 1) Vô hiệu hóa gói hiện tại
        membership.setIsActive(false);
        membership.setPaymentStatus(PaymentStatus.REFUNDED);
        membershipRepository.save(membership);

        // 2) Chuyển khoản ngân hàng KHÔNG có API tự động hoàn tiền như MoMo/ví điện tử.
        //    Admin phải TỰ chuyển khoản hoàn tiền cho user, sau đó hệ thống chỉ ghi nhận
        //    yêu cầu đã được duyệt (refunded=false cho tới khi admin xác nhận đã chuyển tiền).
        boolean refunded = false;

        // 3) Khôi phục gói cũ nếu còn hạn, nếu không thì chuyển về Basic (free)
        restorePreviousOrBasic(membership);

        // 4) Cập nhật request
        request.setStatus(CancellationStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(admin);
        request.setAdminNote(adminNote);
        request.setRefundAmount(membership.getPrice());
        request.setRefunded(refunded);
        cancellationRepository.save(request);

        emailService.sendMembershipConfirmation(membership.getUser().getEmail(), membership.getUser().getFullName(),
                "Đã hủy - hoàn tiền " + membership.getPrice() + "đ", LocalDate.now().toString());
        notificationService.sendToUser(membership.getUser().getId(),
                "Yêu cầu hủy gói đã được duyệt",
                "Gói " + membership.getMembershipType() + " đã được hủy. Admin sẽ chuyển khoản hoàn " +
                        membership.getPrice() + "đ cho bạn trong thời gian sớm nhất.",
                "SYSTEM");

        return buildResponse(request);
    }

    private void restorePreviousOrBasic(Membership cancelledMembership) {
        Long userId = cancelledMembership.getUser().getId();
        List<Membership> history = membershipRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Optional<Membership> restorable = history.stream()
                .filter(m -> !m.getId().equals(cancelledMembership.getId()))
                .filter(m -> m.getPaymentStatus() == PaymentStatus.PAID)
                .filter(m -> m.getEndDate() != null && !m.getEndDate().isBefore(LocalDate.now()))
                .findFirst();

        if (restorable.isPresent()) {
            Membership prev = restorable.get();
            prev.setIsActive(true);
            membershipRepository.save(prev);
            return;
        }

        Membership basic = Membership.builder()
                .user(cancelledMembership.getUser())
                .membershipType(MembershipType.BASIC)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(100)) // gói Basic free, không giới hạn thời gian
                .price(0.0)
                .isActive(true)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod("FREE")
                .paidAt(LocalDateTime.now())
                .notes("Tự động chuyển về Basic (free) sau khi hủy gói trả phí")
                .build();
        membershipRepository.save(basic);
    }

    // ─── Admin TH2: Hủy gói thất bại (từ chối) ────────────────────────────
    @Transactional
    public CancellationRequestResponse reject(Long requestId, String adminEmail, String adminNote) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        MembershipCancellationRequest request = cancellationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Cancellation request not found"));

        if (request.getStatus() != CancellationStatus.PENDING_REVIEW) {
            throw new RuntimeException("Yêu cầu này đã được xử lý rồi");
        }

        String note = adminNote;
        if (note == null || note.isBlank()) {
            note = checkIneligibleReason(request.getMembership()).orElse("Không đủ điều kiện để hủy gói tập");
        }

        request.setStatus(CancellationStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(admin);
        request.setAdminNote(note);
        cancellationRepository.save(request);

        notificationService.sendToUser(request.getUser().getId(),
                "Yêu cầu hủy gói bị từ chối",
                "Yêu cầu hủy gói " + request.getMembership().getMembershipType() + " đã bị từ chối. Lý do: " + note,
                "SYSTEM");

        return buildResponse(request);
    }

    public List<CancellationRequestResponse> getMyRequests(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return cancellationRepository.findByUserIdOrderByRequestedAtDesc(user.getId())
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public List<CancellationRequestResponse> getAllRequests() {
        return cancellationRepository.findAllByOrderByRequestedAtDesc()
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public List<CancellationRequestResponse> getPendingRequests() {
        return cancellationRepository.findByStatusOrderByRequestedAtDesc(CancellationStatus.PENDING_REVIEW)
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    private CancellationRequestResponse buildResponse(MembershipCancellationRequest r) {
        return CancellationRequestResponse.builder()
                .id(r.getId())
                .membershipId(r.getMembership().getId())
                .membershipType(r.getMembership().getMembershipType())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .userEmail(r.getUser().getEmail())
                .reason(r.getReason())
                .status(r.getStatus())
                .requestedAt(r.getRequestedAt())
                .processedAt(r.getProcessedAt())
                .processedByName(r.getProcessedBy() != null ? r.getProcessedBy().getFullName() : null)
                .adminNote(r.getAdminNote())
                .refundAmount(r.getRefundAmount())
                .refunded(r.getRefunded())
                .build();
    }
}