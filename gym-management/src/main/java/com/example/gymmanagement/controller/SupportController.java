package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.SupportMessageRequest;
import com.example.gymmanagement.dto.request.SupportRequestRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.service.SupportChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportChatService supportChatService;

    // User tạo một cuộc hội thoại mới với admin (kèm tiêu đề vấn đề)
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<SupportSessionResponse>> request(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) SupportRequestRequest request) {
        String subject = request != null ? request.getSubject() : null;
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.requestChat(userDetails.getUsername(), subject),
                "Đã gửi yêu cầu, vui lòng chờ admin xác nhận"));
    }

    // Danh sách các cuộc hội thoại đang mở của user (để poll)
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SupportSessionResponse>>> mySessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.getMySessions(userDetails.getUsername())));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<List<SupportMessageResponse>>> myMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.getMyMessages(userDetails.getUsername(), id)));
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<SupportMessageResponse>> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.sendUserMessage(userDetails.getUsername(), id, request.getContent())));
    }

    // User gửi file đính kèm (ảnh / video / file bất kỳ)
    @PostMapping("/sessions/{id}/attachments")
    public ResponseEntity<ApiResponse<SupportMessageResponse>> sendAttachment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.sendUserAttachment(userDetails.getUsername(), id, file, caption)));
    }

    @PostMapping("/sessions/{id}/close")
    public ResponseEntity<ApiResponse<String>> close(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        supportChatService.closeByUser(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Đã kết thúc cuộc hội thoại"));
    }
}
