package com.example.gymmanagement.service;

import com.example.gymmanagement.entity.WorkoutPlan;
import com.example.gymmanagement.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Quy tắc hồi phục mana:
 *  - Chưa từng tập (lastTrainingDate null)         -> full mana.
 *  - Khoảng cách >= 2 ngày kể từ lần tập trước       -> full mana (coi như đã nghỉ trọn 1 ngày).
 *  - Khoảng cách == 1 ngày (tập liên tiếp hôm sau)   -> hồi 75% maxMana (cộng dồn, cap ở maxMana).
 *  - Khoảng cách == 0 (tập nhiều lần trong cùng 1 ngày) -> không hồi thêm, chỉ trừ tiếp.
 *
 * FIX: applyRegen() giờ IDEMPOTENT theo ngày — dựa vào lastManaRegenDate.
 * checkIn() (qua getCurrentManaAfterRegen) và checkOut() (qua consumeMana) của
 * CÙNG 1 buổi đều gọi applyRegen(); trước đây gọi 2 lần trong cùng ngày sẽ cộng
 * regen 2 lần (VD case gap==1 cộng 75% x2). Giờ nếu lastManaRegenDate == hôm nay,
 * applyRegen() thoát sớm, không cộng thêm lần nữa.
 */
@Service
@RequiredArgsConstructor
public class ManaService {

    private static final double NEXT_DAY_REGEN_RATE = 0.75;

    private final WorkoutPlanRepository planRepo;

    @Transactional
    public void applyRegen(WorkoutPlan plan) {
        if (plan.getMaxMana() == null) return; // plan không có hệ thống mana (VD template cũ)

        LocalDate today = LocalDate.now();
        if (today.equals(plan.getLastManaRegenDate())) {
            return; // đã regen hôm nay rồi -> không cộng trùng
        }

        if (plan.getLastTrainingDate() == null) {
            plan.setCurrentMana(plan.getMaxMana());
        } else {
            long gap = ChronoUnit.DAYS.between(plan.getLastTrainingDate(), today);

            if (gap >= 2) {
                plan.setCurrentMana(plan.getMaxMana());
            } else if (gap == 1) {
                int regen = (int) Math.round(plan.getMaxMana() * NEXT_DAY_REGEN_RATE);
                int cur = plan.getCurrentMana() != null ? plan.getCurrentMana() : 0;
                plan.setCurrentMana(Math.min(plan.getMaxMana(), cur + regen));
            }
            // gap == 0: không regen, giữ nguyên currentMana
        }

        plan.setLastManaRegenDate(today);
    }

    /**
     * Trừ mana theo tổng stamina đã tiêu thụ thực tế của buổi tập.
     * @return true nếu vượt quá mana hiện có -> FE cần popup cảnh báo chấn thương.
     */
    @Transactional
    public boolean consumeMana(WorkoutPlan plan, int totalConsumed) {
        if (plan.getMaxMana() == null) return false;

        applyRegen(plan); // an toàn gọi lại — idempotent nếu checkIn() đã regen hôm nay

        int cur = plan.getCurrentMana() != null ? plan.getCurrentMana() : plan.getMaxMana();
        boolean overLimit = totalConsumed > cur;

        plan.setCurrentMana(Math.max(0, cur - totalConsumed));
        plan.setLastTrainingDate(LocalDate.now());
        planRepo.save(plan);

        return overLimit;
    }

    /**
     * Ước tính chi phí mana của 1 buổi tập TRƯỚC khi checkout (dùng ở checkIn
     * để quyết định có cần hiện popup cảnh báo hay không).
     * staminaCost mặc định 10 nếu null (giữ đúng convention cũ), dù dữ liệu thực tế
     * trung bình 15-25/bài.
     */
    public int estimateSessionCost(java.util.List<Integer> staminaCosts) {
        return staminaCosts.stream().mapToInt(c -> c != null ? c : 10).sum();
    }

    /** Mana hiện có SAU KHI đã cộng hồi phục theo ngày nghỉ, dùng để so sánh ở checkIn. */
    @Transactional
    public int getCurrentManaAfterRegen(WorkoutPlan plan) {
        if (plan.getMaxMana() == null) return Integer.MAX_VALUE; // plan không dùng mana -> không giới hạn
        applyRegen(plan);
        planRepo.save(plan);
        return plan.getCurrentMana() != null ? plan.getCurrentMana() : plan.getMaxMana();
    }
}