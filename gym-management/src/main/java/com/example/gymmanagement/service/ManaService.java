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
 *  - Khoảng cách == 1 ngày (tập liên tiếp hôm sau)   -> hồi 50% maxMana (cộng dồn, cap ở maxMana).
 *  - Khoảng cách == 0 (tập nhiều lần trong cùng 1 ngày) -> không hồi thêm, chỉ trừ tiếp.
 */
@Service
@RequiredArgsConstructor
public class ManaService {

    private final WorkoutPlanRepository planRepo;

    @Transactional
    public void applyRegen(WorkoutPlan plan) {
        if (plan.getMaxMana() == null) return; // plan không có hệ thống mana (VD template cũ)

        if (plan.getLastTrainingDate() == null) {
            plan.setCurrentMana(plan.getMaxMana());
            return;
        }

        long gap = ChronoUnit.DAYS.between(plan.getLastTrainingDate(), LocalDate.now());

        if (gap >= 2) {
            plan.setCurrentMana(plan.getMaxMana());
        } else if (gap == 1) {
            int halfRegen = plan.getMaxMana() / 2;
            int cur = plan.getCurrentMana() != null ? plan.getCurrentMana() : 0;
            plan.setCurrentMana(Math.min(plan.getMaxMana(), cur + halfRegen));
        }
        // gap == 0: không regen, giữ nguyên currentMana
    }

    /**
     * Trừ mana theo tổng stamina đã tiêu thụ thực tế của buổi tập.
     * @return true nếu vượt quá mana hiện có -> FE cần popup cảnh báo chấn thương.
     */
    @Transactional
    public boolean consumeMana(WorkoutPlan plan, int totalConsumed) {
        if (plan.getMaxMana() == null) return false;

        applyRegen(plan);

        int cur = plan.getCurrentMana() != null ? plan.getCurrentMana() : plan.getMaxMana();
        boolean overLimit = totalConsumed > cur;

        plan.setCurrentMana(Math.max(0, cur - totalConsumed));
        plan.setLastTrainingDate(LocalDate.now());
        planRepo.save(plan);

        return overLimit;
    }
}