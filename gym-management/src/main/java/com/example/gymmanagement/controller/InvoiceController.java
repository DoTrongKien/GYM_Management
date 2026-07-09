package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.CreateInvoiceRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.dto.response.InvoiceResponse;
import com.example.gymmanagement.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.createInvoice(userDetails.getUsername(), request),
                "Đã tạo hóa đơn. Vui lòng quét mã QR để thanh toán trong 5 phút."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getMyInvoices(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getMyInvoices(userDetails.getUsername())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getOne(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoice(id, userDetails.getUsername())));
    }

    @PostMapping("/{id}/regenerate-qr")
    public ResponseEntity<ApiResponse<InvoiceResponse>> regenerateQr(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.regenerateQr(id, userDetails.getUsername()), "Đã tạo lại mã QR"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancel(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                invoiceService.cancelInvoice(id, userDetails.getUsername()), "Đã hủy hóa đơn"));
    }
}