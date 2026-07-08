//package com.example.gymmanagement.controller;
//
//import com.example.gymmanagement.dto.request.ChatRequest;
//import com.example.gymmanagement.dto.response.*;
//import com.example.gymmanagement.service.ChatBotService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/chat")
//@RequiredArgsConstructor
//public class ChatController {
//
//    private final ChatBotService chatBotService;
//
//    @PostMapping
//    public ResponseEntity<ApiResponse<ChatResponse>> chat(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestBody ChatRequest request) {
//        return ResponseEntity.ok(ApiResponse.success(
//                chatBotService.chat(userDetails.getUsername(), request)));
//    }
//
//    // Gửi file đính kèm cho bot (ảnh / video / file bất kỳ)
//    @PostMapping("/attachments")
//    public ResponseEntity<ApiResponse<ChatResponse>> attachment(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam("file") MultipartFile file,
//            @RequestParam(value = "caption", required = false) String caption) {
//        return ResponseEntity.ok(ApiResponse.success(
//                chatBotService.chatAttachment(userDetails.getUsername(), file, caption)));
//    }
//
//    @GetMapping("/history")
//    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> history(
//            @AuthenticationPrincipal UserDetails userDetails) {
//        return ResponseEntity.ok(ApiResponse.success(
//                chatBotService.getHistory(userDetails.getUsername())));
//    }
//
//    @GetMapping("/suggestions")
//    public ResponseEntity<ApiResponse<List<String>>> suggestions() {
//        return ResponseEntity.ok(ApiResponse.success(chatBotService.getSuggestions()));
//    }
//
//    @DeleteMapping("/history")
//    public ResponseEntity<ApiResponse<String>> clear(
//            @AuthenticationPrincipal UserDetails userDetails) {
//        chatBotService.clearHistory(userDetails.getUsername());
//        return ResponseEntity.ok(ApiResponse.success("Đã xóa lịch sử chat", "OK"));
//    }
//}
