package com.example.gymmanagement.service.setrep;

import com.example.gymmanagement.enums.Goal;

import java.util.EnumMap;
import java.util.Map;

/**
 * Vùng rep chuẩn (training zone) cho từng mục tiêu — dùng để clamp rep
 * sau khi cộng base (theo FS) + adjustment (theo BodyType), tránh bài tập
 * bị đẩy sang zone khác (VD: Tăng cơ 8-12 rep bị cộng dồn lên 14-15 sẽ
 * ngả sang vùng Sức bền).
 *
 * Nếu sau này bạn cần đổi floor/ceiling, CHỈ sửa ở đây — không sửa rải rác
 * trong FitnessCalculator hay WorkoutPlanService.
 */
public final class TrainingZone {
    private TrainingZone() {}

    public record Zone(int repFloor, int repCeiling) {}

    private static final Map<Goal, Zone> ZONES = new EnumMap<>(Goal.class);
    static {
        ZONES.put(Goal.MUSCLE_GAIN, new Zone(5, 12));
        ZONES.put(Goal.WEIGHT_LOSS, new Zone(12, 20));
        ZONES.put(Goal.ENDURANCE, new Zone(12, 25));
        ZONES.put(Goal.FLEXIBILITY, new Zone(8, 15));
        ZONES.put(Goal.MAINTENANCE, new Zone(8, 15));
    }

    public static Zone of(Goal goal) {
        Zone z = ZONES.get(goal);
        if (z == null) {
            throw new IllegalStateException("Chưa khai báo training zone cho Goal=" + goal
                    + " — thêm vào TrainingZone.ZONES trước khi dùng.");
        }
        return z;
    }
}