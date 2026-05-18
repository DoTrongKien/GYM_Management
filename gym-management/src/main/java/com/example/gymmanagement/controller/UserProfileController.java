package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.UserProfileRequest;
import com.example.gymmanagement.entity.UserProfile;
import com.example.gymmanagement.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * ROLE_USER: tự cập nhật profile của mình
     * userId lấy từ token — không cần truyền trên URL
     * POST /api/profile
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public UserProfile createOrUpdateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserProfileRequest request
    ) {
        String email = userDetails.getUsername(); // email từ token
        return userProfileService.createOrUpdateProfile(email, request);
    }

    /**
     * ROLE_ADMIN: cập nhật profile của bất kỳ user nào
     * POST /api/profile/{userId}
     */
    @PostMapping("/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserProfile createOrUpdateProfileByAdmin(
            @PathVariable Long userId,
            @RequestBody UserProfileRequest request
    ) {
        return userProfileService.createOrUpdateProfile(userId, request);
    }
}