package com.example.gymmanagement.scheduler;

import com.example.gymmanagement.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceExpiryScheduler {

    private final InvoiceService invoiceService;

    @Scheduled(fixedRate = 30000)
    public void expireOverdueInvoices() {
        int count = invoiceService.expireOverdueInvoices();
        if (count > 0) {
            log.info("[InvoiceExpiryScheduler] Đã chuyển {} hóa đơn sang EXPIRED", count);
        }
    }
}