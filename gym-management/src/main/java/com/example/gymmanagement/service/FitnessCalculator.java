package com.example.gymmanagement.service;

import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import com.example.gymmanagement.service.setrep.SetRepModels;
import com.example.gymmanagement.service.setrep.SetRepModels.LoadHint;
import com.example.gymmanagement.service.setrep.SetRepModels.SetRepResult;
import com.example.gymmanagement.service.setrep.TrainingZone;
import org.springframework.stereotype.Component;

@Component
public class FitnessCalculator {
    public enum FsLevel { EXCELLENT, GOOD, AVERAGE, WEAK }

    public enum BodyType {
        CAO_GAY, GAY_CAN_DOI, CAN_DOI, CO_BAP, VAN_DONG_VIEN, THUA_CAN
    }

    // ── Tính Fitness Score (0–100) ────────────────────────────
    // FIX: thêm gender để tính W_chuan chính xác hơn cho nữ.
    public double calculateFS(Integer age, Double height, Double weight, String gender) {
        if (age == null || height == null || weight == null) return 60.0;
        double sTuoi = calcSTuoi(age);
        double sCannang = calcSCannang(height, weight, gender);
        double fs = sTuoi * 0.4 + sCannang * 0.6;
        return clamp(fs, 0, 100);
    }

    /** Overload giữ tương thích ngược cho code cũ chưa truyền gender. */
    public double calculateFS(Integer age, Double height, Double weight) {
        return calculateFS(age, height, weight, null);
    }

    private double calcSTuoi(int age) {
        if (age >= 18 && age <= 25) return 100.0;
        if (age < 18) return clamp(100.0 - (18 - age) * 2.0, 0, 100);
        if (age <= 40) return clamp(100.0 - (age - 25) * 1.5, 0, 100);
        int capped = Math.min(age, 80); // cap tuổi ở 80 TRƯỚC khi tính, không cap kết quả sau
        return clamp(80.0 - (capped - 40) * 2.0, 0, 100);
    }

    private double calcSCannang(double heightCm, double weightKg, String gender) {
        double wChuan = wChuan(heightCm, gender);
        if (wChuan <= 0) return 50.0;
        double doLech = Math.abs(weightKg - wChuan) / wChuan * 100.0;
        return clamp(100.0 - doLech * 2.0, 0, 100);
    }

    /** W_chuan dùng chung cho cả FS và BodyType để 2 điểm số nhất quán với nhau. */
     double wChuan(double heightCm, String gender) {
        double base = (heightCm - 100) * 0.9;
        return isFemale(gender) ? base * 0.9 : base;
    }

    private boolean isFemale(String gender) {
        if (gender == null) return false;
        String g = gender.trim().toUpperCase();
        return g.equals("FEMALE") || g.equals("F") || g.equals("NU") || g.equals("NỮ");
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ── FS → FsLevel ─────────────────────────────────────────
    public FsLevel getFsLevel(double fs) {
        if (fs >= 85) return FsLevel.EXCELLENT;
        if (fs >= 65) return FsLevel.GOOD;
        if (fs >= 50) return FsLevel.AVERAGE;
        return FsLevel.WEAK;
    }

    public FitnessLevel fsToFitnessLevel(double fs) {
        if (fs >= 85) return FitnessLevel.ADVANCED;
        if (fs >= 65) return FitnessLevel.INTERMEDIATE;
        return FitnessLevel.BEGINNER;
    }

    // ── Phân loại BodyType ────────────────────────────────────
    // FIX: đổi trục chính sang BMI để không còn khoảng (Delta, BMI) nào
    // rơi tự do vào nhánh mặc định CAN_DOI như bản cũ.
    public BodyType classifyBodyType(Double heightCm, Double weightKg, Double bmi, String gender) {
        if (heightCm == null || weightKg == null) return BodyType.CAN_DOI;
        double wChuanVal = wChuan(heightCm, gender);
        double delta = weightKg - wChuanVal;
        double bmiVal = bmi != null ? bmi : weightKg / Math.pow(heightCm / 100.0, 2);

        if (bmiVal < 18.5) {
            return delta < -8 ? BodyType.CAO_GAY : BodyType.GAY_CAN_DOI;
        }
        if (bmiVal < 25) {
            if (delta < -5) return BodyType.GAY_CAN_DOI;
            if (delta < 5) return BodyType.CAN_DOI;
            if (delta <= 8) return BodyType.CO_BAP;
            return BodyType.VAN_DONG_VIEN;
        }
        // bmiVal >= 25: không còn nhánh rơi tự do — chỉ còn CO_BAP hoặc THUA_CAN
        return delta <= 8 ? BodyType.CO_BAP : BodyType.THUA_CAN;
    }

    /** Overload dùng bodyFatPercentage để phân biệt "cơ nhiều" và "mỡ nhiều" khi BMI >= 25. */
    public BodyType classifyBodyType(Double heightCm, Double weightKg, Double bmi,
                                     String gender, Double bodyFatPercentage) {
        if (heightCm == null || weightKg == null) return BodyType.CAN_DOI;
        double bmiVal = bmi != null ? bmi : weightKg / Math.pow(heightCm / 100.0, 2);

        if (bmiVal >= 25 && bodyFatPercentage != null) {
            boolean female = isFemale(gender);
            double vdvMax = female ? 25 : 18;
            double coBapMax = female ? 31 : 24;
            if (bodyFatPercentage < vdvMax) return BodyType.VAN_DONG_VIEN;
            if (bodyFatPercentage <= coBapMax) return BodyType.CO_BAP;
            return BodyType.THUA_CAN;
        }
        return classifyBodyType(heightCm, weightKg, bmi, gender);
    }

    /** Overload giữ tương thích ngược cho code cũ (không có gender). */
    public BodyType classifyBodyType(Double heightCm, Double weightKg, Double bmi) {
        return classifyBodyType(heightCm, weightKg, bmi, null);
    }

    // ── Sets/Reps theo FsLevel × Goal ────────────────────────
    // FIX: đồng bộ đúng số liệu đã chốt ở bảng 3.docx (không còn là số nháp cũ).
    // ĐÃ XOÁ: case FLEXIBILITY (Goal FLEXIBILITY không còn tồn tại trong hệ thống).
    public int[] calcSetsRepsByFS(FsLevel fsLevel, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN -> switch (fsLevel) {
                case EXCELLENT -> new int[]{4, 6};
                case GOOD -> new int[]{4, 8};
                case AVERAGE -> new int[]{3, 10};
                case WEAK -> new int[]{3, 12};
            };
            case WEIGHT_LOSS -> switch (fsLevel) {
                case EXCELLENT -> new int[]{4, 15};
                case GOOD -> new int[]{4, 14};
                case AVERAGE -> new int[]{3, 13};
                case WEAK -> new int[]{3, 12};
            };
            case ENDURANCE -> switch (fsLevel) {
                case EXCELLENT -> new int[]{4, 16};
                case GOOD -> new int[]{4, 14};
                case AVERAGE -> new int[]{3, 13};
                case WEAK -> new int[]{3, 12};
            };
            case MAINTENANCE -> switch (fsLevel) {
                case EXCELLENT -> new int[]{3, 10};
                case GOOD -> new int[]{3, 11};
                case AVERAGE -> new int[]{3, 12};
                case WEAK -> new int[]{3, 13};
            };
        };
    }

    // ── Điều chỉnh Sets/Reps theo BodyType × Goal ────────────
    // FIX: WEIGHT_LOSS và ENDURANCE trước đây bị copy-paste giống hệt nhau — đã tách riêng.
    // ĐÃ XOÁ: case FLEXIBILITY.
    public int[] bodyTypeAdjustment(BodyType bodyType, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN -> switch (bodyType) {
                case CAO_GAY -> new int[]{0, +2};
                case GAY_CAN_DOI -> new int[]{0, +1};
                case CAN_DOI -> new int[]{0, 0};
                case CO_BAP -> new int[]{0, -1};
                case VAN_DONG_VIEN -> new int[]{0, -2};
                case THUA_CAN -> new int[]{+1, 0};
            };
            case WEIGHT_LOSS -> switch (bodyType) {
                case CAO_GAY -> new int[]{0, 0};
                case GAY_CAN_DOI -> new int[]{0, 0};
                case CAN_DOI -> new int[]{0, 0};
                case CO_BAP -> new int[]{+1, 0};
                case VAN_DONG_VIEN -> new int[]{+1, +2};
                case THUA_CAN -> new int[]{+2, 0};
            };
            case ENDURANCE -> switch (bodyType) {
                case CAO_GAY -> new int[]{0, +1};
                case GAY_CAN_DOI -> new int[]{0, +1};
                case CAN_DOI -> new int[]{0, 0};
                case CO_BAP -> new int[]{+1, 0};
                case VAN_DONG_VIEN -> new int[]{+1, +1};
                case THUA_CAN -> new int[]{+1, 0};
            };
            case MAINTENANCE -> switch (bodyType) {
                case CAO_GAY -> new int[]{0, +1};
                case GAY_CAN_DOI -> new int[]{0, 0};
                case CAN_DOI -> new int[]{0, 0};
                case CO_BAP -> new int[]{0, -1};
                case VAN_DONG_VIEN -> new int[]{0, -1};
                case THUA_CAN -> new int[]{+1, 0};
            };
        };
    }

    // ── MỚI: cộng dồn base + adjustment rồi CLAMP theo training zone ──
    // Đây là điểm WorkoutPlanService nên gọi thay vì tự cộng tay bằng
    // Math.max(1, baseSR[0]+adj[0]) / Math.max(4, baseSR[1]+adj[1]) như hiện tại,
    // vì cách cộng tay hiện tại KHÔNG có trần rep -> có thể vỡ training zone.
    public SetRepResult resolveFinalSetsReps(FsLevel fsLevel, Goal goal, BodyType bodyType) {
        int[] base = calcSetsRepsByFS(fsLevel, goal);
        int[] adj = bodyTypeAdjustment(bodyType, goal);

        int finalSet = Math.max(1, base[0] + adj[0]);
        int rawRep = base[1] + adj[1];

        TrainingZone.Zone zone = TrainingZone.of(goal);
        int clampedRep = Math.max(zone.repFloor(), Math.min(zone.repCeiling(), rawRep));

        LoadHint hint = SetRepModels.LoadHint.NONE;
        if (rawRep > zone.repCeiling()) {
            hint = LoadHint.INCREASE_WEIGHT;
        } else if (rawRep < zone.repFloor()) {
            hint = LoadHint.DECREASE_WEIGHT;
        }

        return new SetRepResult(finalSet, clampedRep, hint);
    }
}