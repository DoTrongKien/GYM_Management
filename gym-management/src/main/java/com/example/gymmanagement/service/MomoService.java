package com.example.gymmanagement.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tích hợp MoMo AIO (captureWallet) - tạo mã QR thanh toán + xác thực IPN callback.
 * Tài liệu: https://developers.momo.vn
 *
 * Mặc định dùng bộ test credentials công khai của MoMo sandbox (chỉ dùng để dev/test).
 * Khi lên production, đổi toàn bộ giá trị momo.* trong application.properties sang
 * thông tin merchant thật do MoMo cấp.
 */
@Service
@Slf4j
public class MomoService {

    @Value("${momo.partner-code}")
    private String partnerCode;

    @Value("${momo.access-key}")
    private String accessKey;

    @Value("${momo.secret-key}")
    private String secretKey;

    @Value("${momo.endpoint}")
    private String endpoint;

    @Value("${momo.redirect-url}")
    private String redirectUrl;

    @Value("${momo.ipn-url}")
    private String ipnUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Data
    public static class MomoCreateResult {
        private boolean success;
        private String payUrl;
        private String qrCodeUrl;
        private String deeplink;
        private String requestId;
        private Integer resultCode;
        private String message;
    }

    /**
     * Gọi MoMo tạo giao dịch thanh toán (QR code), trả về payUrl/qrCodeUrl/deeplink.
     */
    public MomoCreateResult createPayment(String momoOrderId, long amount, String orderInfo) {
        String requestId = momoOrderId + "-" + System.currentTimeMillis();
        String requestType = "captureWallet";
        String extraData = "";

        // Chuỗi ký theo đúng thứ tự MoMo yêu cầu
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + momoOrderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = hmacSHA256(rawSignature, secretKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("accessKey", accessKey);
        body.put("requestId", requestId);
        body.put("amount", String.valueOf(amount));
        body.put("orderId", momoOrderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", ipnUrl);
        body.put("extraData", extraData);
        body.put("requestType", requestType);
        body.put("signature", signature);
        body.put("lang", "vi");
// Đồng bộ hạn thanh toán trên trang MoMo với đúng 5 phút của hệ thống (đơn vị: phút).
// Field này KHÔNG nằm trong công thức ký signature nên thêm vào body không ảnh hưởng chữ ký.
        body.put("orderExpireTime", 5);

        MomoCreateResult result = new MomoCreateResult();
        result.setRequestId(requestId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(endpoint, entity, Map.class);

            if (response == null) {
                result.setSuccess(false);
                result.setMessage("Không nhận được phản hồi từ MoMo");
                return result;
            }

            Object resultCodeObj = response.get("resultCode");
            int resultCode = resultCodeObj != null ? Integer.parseInt(resultCodeObj.toString()) : -1;
            result.setResultCode(resultCode);
            result.setMessage((String) response.get("message"));

            if (resultCode == 0) {
                result.setSuccess(true);
                result.setPayUrl((String) response.get("payUrl"));
                result.setQrCodeUrl((String) response.get("qrCodeUrl"));
                result.setDeeplink((String) response.get("deeplink"));
            } else {
                result.setSuccess(false);
                log.warn("[MoMo] createPayment failed resultCode={} message={}", resultCode, response.get("message"));
            }
        } catch (Exception e) {
            log.error("[MoMo] createPayment error for orderId={}", momoOrderId, e);
            result.setSuccess(false);
            result.setMessage("Lỗi kết nối MoMo: " + e.getMessage());
        }

        return result;
    }

    /**
     * Xác thực chữ ký của IPN callback MoMo gửi về, chống giả mạo request.
     * Không tin bất kỳ trường nào trong payload nếu chữ ký không khớp.
     */
    public boolean verifyIpnSignature(Map<String, Object> payload) {
        try {
            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + payload.get("amount") +
                    "&extraData=" + payload.getOrDefault("extraData", "") +
                    "&message=" + payload.get("message") +
                    "&orderId=" + payload.get("orderId") +
                    "&orderInfo=" + payload.get("orderInfo") +
                    "&orderType=" + payload.get("orderType") +
                    "&partnerCode=" + payload.get("partnerCode") +
                    "&payType=" + payload.get("payType") +
                    "&requestId=" + payload.get("requestId") +
                    "&responseTime=" + payload.get("responseTime") +
                    "&resultCode=" + payload.get("resultCode") +
                    "&transId=" + payload.get("transId");

            String expectedSignature = hmacSHA256(rawSignature, secretKey);
            String receivedSignature = String.valueOf(payload.get("signature"));
            return expectedSignature.equalsIgnoreCase(receivedSignature);
        } catch (Exception e) {
            log.error("[MoMo] verifyIpnSignature error", e);
            return false;
        }
    }

    /**
     * Gọi MoMo refund API (hoàn tiền) cho 1 giao dịch đã PAID.
     * Yêu cầu transId gốc trả về từ MoMo lúc thanh toán thành công.
     * NOTE: cần merchant thật + quyền refund được MoMo cấp mới hoạt động ở production.
     */
    public boolean refund(String momoOrderId, String transId, long amount, String description) {
        String requestId = "REFUND-" + momoOrderId + "-" + System.currentTimeMillis();
        String refundEndpoint = endpoint.replace("/create", "/refund");

        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&description=" + description +
                "&orderId=" + requestId +
                "&partnerCode=" + partnerCode +
                "&requestId=" + requestId +
                "&transId=" + transId;
        String signature = hmacSHA256(rawSignature, secretKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("orderId", requestId);
        body.put("requestId", requestId);
        body.put("amount", amount);
        body.put("transId", transId);
        body.put("lang", "vi");
        body.put("description", description);
        body.put("signature", signature);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(refundEndpoint, entity, Map.class);
            Object resultCodeObj = response != null ? response.get("resultCode") : null;
            int resultCode = resultCodeObj != null ? Integer.parseInt(resultCodeObj.toString()) : -1;
            if (resultCode != 0) {
                log.warn("[MoMo] refund failed orderId={} response={}", momoOrderId, response);
            }
            return resultCode == 0;
        } catch (Exception e) {
            log.error("[MoMo] refund error orderId={}", momoOrderId, e);
            return false;
        }
    }

    private String hmacSHA256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Không thể tạo chữ ký MoMo", e);
        }
    }
}