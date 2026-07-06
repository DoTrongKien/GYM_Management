package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.SupportMessageRequest;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.service.SupportChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminSupportController {

    private final SupportChatService supportChatService;

    // Danh sách yêu cầu chờ + phiên đang hoạt động
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SupportSessionResponse>>> sessions() {
        return ResponseEntity.ok(ApiResponse.success(supportChatService.listOpenSessions()));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<SupportSessionResponse>> accept(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.accept(userDetails.getUsername(), id),
                "Đã chấp nhận yêu cầu"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<String>> reject(@PathVariable Long id) {
        supportChatService.reject(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Đã từ chối yêu cầu"));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<String>> close(@PathVariable Long id) {
        supportChatService.close(id);
        return ResponseEntity.ok(ApiResponse.success("OK", "Đã kết thúc phiên chat"));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<SupportMessageResponse>>> messages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(supportChatService.getSessionMessages(id)));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<SupportMessageResponse>> send(
            @PathVariable Long id,
            @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.sendAdminMessage(id, request.getContent())));
    }

    // Admin gửi file đính kèm
    @PostMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<SupportMessageResponse>> sendAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption) {
        return ResponseEntity.ok(ApiResponse.success(
                supportChatService.sendAdminAttachment(id, file, caption)));
    }
}
