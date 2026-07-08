package com.example.gymmanagement.controller;

import com.example.gymmanagement.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bank/webhook")
@RequiredArgsConstructor
@Slf4j
public class BankWebhookController {

    private final InvoiceService invoiceService;

    @Value("${sepay.webhook-api-key}")
    private String expectedApiKey;

    @PostMapping("/sepay")
    public ResponseEntity<Map<String, Object>> sepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> payload) {

        log.info("[SePay Webhook] Received: {}", payload);

        String expected = "Apikey " + expectedApiKey;
        if (authorization == null || !authorization.trim().equals(expected)) {
            log.warn("[SePay Webhook] Sai API key, từ chối request. Header nhận được: {}", authorization);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        try {
            invoiceService.handleBankWebhook(payload);
        } catch (Exception e) {
            log.error("[SePay Webhook] Lỗi xử lý payload", e);
        }

        return ResponseEntity.ok(Map.of("success", true));
    }
}