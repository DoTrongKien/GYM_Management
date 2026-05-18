package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.UserProfileRequest;
import com.example.gymmanagement.entity.UserProfile;
import com.example.gymmanagement.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping("/{userId}")
    public UserProfile createOrUpdateProfile(
            @PathVariable Long userId,
            @RequestBody UserProfileRequest request
    ) {

        return userProfileService.createOrUpdateProfile(
                userId,
                request
        );
    }
}