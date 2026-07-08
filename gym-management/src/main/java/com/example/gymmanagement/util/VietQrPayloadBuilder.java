package com.example.gymmanagement.util;

import java.nio.charset.StandardCharsets;

public class VietQrPayloadBuilder {

    private static String tlv(String tag, String value) {
        String len = String.format("%02d", value.length());
        return tag + len + value;
    }

    public static String build(String bankBin, String accountNo, long amount, String content) {
        String merchantAccountInfo =
                tlv("00", "A000000727") +
                        tlv("01", tlv("00", bankBin) + tlv("01", accountNo)) +
                        tlv("02", "QRIBFTTA");

        String additionalData = tlv("08", content);

        StringBuilder sb = new StringBuilder();
        sb.append(tlv("00", "01"));
        sb.append(tlv("01", "12"));
        sb.append(tlv("38", merchantAccountInfo));
        sb.append(tlv("53", "704"));
        sb.append(tlv("54", String.valueOf(amount)));
        sb.append(tlv("58", "VN"));
        sb.append(tlv("62", additionalData));
        sb.append("6304");

        String crc = crc16CCITT(sb.toString());
        sb.append(crc);
        return sb.toString();
    }

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