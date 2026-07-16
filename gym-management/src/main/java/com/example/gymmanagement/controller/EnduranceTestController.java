package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.SubmitEnduranceTestRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.dto.response.EnduranceTestResponse;
import com.example.gymmanagement.service.EnduranceTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/endurance-test")
@RequiredArgsConstructor
public class EnduranceTestController {

    private final EnduranceTestService enduranceTestService;

    @PostMapping
    public ResponseEntity<ApiResponse<EnduranceTestResponse>> submit(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody SubmitEnduranceTestRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                enduranceTestService.submitTest(ud.getUsername(), req), "Đã lưu kết quả bài test"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<EnduranceTestResponse>> getMine(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(
                enduranceTestService.getMyTest(ud.getUsername()).orElse(null)));
    }
}