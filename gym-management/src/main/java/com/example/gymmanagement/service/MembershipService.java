package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.MembershipRequest;
import com.example.gymmanagement.dto.response.MembershipResponse;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final Map<MembershipType, Double> PRICES = Map.of(
            MembershipType.BASIC, 299000.0,
            MembershipType.STANDARD, 499000.0,
            MembershipType.PREMIUM, 799000.0,
            MembershipType.VIP, 1299000.0
    );

    private static final Map<MembershipType, Integer> DURATIONS_MONTHS = Map.of(
            MembershipType.BASIC, 1,
            MembershipType.STANDARD, 3,
            MembershipType.PREMIUM, 6,
            MembershipType.VIP, 12
    );

    public double getPrice(MembershipType type) {
        return PRICES.getOrDefault(type, 299000.0);
    }

    public int getDurationMonths(MembershipType type) {
        return DURATIONS_MONTHS.getOrDefault(type, 1);
    }

    /**
     * Dùng bởi InvoiceService: khi MoMo IPN xác nhận PAID, tạo thẳng 1 Membership
     * ở trạng thái PAID/active (khác với purchase() cũ luôn tạo PENDING).
     * Gói active cũ (nếu có) bị vô hiệu hóa nhưng KHÔNG xóa, để có thể khôi phục lại
     * khi user hủy gói mới trong thời hạn cho phép (xem MembershipCancellationService).
     */
    @Transactional
    public Membership activatePaidMembership(User user, MembershipType type, String transactionId, String paymentMethod) {
        membershipRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(m -> {
            m.setIsActive(false);
            membershipRepository.save(m);
        });

        LocalDate start = LocalDate.now();
        int months = getDurationMonths(type);
        LocalDate end = start.plusMonths(months);

        Membership membership = Membership.builder()
                .user(user)
                .membershipType(type)
                .startDate(start)
                .endDate(end)
                .price(getPrice(type))
                .isActive(true)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(paymentMethod)
                .transactionId(transactionId)
                .paidAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        emailService.sendMembershipConfirmation(
                user.getEmail(), user.getFullName(), type.name(), end.toString());

        return membership;
    }

    @Transactional
    public MembershipResponse purchase(String email, MembershipRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Deactivate current membership
        membershipRepository.findByUserIdAndIsActiveTrue(user.getId()).ifPresent(m -> {
            m.setIsActive(false);
            membershipRepository.save(m);
        });

        LocalDate start = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        int months = DURATIONS_MONTHS.getOrDefault(request.getMembershipType(), 1);
        LocalDate end = start.plusMonths(months);
        double price = PRICES.getOrDefault(request.getMembershipType(), 299000.0);

        Membership membership = Membership.builder()
                .user(user)
                .membershipType(request.getMembershipType())
                .startDate(start)
                .endDate(end)
                .price(price)
                .isActive(true)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();
        membershipRepository.save(membership);

        return buildResponse(membership);
    }

    @Transactional
    public MembershipResponse confirmPayment(Long membershipId, String transactionId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));
        membership.setPaymentStatus(PaymentStatus.PAID);
        membership.setTransactionId(transactionId);
        membership.setPaidAt(LocalDateTime.now());
        membershipRepository.save(membership);

        emailService.sendMembershipConfirmation(
                membership.getUser().getEmail(),
                membership.getUser().getFullName(),
                membership.getMembershipType().name(),
                membership.getEndDate().toString()
        );

        return buildResponse(membership);
    }

    public List<MembershipResponse> getMyMemberships(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return membershipRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public MembershipResponse getActiveMembership(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return membershipRepository.findByUserIdAndIsActiveTrue(user.getId())
                .map(this::buildResponse)
                .orElseThrow(() -> new RuntimeException("No active membership found"));
    }

    // Admin methods
    public List<MembershipResponse> getAllMemberships() {
        return membershipRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    public MembershipResponse buildResponse(Membership m) {
        int daysRemaining = 0;
        if (m.getEndDate() != null && m.getIsActive()) {
            daysRemaining = (int) LocalDate.now().until(m.getEndDate(), java.time.temporal.ChronoUnit.DAYS);
        }
        return MembershipResponse.builder()
                .id(m.getId())
                .userId(m.getUser().getId())
                .userName(m.getUser().getFullName())
                .userEmail(m.getUser().getEmail())
                .membershipType(m.getMembershipType())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .price(m.getPrice())
                .isActive(m.getIsActive())
                .paymentStatus(m.getPaymentStatus())
                .paymentMethod(m.getPaymentMethod())
                .paidAt(m.getPaidAt())
                .createdAt(m.getCreatedAt())
                .daysRemaining(daysRemaining)
                .build();
    }
}