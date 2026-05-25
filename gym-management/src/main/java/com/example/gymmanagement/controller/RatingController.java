package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.AdminReplyRequest;
import com.example.gymmanagement.dto.request.RatingRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.dto.response.RatingResponse;
import com.example.gymmanagement.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<ApiResponse<RatingResponse>> addRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.addRating(userDetails.getUsername(), request),
                "Cảm ơn bạn đã đánh giá!"));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.success(ratingService.getPublicRatings()));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getMyRatings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(ratingService.getMyRatings(userDetails.getUsername())));
    }

    @GetMapping("/averages")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getAverages() {
        return ResponseEntity.ok(ApiResponse.success(ratingService.getAverageRatings()));
    }

    // ── Admin ──────────────────────────────────────────────────
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getAllRatings() {
        return ResponseEntity.ok(ApiResponse.success(ratingService.getAllRatings()));
    }

    @PostMapping("/admin/{id}/reply")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<RatingResponse>> adminReply(
            @PathVariable Long id,
            @RequestBody AdminReplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.adminReply(id, request), "Đã gửi phản hồi!"));
    }
}