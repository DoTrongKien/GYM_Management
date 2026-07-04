//package com.example.gymmanagement.controller;
//
//import com.example.gymmanagement.service.InvoiceService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
///**
// * Endpoint public để MoMo gọi server-to-server báo kết quả thanh toán (IPN).
// * KHÔNG được yêu cầu JWT - phải mở public trong SecurityConfig.
// * Chữ ký của payload được xác thực bên trong InvoiceService.handleIpn().
// */
//@RestController
//@RequestMapping("/api/momo")
//@RequiredArgsConstructor
//@Slf4j
//public class MomoWebhookController {
//
//    private final InvoiceService invoiceService;
//
//    @PostMapping("/ipn")
//    public ResponseEntity<Map<String, Object>> ipn(@RequestBody Map<String, Object> payload) {
//        log.info("[MoMo IPN] Received: {}", payload);
//        try {
//            invoiceService.handleIpn(payload);
//        } catch (Exception e) {
//            log.error("[MoMo IPN] Error handling payload", e);
//        }
//        // MoMo yêu cầu luôn trả về resultCode=0 để xác nhận đã nhận IPN,
//        // tránh MoMo gửi lại nhiều lần.
//        return ResponseEntity.ok(Map.of("resultCode", 0, "message", "Confirm Success"));
//    }
//}