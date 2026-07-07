package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.dto.request.WorkoutTemplateRequest;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.PaymentStatus;
import com.example.gymmanagement.repository.*;
import com.example.gymmanagement.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final MembershipRepository membershipRepository;
    private final DashboardService dashboardService;
    private final NotificationService notificationService;
    private final WorkoutPlanService workoutPlanService;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final WorkoutPlanDayRepository workoutPlanDayRepository;
    private final UserProfileService userProfileService;
    private final BCryptPasswordEncoder passwordEncoder;

    // ─── Dashboard ───────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getAdminDashboard() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getAdminDashboard()));
    }

    // ─── User Management ─────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("fullName", u.getFullName() != null ? u.getFullName() : "");
            m.put("email", u.getEmail());
            m.put("phone", u.getPhone() != null ? u.getPhone() : "");
            m.put("status", u.getStatus());
            m.put("emailVerified", u.getEmailVerified());
            m.put("role", u.getRole().getRoleName());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", u.getId());
        data.put("fullName", u.getFullName() != null ? u.getFullName() : "");
        data.put("email", u.getEmail());
        data.put("phone", u.getPhone() != null ? u.getPhone() : "");
        data.put("status", u.getStatus());
        data.put("emailVerified", u.getEmailVerified());
        data.put("role", u.getRole().getRoleName());
        data.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(body.get("status"));
        userRepository.save(user);
        String msg = Boolean.TRUE.equals(body.get("status")) ? "User activated" : "User deactivated";
        return ResponseEntity.ok(ApiResponse.success(null, msg));
    }

    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(false);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted (deactivated)"));
    }

    // ─── Membership / Invoice Management ─────────────────────
    @GetMapping("/memberships")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getAllMemberships() {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getAllMemberships()));
    }

    @GetMapping("/memberships/pending")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getPendingPayments() {
        List<MembershipResponse> pending = membershipRepository.findByPaymentStatus(PaymentStatus.PENDING)
                .stream().map(membershipService::buildResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @PostMapping("/memberships/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<MembershipResponse>> adminConfirmPayment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String txId = body != null ? body.getOrDefault("transactionId", "ADMIN-" + id) : "ADMIN-" + id;
        return ResponseEntity.ok(ApiResponse.success(
                membershipService.confirmPayment(id, txId), "Payment confirmed"));
    }

    @PutMapping("/memberships/{id}/refund")
    public ResponseEntity<ApiResponse<Void>> refundMembership(@PathVariable Long id) {
        Membership m = membershipRepository.findById(id).orElseThrow(() -> new RuntimeException("Membership not found"));
        m.setPaymentStatus(PaymentStatus.REFUNDED);
        m.setIsActive(false);
        membershipRepository.save(m);
        return ResponseEntity.ok(ApiResponse.success(null, "Membership refunded"));
    }

    @GetMapping("/memberships/user/{userId}")
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMembershipsOfUser(@PathVariable Long userId) {
        List<MembershipResponse> list = membershipRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(membershipService::buildResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // ─── Revenue / Statistics ─────────────────────────────────
    @GetMapping("/stats/revenue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRevenueStats() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime monthStart = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
        java.time.LocalDateTime yearStart = java.time.LocalDate.now().withDayOfYear(1).atStartOfDay();

        Double totalRevenue = membershipRepository.sumRevenueBetween(
                java.time.LocalDate.of(2020,1,1).atStartOfDay(), now);
        Double monthRevenue = membershipRepository.sumRevenueBetween(monthStart, now);
        Double yearRevenue  = membershipRepository.sumRevenueBetween(yearStart, now);

        long totalMembers = membershipRepository.count();
        long paidMembers  = membershipRepository.findByPaymentStatus(PaymentStatus.PAID).size();

        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalRevenue",   totalRevenue  != null ? totalRevenue  : 0.0);
        stats.put("monthRevenue",   monthRevenue  != null ? monthRevenue  : 0.0);
        stats.put("yearRevenue",    yearRevenue   != null ? yearRevenue   : 0.0);
        stats.put("totalMembers",   totalMembers);
        stats.put("paidMembers",    paidMembers);
        stats.put("totalUsers",     userRepository.count());
        stats.put("activeUsers",    userRepository.findAllActiveUsers().size());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    // ─── Notifications / Broadcast ────────────────────────────
    @PostMapping("/notifications/broadcast")
    public ResponseEntity<ApiResponse<Void>> broadcast(@RequestBody Map<String, String> body) {
        notificationService.sendBroadcast(body.get("title"), body.get("message"),
                body.getOrDefault("type", "PROMOTION"));
        return ResponseEntity.ok(ApiResponse.success(null, "Broadcast sent to all active users"));
    }

    @PostMapping("/notifications/user/{userId}")
    public ResponseEntity<ApiResponse<Void>> sendToUser(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        notificationService.sendToUser(userId, body.get("title"), body.get("message"),
                body.getOrDefault("type", "SYSTEM"));
        return ResponseEntity.ok(ApiResponse.success(null, "Notification sent"));
    }

    // ─── User Profile (read-only) ─────────────────────────────
    @GetMapping("/users/{id}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userProfileService.getProfileById(id)));
    }
}