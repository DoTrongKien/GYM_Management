package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.CancelMembershipRequest;
import com.example.gymmanagement.dto.request.MembershipRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.service.MembershipCancellationService;
import com.example.gymmanagement.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;
    private final MembershipCancellationService cancellationService;

    @PostMapping
    public ResponseEntity<ApiResponse<MembershipResponse>> purchase(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MembershipRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipService.purchase(userDetails.getUsername(), request),
                "Membership created! Please complete payment."));
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<MembershipResponse>> confirmPayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipService.confirmPayment(id, body.get("transactionId")),
                "Payment confirmed! Membership activated."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipResponse>>> getMyMemberships(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getMyMemberships(userDetails.getUsername())));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<MembershipResponse>> getActiveMembership(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getActiveMembership(userDetails.getUsername())));
    }

    // ─── TH3: User gửi yêu cầu hủy gói đã thanh toán ──────────────────────
    @PostMapping("/{id}/cancel-request")
    public ResponseEntity<ApiResponse<CancellationRequestResponse>> requestCancel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) CancelMembershipRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.success(
                cancellationService.requestCancellation(userDetails.getUsername(), id, reason),
                "Đã gửi yêu cầu hủy gói. Admin sẽ xem xét và phản hồi sớm nhất."));
    }

    @GetMapping("/cancel-requests")
    public ResponseEntity<ApiResponse<List<CancellationRequestResponse>>> myCancelRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(cancellationService.getMyRequests(userDetails.getUsername())));
    }
}