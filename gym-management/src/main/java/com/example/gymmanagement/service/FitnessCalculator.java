package com.example.gymmanagement.service;
import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.enums.Goal;
import org.springframework.stereotype.Component;

@Component
public class FitnessCalculator {
    public enum FsLevel { EXCELLENT, GOOD, AVERAGE, WEAK }

    public enum BodyType {
        CAO_GAY,        // Delta < -8
        GAY_CAN_DOI,    // Delta [-8, +2] và BMI < 18.5
        CAN_DOI,        // Delta [-5, +5] và BMI [18.5, 25)
        CO_BAP,         // Delta (+5, +8] và BMI [18.5, 25]
        VAN_DONG_VIEN,  // Delta > +8 và BMI < 25
        THUA_CAN        // Delta > +10 hoặc (BMI ≥ 25 và Delta > 5)
    }

    // ── Tính Fitness Score (0–100) ────────────────────────────
    public double calculateFS(Integer age, Double height, Double weight) {
        if (age == null || height == null || weight == null) return 60.0;
        double sTuoi    = calcSTuoi(age);
        double sCannang = calcSCannang(height, weight);
        return sTuoi * 0.4 + sCannang * 0.6;
    }

    private double calcSTuoi(int age) {
        if (age >= 18 && age <= 25) return 100.0;
        if (age < 18)               return Math.max(0, 100.0 - (18 - age) * 2.0);
        if (age <= 40)              return 100.0 - (age - 25) * 1.5;
        int capped = Math.min(age, 80);
        return Math.max(0, 80.0 - (capped - 40) * 2.0);
    }

    private double calcSCannang(double heightCm, double weightKg) {
        double wChuan = (heightCm - 100) * 0.9;
        if (wChuan <= 0) return 50.0;
        double doLech = Math.abs(weightKg - wChuan) / wChuan * 100.0;
        return Math.max(0, 100.0 - doLech * 2.0);
    }

    // ── FS → FsLevel ─────────────────────────────────────────
    public FsLevel getFsLevel(double fs) {
        if (fs >= 85) return FsLevel.EXCELLENT;
        if (fs >= 65) return FsLevel.GOOD;
        if (fs >= 50) return FsLevel.AVERAGE;
        return FsLevel.WEAK;
    }

    // ── FS → FitnessLevel (dùng để lưu vào plan nếu cần) ────
    public FitnessLevel fsToFitnessLevel(double fs) {
        if (fs >= 85) return FitnessLevel.ADVANCED;
        if (fs >= 65) return FitnessLevel.INTERMEDIATE;
        return FitnessLevel.BEGINNER;
    }

    // ── Phân loại BodyType ────────────────────────────────────
    public BodyType classifyBodyType(Double heightCm, Double weightKg, Double bmi) {
        if (heightCm == null || weightKg == null) return BodyType.CAN_DOI;
        double wChuan = (heightCm - 100) * 0.9;
        double delta  = weightKg - wChuan;
        double bmiVal = bmi != null ? bmi : weightKg / Math.pow(heightCm / 100.0, 2);

        if (delta > 10 || (bmiVal >= 25 && delta > 5)) return BodyType.THUA_CAN;
        if (delta > 8  && bmiVal < 25)                 return BodyType.VAN_DONG_VIEN;
        if (delta > 5  && delta <= 8 && bmiVal <= 25)  return BodyType.CO_BAP;
        if (delta < -8)                                 return BodyType.CAO_GAY;
        if (delta >= -8 && delta <= 2 && bmiVal < 18.5) return BodyType.GAY_CAN_DOI;
        return BodyType.CAN_DOI;
    }

    // ── Sets/Reps theo FsLevel × Goal ────────────────────────
    // Dựa theo bảng 4.x trong tài liệu hệ thống thể lực
    public int[] calcSetsRepsByFS(FsLevel fsLevel, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN -> switch (fsLevel) {
                case EXCELLENT -> new int[]{6, 8};
                case GOOD      -> new int[]{5, 10};
                case AVERAGE   -> new int[]{3, 12};
                case WEAK      -> new int[]{3, 10};
            };
            case WEIGHT_LOSS -> switch (fsLevel) {
                case EXCELLENT -> new int[]{6, 12};
                case GOOD      -> new int[]{5, 14};
                case AVERAGE   -> new int[]{3, 14};
                case WEAK      -> new int[]{3, 12};
            };
            case ENDURANCE -> switch (fsLevel) {
                case EXCELLENT -> new int[]{5, 20};
                case GOOD      -> new int[]{4, 18};
                case AVERAGE   -> new int[]{3, 15};
                case WEAK      -> new int[]{3, 12};
            };
            case FLEXIBILITY -> switch (fsLevel) {
                case EXCELLENT -> new int[]{4, 12};
                case GOOD      -> new int[]{3, 12};
                case AVERAGE   -> new int[]{3, 10};
                case WEAK      -> new int[]{2, 10};
            };
            case MAINTENANCE -> switch (fsLevel) {
                case EXCELLENT -> new int[]{6, 10};
                case GOOD      -> new int[]{5, 12};
                case AVERAGE   -> new int[]{3, 13};
                case WEAK      -> new int[]{3, 11};
            };
        };
    }

    // ── Điều chỉnh Sets/Reps theo BodyType × Goal ────────────
    // Trả về [deltaSet, deltaRep] — cộng vào sets/reps gốc
    // Chỉ điều chỉnh số lượng (sets/reps), KHÔNG điều chỉnh % tạ
    // (% tạ do Completion Rate quyết định qua weightAdjustmentNote)
    public int[] bodyTypeAdjustment(BodyType bodyType, Goal goal) {
        return switch (goal) {
            case MUSCLE_GAIN -> switch (bodyType) {
                case CAO_GAY        -> new int[]{ 0, +1};
                case GAY_CAN_DOI    -> new int[]{ 0, +1};
                case CAN_DOI        -> new int[]{ 0,  0};
                case CO_BAP         -> new int[]{ 0, -2};
                case VAN_DONG_VIEN  -> new int[]{ 0, -2};
                case THUA_CAN       -> new int[]{+1,  0};
            };
            case WEIGHT_LOSS -> switch (bodyType) {
                case CAO_GAY        -> new int[]{ 0, +2};
                case GAY_CAN_DOI    -> new int[]{ 0,  0};
                case CAN_DOI        -> new int[]{ 0,  0};
                case CO_BAP         -> new int[]{ 0, +2};
                case VAN_DONG_VIEN  -> new int[]{+1,  0};
                case THUA_CAN       -> new int[]{+2,  0};
            };
            case ENDURANCE -> switch (bodyType) {
                case CAO_GAY        -> new int[]{ 0, +2};
                case GAY_CAN_DOI    -> new int[]{ 0,  0};
                case CAN_DOI        -> new int[]{ 0,  0};
                case CO_BAP         -> new int[]{ 0, +2};
                case VAN_DONG_VIEN  -> new int[]{+1,  0};
                case THUA_CAN       -> new int[]{+2,  0};
            };
            case FLEXIBILITY -> switch (bodyType) {
                case CAO_GAY        -> new int[]{ 0, +1};
                case GAY_CAN_DOI    -> new int[]{ 0,  0};
                case CAN_DOI        -> new int[]{ 0,  0};
                case CO_BAP         -> new int[]{ 0, +2};
                case VAN_DONG_VIEN  -> new int[]{+1,  0};
                case THUA_CAN       -> new int[]{ 0, +2};
            };
            case MAINTENANCE -> switch (bodyType) {
                case CAO_GAY        -> new int[]{ 0, +1};
                case GAY_CAN_DOI    -> new int[]{ 0,  0};
                case CAN_DOI        -> new int[]{ 0,  0};
                case CO_BAP         -> new int[]{ 0, -1};
                case VAN_DONG_VIEN  -> new int[]{ 0, -1};
                case THUA_CAN       -> new int[]{+1,  0};
            };
        };
    }
}
