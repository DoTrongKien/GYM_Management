package com.example.gymmanagement.service.plan;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import com.example.gymmanagement.enums.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MuscleGroupSplitPlanner đã được refactor hoàn toàn theo I.docx mục 6.1.1 -> 6.1.5 +
 * thuật toán BaseQuota -> AdjustedQuota -> Largest Remainder Method (buildWeekPlan).
 * API cũ (buildDayConfigs / exercisesPerGroupPerDay / coverageCycleDays) không còn tồn tại.
 *
 * Mỗi khi phát hiện case chia lịch/nhóm cơ chưa hợp lý trong thực tế,
 * thêm @Test mới vào đây.
 */
class MuscleGroupSplitPlannerTest {

    // Các tổ hợp (Goal, sessionsPerWeek) hợp lệ sau khi calcSessionsPerWeek đã clamp
    // theo mục 4 I.docx — đây cũng chính xác là các tổ hợp có mặt trong bảng 6.1.x.
    private static final Map<Goal, List<Integer>> VALID_SESSIONS = Map.of(
            Goal.MUSCLE_GAIN, List.of(4, 5, 6),
            Goal.WEIGHT_LOSS, List.of(4, 5, 6),
            Goal.ENDURANCE, List.of(3, 4, 5),
            Goal.FLEXIBILITY, List.of(2, 3, 4),
            Goal.MAINTENANCE, List.of(3, 4, 5)
    );

    // ============================================================
    // 1. Số ngày trả về phải luôn đúng bằng sessionsPerWeek, cho MỌI tổ hợp hợp lệ
    // và MỌI FitnessLevel.
    // ============================================================
    @Test
    void soNgayTraVe_dungBangSessions_choTatCaComboHopLe() {
        for (Map.Entry<Goal, List<Integer>> e : VALID_SESSIONS.entrySet()) {
            Goal goal = e.getKey();
            for (int sessions : e.getValue()) {
                for (FitnessLevel level : FitnessLevel.values()) {
                    List<Map<MuscleGroup, Integer>> weekPlan =
                            MuscleGroupSplitPlanner.buildWeekPlan(goal, level, sessions);
                    assertEquals(sessions, weekPlan.size(),
                            () -> "Goal=" + goal + " level=" + level + " sessions=" + sessions
                                    + " phải trả về đúng " + sessions + " ngày");
                }
            }
        }
    }

    // ============================================================
    // 2. Số bài tập/nhóm cơ/buổi không bao giờ được âm (có thể = 0 khi BaseQuota < f,
    // xem test riêng bên dưới cho case cụ thể, nhưng KHÔNG BAO GIỜ âm).
    // ============================================================
    @Test
    void soBaiMoiNhomCoMoiBuoi_khongDuocAm() {
        for (Map.Entry<Goal, List<Integer>> e : VALID_SESSIONS.entrySet()) {
            Goal goal = e.getKey();
            for (int sessions : e.getValue()) {
                for (FitnessLevel level : FitnessLevel.values()) {
                    List<Map<MuscleGroup, Integer>> weekPlan =
                            MuscleGroupSplitPlanner.buildWeekPlan(goal, level, sessions);
                    for (Map<MuscleGroup, Integer> dayMap : weekPlan) {
                        for (Map.Entry<MuscleGroup, Integer> entry : dayMap.entrySet()) {
                            assertTrue(entry.getValue() >= 0,
                                    "Goal=" + goal + " level=" + level + " sessions=" + sessions
                                            + " nhóm cơ=" + entry.getKey() + " có giá trị âm: " + entry.getValue());
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // 3. MUSCLE_GAIN / INTERMEDIATE / sessions=4: f=2 cho mọi nhóm cơ, BaseQuota=4
    // -> AdjustedQuota=4, LRM chia đều 2 ngày -> mỗi ngày 2 bài/nhóm.
    // Ngày 0 và ngày 2 phải hoàn toàn giống nhau (chu kỳ lặp lại sau 2 ngày).
    // ============================================================
    @Test
    void muscleGain_intermediate_sessions4_choDungSoBaiMoiNgayVaChuKyLapLai() {
        List<Map<MuscleGroup, Integer>> weekPlan =
                MuscleGroupSplitPlanner.buildWeekPlan(Goal.MUSCLE_GAIN, FitnessLevel.INTERMEDIATE, 4);

        Map<MuscleGroup, Integer> expectedPushDay = Map.of(
                MuscleGroup.CHEST, 2, MuscleGroup.SHOULDERS, 2, MuscleGroup.ARMS, 2);
        Map<MuscleGroup, Integer> expectedPullDay = Map.of(
                MuscleGroup.BACK, 2, MuscleGroup.CORE, 2, MuscleGroup.LEGS, 2);

        assertEquals(expectedPushDay, weekPlan.get(0));
        assertEquals(expectedPullDay, weekPlan.get(1));
        assertEquals(expectedPushDay, weekPlan.get(2), "Ngày 2 phải lặp lại đúng ngày 0 (chu kỳ 2 ngày)");
        assertEquals(expectedPullDay, weekPlan.get(3), "Ngày 3 phải lặp lại đúng ngày 1 (chu kỳ 2 ngày)");
    }

    // ============================================================
    // 4. MUSCLE_GAIN / ADVANCED / sessions=6: f=2 cho mọi nhóm cơ (kể cả FULL_BODY/CARDIO),
    // BaseQuota=6 -> AdjustedQuota=6, LRM chia đều 2 ngày -> 3 bài/nhóm/ngày.
    // Chu kỳ lặp lại sau 3 ngày: ngày0==ngày3, ngày1==ngày4, ngày2==ngày5.
    // Số nhóm cơ/ngày không cố định 2 như bảng cứng cũ mà là [3,3,2,3,3,2].
    // ============================================================
    @Test
    void muscleGain_advanced_sessions6_chuKyLapLaiSauBaNgay() {
        List<Map<MuscleGroup, Integer>> weekPlan =
                MuscleGroupSplitPlanner.buildWeekPlan(Goal.MUSCLE_GAIN, FitnessLevel.ADVANCED, 6);

        assertEquals(6, weekPlan.size());
        assertEquals(List.of(3, 3, 2, 3, 3, 2),
                weekPlan.stream().map(Map::size).toList(),
                "Số nhóm cơ/ngày phải theo đúng pattern Push(3)-Pull(3)-FullBody(2) lặp lại");

        Map<MuscleGroup, Integer> expectedPush = Map.of(
                MuscleGroup.CHEST, 3, MuscleGroup.SHOULDERS, 3, MuscleGroup.ARMS, 3);
        Map<MuscleGroup, Integer> expectedPull = Map.of(
                MuscleGroup.BACK, 3, MuscleGroup.CORE, 3, MuscleGroup.LEGS, 3);
        Map<MuscleGroup, Integer> expectedFullBody = Map.of(
                MuscleGroup.FULL_BODY, 3, MuscleGroup.CARDIO, 3);

        assertEquals(expectedPush, weekPlan.get(0));
        assertEquals(expectedPull, weekPlan.get(1));
        assertEquals(expectedFullBody, weekPlan.get(2));
        assertEquals(weekPlan.get(0), weekPlan.get(3), "Ngày 3 phải lặp lại đúng ngày 0");
        assertEquals(weekPlan.get(1), weekPlan.get(4), "Ngày 4 phải lặp lại đúng ngày 1");
        assertEquals(weekPlan.get(2), weekPlan.get(5), "Ngày 5 phải lặp lại đúng ngày 2");
    }

    // ============================================================
    // 5. FLEXIBILITY / BEGINNER / sessions=2: mỗi ngày đều Full body + Cardio,
    // f=2 cho cả 2 nhóm, BaseQuota=4 -> 2 bài/nhóm/ngày, 2 ngày giống hệt nhau.
    // (thay thế test "sessions2 full-body" cũ vốn viết cho MUSCLE_GAIN — giờ
    // MUSCLE_GAIN không còn hỗ trợ sessions=2 theo mục 4 I.docx).
    // ============================================================
    @Test
    void flexibility_beginner_sessions2_caHaiNgayGiongHetNhau() {
        List<Map<MuscleGroup, Integer>> weekPlan =
                MuscleGroupSplitPlanner.buildWeekPlan(Goal.FLEXIBILITY, FitnessLevel.BEGINNER, 2);

        Map<MuscleGroup, Integer> expected = Map.of(
                MuscleGroup.FULL_BODY, 2, MuscleGroup.CARDIO, 2);

        assertEquals(2, weekPlan.size());
        assertEquals(expected, weekPlan.get(0));
        assertEquals(expected, weekPlan.get(1));
    }

    // ============================================================
    // 6. Case đặc biệt: ENDURANCE / BEGINNER / sessions=4.
    // CARDIO xuất hiện cả 4 ngày (f=4) nhưng BaseQuota(BEGINNER)=3 < f=4
    // -> LRM: base=3/4=0, remainder=3 -> 3 ngày đầu được 1, ngày cuối = 0.
    // Đây là hành vi ĐÚNG theo thiết kế (không phải bug) — khoá lại bằng test
    // để không bị "sửa nhầm" thành luôn >= 1 trong tương lai.
    // ============================================================
    @Test
    void endurance_beginner_sessions4_cardioNgayCuoiBangKhong_doBaseQuotaThapHonTanSuat() {
        List<Map<MuscleGroup, Integer>> weekPlan =
                MuscleGroupSplitPlanner.buildWeekPlan(Goal.ENDURANCE, FitnessLevel.BEGINNER, 4);

        assertEquals(4, weekPlan.size());

        assertEquals(Map.of(MuscleGroup.CHEST, 3, MuscleGroup.SHOULDERS, 3, MuscleGroup.CARDIO, 1),
                weekPlan.get(0));
        assertEquals(Map.of(MuscleGroup.CORE, 3, MuscleGroup.LEGS, 3, MuscleGroup.CARDIO, 1),
                weekPlan.get(1));
        assertEquals(Map.of(MuscleGroup.ARMS, 3, MuscleGroup.BACK, 3, MuscleGroup.CARDIO, 1),
                weekPlan.get(2));
        assertEquals(Map.of(MuscleGroup.FULL_BODY, 3, MuscleGroup.CARDIO, 0),
                weekPlan.get(3), "CARDIO ngày cuối phải = 0 vì BaseQuota(3) < tần suất xuất hiện(4)");
    }

    // ============================================================
    // 7. Tổ hợp (Goal, sessions) KHÔNG có trong bảng 6.1.x phải ném lỗi rõ ràng,
    // thay vì âm thầm trả kết quả sai hoặc null.
    // ============================================================
    @Test
    void comboKhongHopLe_phaiNemIllegalStateException() {
        assertThrows(IllegalStateException.class,
                () -> MuscleGroupSplitPlanner.buildWeekPlan(Goal.MUSCLE_GAIN, FitnessLevel.BEGINNER, 2),
                "MUSCLE_GAIN không có cấu hình cho sessions=2 (minRequired đã là 4)");

        assertThrows(IllegalStateException.class,
                () -> MuscleGroupSplitPlanner.buildWeekPlan(Goal.FLEXIBILITY, FitnessLevel.BEGINNER, 5),
                "FLEXIBILITY không có cấu hình cho sessions=5 (maxRequired đã là 4)");

        assertThrows(IllegalStateException.class,
                () -> MuscleGroupSplitPlanner.buildWeekPlan(Goal.ENDURANCE, FitnessLevel.BEGINNER, 6),
                "ENDURANCE không có cấu hình cho sessions=6 (maxRequired đã là 5)");
    }

    // ---- Thêm case mới của bạn ở đây khi phát hiện lỗi thực tế ----
}