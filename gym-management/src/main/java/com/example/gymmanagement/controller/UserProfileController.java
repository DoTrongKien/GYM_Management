package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.UserProfileRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.dto.response.UserProfileResponse;
import com.example.gymmanagement.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService profileService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> saveProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.saveOrUpdate(userDetails.getUsername(), request),
                "Profile updated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getMyProfile(userDetails.getUsername())));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileById(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfileById(userId)));
    }
}