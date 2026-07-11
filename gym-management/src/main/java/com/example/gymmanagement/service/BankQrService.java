package com.example.gymmanagement.service;

import com.example.gymmanagement.util.VietQrPayloadBuilder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class BankQrService {

    @Value("${bank.id}")
    private String bankBin;

    @Value("${bank.account-no}")
    private String accountNo;

    @Value("${bank.account-name}")
    private String accountName;

    @Data
    public static class BankQrResult {
        private String transferCode;
        private String content;
        private String qrRawPayload;
        private String qrImageUrl;
    }

    public BankQrResult generate(Long invoiceId, long amount) {
        String transferCode = "GYMPRO" + invoiceId;

        BankQrResult result = new BankQrResult();
        result.setTransferCode(transferCode);
        result.setContent(transferCode);
        result.setQrRawPayload(VietQrPayloadBuilder.build(bankBin, accountNo, amount, transferCode));
        result.setQrImageUrl(buildImageUrl(amount, transferCode));
        return result;
    }

    private String buildImageUrl(long amount, String content) {
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        return String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankBin, accountNo, amount, encodedContent, encodedName);
    }
}