package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.RatingRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.entity.ServiceRating;
import com.example.gymmanagement.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<ServiceRating>> addRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                ratingService.addRating(userDetails.getUsername(), request), "Thank you for your feedback!"));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ServiceRating>>> getPublic() {
        return ResponseEntity.ok(ApiResponse.success(ratingService.getPublicRatings()));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ServiceRating>>> getMyRatings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(ratingService.getMyRatings(userDetails.getUsername())));
    }

    @GetMapping("/averages")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getAverages() {
        return ResponseEntity.ok(ApiResponse.success(ratingService.getAverageRatings()));
    }
}