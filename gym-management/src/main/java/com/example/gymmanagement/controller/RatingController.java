package com.example.gymmanagement.controller;

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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // User viết đánh giá (kèm file đính kèm tùy chọn)
    @PostMapping
    public ResponseEntity<ApiResponse<RatingResponse>> addRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute RatingRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.addRating(userDetails.getUsername(), request, file),
                "Cảm ơn bạn đã đánh giá!"));
    }

    // User sửa đánh giá của chính mình (kèm file đính kèm tùy chọn)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RatingResponse>> updateRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @ModelAttribute RatingRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "removeAttachment", required = false, defaultValue = "false") boolean removeAttachment) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.updateRating(userDetails.getUsername(), id, request, file, removeAttachment),
                "Đã cập nhật đánh giá"));
    }

    // User xóa đánh giá của chính mình
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ratingService.deleteRating(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa đánh giá", "OK"));
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

    // Admin phản hồi đánh giá (kèm file đính kèm tùy chọn)
    @PostMapping("/admin/{id}/reply")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<RatingResponse>> adminReply(
            @PathVariable Long id,
            @RequestParam(value = "reply", required = false) String reply,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "removeAttachment", required = false, defaultValue = "false") boolean removeAttachment) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.adminReply(id, reply, file, removeAttachment), "Đã gửi phản hồi!"));
    }

    // Admin xóa đánh giá của bất kỳ ai
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminDeleteRating(@PathVariable Long id) {
        ratingService.adminDeleteRating(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa đánh giá", "OK"));
    }
}