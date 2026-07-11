package com.example.gymmanagement.service.plan;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import com.example.gymmanagement.enums.MuscleGroup;

import java.util.*;

/**
 * Sinh "nhóm cơ theo từng buổi trong tuần" (dayIndex, KHÔNG phải dayOfWeek) và "số bài
 * tập/nhóm cơ/buổi" theo đúng I.docx mục 6 — bảng tra cứu tường minh (6.1.1 → 6.1.5),
 * KHÔNG dùng công thức xoay vòng như bản cũ.
 *
 * dayIndex (0,1,2,...) là thứ tự buổi tập trong tuần theo bảng 6.1.x, độc lập với
 * dayOfWeek thực tế (dayOfWeek đến từ ScheduleCatalog và được WorkoutPlanService ánh xạ
 * theo đúng thứ tự dayIndex -> vị trí trong candidate lịch).
 *
 * ────────────────────────────────────────────────────────────────
 * THUẬT TOÁN (mục 6.2 I.docx)
 * ────────────────────────────────────────────────────────────────
 * 1) Với mỗi nhóm cơ xuất hiện trong tuần, xác định f = số buổi (ngày) nhóm cơ đó
 *    THỰC SỰ xuất hiện (đếm theo bảng DAY_MUSCLE_GROUPS).
 * 2) BaseQuota = số bài/nhóm cơ/TUẦN theo (Goal, FitnessLevel) — bảng BASE_QUOTA.
 * 3) AdjustedQuota:
 *      f >= 2  -> AdjustedQuota = BaseQuota
 *      f == 1  -> AdjustedQuota = min(BaseQuota, T_max=4)
 * 4) Chia AdjustedQuota cho f buổi bằng Largest Remainder Method (mọi buổi trọng số
 *    bằng nhau): base = AdjustedQuota / f, remainder = AdjustedQuota % f; các buổi đầu
 *    tiên (theo thứ tự dayIndex xuất hiện) nhận thêm 1 cho tới khi hết remainder.
 */
public final class MuscleGroupSplitPlanner {

    private MuscleGroupSplitPlanner() {}

    private static final int T_MAX = 4;

    // ── Nhóm cơ theo từng buổi trong tuần, tra theo (Goal, sessionsPerWeek) ──
    // Mỗi phần tử ngoài cùng = 1 buổi (dayIndex theo thứ tự), giá trị = các nhóm cơ của buổi đó.
    // Nguồn: I.docx mục 6.1.1 -> 6.1.5.
    private static final Map<Goal, Map<Integer, List<List<MuscleGroup>>>> DAY_MUSCLE_GROUPS = new EnumMap<>(Goal.class);
    static {
        // ── 6.1.1 MUSCLE_GAIN ──
        Map<Integer, List<List<MuscleGroup>>> muscleGain = new HashMap<>();
        muscleGain.put(4, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS)
        ));
        muscleGain.put(5, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS)
        ));
        muscleGain.put(6, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        DAY_MUSCLE_GROUPS.put(Goal.MUSCLE_GAIN, muscleGain);

        // ── 6.1.2 WEIGHT_LOSS ──
        Map<Integer, List<List<MuscleGroup>>> weightLoss = new HashMap<>();
        weightLoss.put(4, List.of(
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS)
        ));
        weightLoss.put(5, List.of(
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        weightLoss.put(6, List.of(
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        DAY_MUSCLE_GROUPS.put(Goal.WEIGHT_LOSS, weightLoss);

        // ── 6.1.3 ENDURANCE ──
        Map<Integer, List<List<MuscleGroup>>> endurance = new HashMap<>();
        endurance.put(3, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CORE, MuscleGroup.LEGS, MuscleGroup.CARDIO),
                List.of(MuscleGroup.ARMS, MuscleGroup.BACK, MuscleGroup.CARDIO)
        ));
        endurance.put(4, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CORE, MuscleGroup.LEGS, MuscleGroup.CARDIO),
                List.of(MuscleGroup.ARMS, MuscleGroup.BACK, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        endurance.put(5, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CORE, MuscleGroup.LEGS, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.ARMS, MuscleGroup.BACK, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        DAY_MUSCLE_GROUPS.put(Goal.ENDURANCE, endurance);

        // ── 6.1.4 FLEXIBILITY ──
        Map<Integer, List<List<MuscleGroup>>> flexibility = new HashMap<>();
        flexibility.put(2, List.of(
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        flexibility.put(3, List.of(
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        flexibility.put(4, List.of(
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        DAY_MUSCLE_GROUPS.put(Goal.FLEXIBILITY, flexibility);

        // ── 6.1.5 MAINTENANCE ──
        Map<Integer, List<List<MuscleGroup>>> maintenance = new HashMap<>();
        maintenance.put(3, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO)
        ));
        maintenance.put(4, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS)
        ));
        maintenance.put(5, List.of(
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS),
                List.of(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO),
                List.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
                List.of(MuscleGroup.BACK, MuscleGroup.CORE, MuscleGroup.LEGS)
        ));
        DAY_MUSCLE_GROUPS.put(Goal.MAINTENANCE, maintenance);
    }

    // ── BaseQuota: số bài/nhóm cơ/TUẦN theo (Goal, FitnessLevel) ──
    // Nguồn: bảng "Số bài tập cho mỗi nhóm cơ / TUẦN" ngay dưới mỗi bảng 6.1.x.
    private static final Map<Goal, Map<FitnessLevel, Integer>> BASE_QUOTA = new EnumMap<>(Goal.class);
    static {
        Map<FitnessLevel, Integer> strengthLike = new EnumMap<>(FitnessLevel.class);
        strengthLike.put(FitnessLevel.BEGINNER, 4);
        strengthLike.put(FitnessLevel.INTERMEDIATE, 4);
        strengthLike.put(FitnessLevel.ADVANCED, 6);
        BASE_QUOTA.put(Goal.MUSCLE_GAIN, strengthLike);
        BASE_QUOTA.put(Goal.WEIGHT_LOSS, new EnumMap<>(strengthLike));
        BASE_QUOTA.put(Goal.FLEXIBILITY, new EnumMap<>(strengthLike));

        Map<FitnessLevel, Integer> enduranceLike = new EnumMap<>(FitnessLevel.class);
        enduranceLike.put(FitnessLevel.BEGINNER, 3);
        enduranceLike.put(FitnessLevel.INTERMEDIATE, 4);
        enduranceLike.put(FitnessLevel.ADVANCED, 5);
        BASE_QUOTA.put(Goal.ENDURANCE, enduranceLike);
        BASE_QUOTA.put(Goal.MAINTENANCE, new EnumMap<>(enduranceLike));
    }

    /**
     * Trả về, cho từng buổi trong tuần (index = dayIndex, 0..sessions-1), map
     * "nhóm cơ -> số bài tập cần chọn cho nhóm cơ đó trong buổi này".
     * Thứ tự các entry trong mỗi Map giữ đúng thứ tự xuất hiện của nhóm cơ trong bảng 6.1.x.
     */
    public static List<Map<MuscleGroup, Integer>> buildWeekPlan(Goal goal, FitnessLevel level, int sessions) {
        List<List<MuscleGroup>> dayGroups = dayGroupsFor(goal, sessions);
        int baseQuota = baseQuotaFor(goal, level);

        // group -> danh sách dayIndex mà nhóm cơ đó xuất hiện (theo thứ tự tăng dần)
        Map<MuscleGroup, List<Integer>> occurrenceDays = new EnumMap<>(MuscleGroup.class);
        for (int d = 0; d < dayGroups.size(); d++) {
            for (MuscleGroup mg : dayGroups.get(d)) {
                occurrenceDays.computeIfAbsent(mg, k -> new ArrayList<>()).add(d);
            }
        }

        // group -> (dayIndex -> số bài của nhóm cơ đó trong buổi dayIndex)
        Map<MuscleGroup, Map<Integer, Integer>> perGroupPerDay = new EnumMap<>(MuscleGroup.class);
        for (Map.Entry<MuscleGroup, List<Integer>> e : occurrenceDays.entrySet()) {
            List<Integer> days = e.getValue();
            int f = days.size();
            int adjustedQuota = (f == 1) ? Math.min(baseQuota, T_MAX) : baseQuota;
            perGroupPerDay.put(e.getKey(), largestRemainderDistribute(adjustedQuota, days));
        }

        List<Map<MuscleGroup, Integer>> result = new ArrayList<>();
        for (int d = 0; d < dayGroups.size(); d++) {
            Map<MuscleGroup, Integer> dayMap = new LinkedHashMap<>();
            for (MuscleGroup mg : dayGroups.get(d)) {
                dayMap.put(mg, perGroupPerDay.get(mg).get(d));
            }
            result.add(dayMap);
        }
        return result;
    }

    /** Largest Remainder Method: chia quota cho các dayIndex trong "days", trọng số bằng nhau. */
    private static Map<Integer, Integer> largestRemainderDistribute(int quota, List<Integer> days) {
        int f = days.size();
        int base = quota / f;
        int remainder = quota % f;
        Map<Integer, Integer> dist = new LinkedHashMap<>();
        for (int i = 0; i < f; i++) {
            int count = base + (i < remainder ? 1 : 0);
            dist.put(days.get(i), count);
        }
        return dist;
    }

    private static List<List<MuscleGroup>> dayGroupsFor(Goal goal, int sessions) {
        Map<Integer, List<List<MuscleGroup>>> byGoal = DAY_MUSCLE_GROUPS.get(goal);
        if (byGoal == null || !byGoal.containsKey(sessions)) {
            throw new IllegalStateException("Chưa khai báo nhóm cơ theo ngày cho Goal=" + goal
                    + ", sessions=" + sessions + " — thêm vào MuscleGroupSplitPlanner.DAY_MUSCLE_GROUPS.");
        }
        return byGoal.get(sessions);
    }

    private static int baseQuotaFor(Goal goal, FitnessLevel level) {
        Map<FitnessLevel, Integer> byLevel = BASE_QUOTA.get(goal);
        if (byLevel == null || !byLevel.containsKey(level)) {
            throw new IllegalStateException("Chưa khai báo BaseQuota cho Goal=" + goal
                    + ", Level=" + level + " — thêm vào MuscleGroupSplitPlanner.BASE_QUOTA.");
        }
        return byLevel.get(level);
    }
}