package com.example.gymmanagement.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Kết quả check-in — 2 trường hợp:
 *  - requiresConfirmation=true  : chưa check-in thật, FE hiện popup cảnh báo chấn thương,
 *                                   session=null. Gọi lại checkIn() với confirmReducedIntensity=true
 *                                   nếu người dùng chọn "vẫn muốn tập".
 *  - requiresConfirmation=false : đã check-in thật, session chứa dữ liệu buổi tập
 *                                   (set/rep có thể đã bị giảm nếu mana không đủ và người
 *                                   dùng xác nhận tập tiếp).
 */
@Getter
@Builder
public class CheckInResult {
    private boolean requiresConfirmation;
    private String warningMessage;
    private Integer estimatedManaCost;
    private Integer currentMana;
    private WorkoutSessionResponse session;
}