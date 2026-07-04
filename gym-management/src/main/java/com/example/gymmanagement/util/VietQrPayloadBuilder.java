package com.example.gymmanagement.util;

import java.nio.charset.StandardCharsets;

/**
 * Sinh chuỗi payload QR chuẩn VietQR / NAPAS 247 (theo chuẩn EMVCo QR Code).
 * Chuỗi này FE sẽ tự render thành ảnh QR bằng thư viện `qrcode` ngay trên trình duyệt
 * (không cần gọi ra server ngoài để lấy ảnh QR -> không bao giờ bị vỡ ảnh do mạng chặn).
 *
 * Tham khảo chuẩn: EMVCo QR Code Specification for Payment Systems + VietQR/NAPAS 247.
 */
public class VietQrPayloadBuilder {

    private static String tlv(String tag, String value) {
        String len = String.format("%02d", value.length());
        return tag + len + value;
    }

    /**
     * @param bankBin   Mã BIN ngân hàng theo chuẩn VietQR (vd Vietcombank=970436, MB Bank=970422...)
     * @param accountNo Số tài khoản nhận tiền
     * @param amount    Số tiền (VNĐ, số nguyên, không có phần thập phân)
     * @param content   Nội dung chuyển khoản (nên chứa mã hóa đơn duy nhất, vd "GYMPRO6")
     */
    public static String build(String bankBin, String accountNo, long amount, String content) {
        String merchantAccountInfo =
                tlv("00", "A000000727") +                       // GUID của NAPAS
                        tlv("01", tlv("00", bankBin) + tlv("01", accountNo)) +
                        tlv("02", "QRIBFTTA");                            // Dịch vụ: chuyển khoản nhanh tới TK

        String additionalData = tlv("08", content);               // Purpose / nội dung chuyển khoản

        StringBuilder sb = new StringBuilder();
        sb.append(tlv("00", "01"));                                // Payload Format Indicator
        sb.append(tlv("01", "12"));                                // Point of Initiation - dynamic (có sẵn số tiền)
        sb.append(tlv("38", merchantAccountInfo));                 // Merchant Account Info (NAPAS)
        sb.append(tlv("53", "704"));                               // Currency = VND
        sb.append(tlv("54", String.valueOf(amount)));              // Transaction Amount
        sb.append(tlv("58", "VN"));                                // Country Code
        sb.append(tlv("62", additionalData));                      // Additional Data (nội dung CK)
        sb.append("6304");                                         // CRC tag + length (giá trị tính sau)

        String crc = crc16CCITT(sb.toString());
        sb.append(crc);
        return sb.toString();
    }

    /** CRC16-CCITT (False) - polynomial 0x1021, init 0xFFFF - đúng chuẩn EMVCo yêu cầu. */
    private static String crc16CCITT(String data) {
        int crc = 0xFFFF;
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}