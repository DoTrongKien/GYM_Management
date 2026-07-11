package com.example.gymmanagement.service;

import com.example.gymmanagement.enums.Goal;
import com.example.gymmanagement.service.setrep.SetRepModels.LoadHint;
import com.example.gymmanagement.service.setrep.SetRepModels.SetRepResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mỗi khi phát hiện case set/rep hoặc phân loại thể trạng bị sai trong thực tế,
 * thêm 1 @Test mới vào đây — KHÔNG sửa trực tiếp trong FitnessCalculator để
 * tránh vá lỗi mà không có test giữ lại bằng chứng.
 */
class FitnessCalculatorTest {

    private final FitnessCalculator calculator = new FitnessCalculator();

    // ============================================================
    // 1. classifyBodyType — các case biên từng bị rơi vào default CAN_DOI
    // ============================================================

    @Test
    void bmiDuoi18_5_deltaAmSau_phaiLaCaoGay() {
        // height=180 -> wChuan=(180-100)*0.9=72 ; weight=54 -> delta=-18, bmi=16.7 (<18.5)
        FitnessCalculator.BodyType result =
                calculator.classifyBodyType(180.0, 54.0, null, "MALE");
        assertEquals(FitnessCalculator.BodyType.CAO_GAY, result);
    }

    @Test
    void bmiTren25_deltaDuoi8_khongConRoiVeCanDoiMacDinh() {
        // height=170 -> wChuan=(170-100)*0.9=63 ; weight=68 -> delta=5 (<=8)
        // Truyền bmi=26.0 (override) để chủ động rơi vào nhánh BMI>=25.
        // Trước fix: case này lọt qua hết các if rồi rơi vào "return CAN_DOI" mặc định (SAI).
        // Sau fix: BMI>=25 & delta<=8 -> phải là CO_BAP.
        FitnessCalculator.BodyType result =
                calculator.classifyBodyType(170.0, 68.0, 26.0, "MALE");
        assertEquals(FitnessCalculator.BodyType.CO_BAP, result);
    }

    @Test
    void bmiTren25_deltaTren8_phaiLaThuaCan() {
        // height=170 -> wChuan=63 ; weight=80 -> delta=17 (>8), bmi override=27.7
        FitnessCalculator.BodyType result =
                calculator.classifyBodyType(170.0, 80.0, 27.7, "MALE");
        assertEquals(FitnessCalculator.BodyType.THUA_CAN, result);
    }

    @Test
    void bmiTren25_bodyFatThap_duocXepVanDongVienKhongPhaiThuaCan() {
        // BMI cao nhưng bodyFat thấp -> phải là VAN_DONG_VIEN, không phải THUA_CAN
        FitnessCalculator.BodyType result =
                calculator.classifyBodyType(175.0, 85.0, null, "MALE", 15.0);
        assertEquals(FitnessCalculator.BodyType.VAN_DONG_VIEN, result);
    }

    @Test
    void bmiTren25_bodyFatCao_duocXepThuaCan() {
        FitnessCalculator.BodyType result =
                calculator.classifyBodyType(175.0, 85.0, null, "MALE", 30.0);
        assertEquals(FitnessCalculator.BodyType.THUA_CAN, result);
    }

    // ============================================================
    // 2. resolveFinalSetsReps — clamp theo training zone
    // ============================================================

    @Test
    void tangCo_Yeu_CaoGay_repBiClampVeTranVaGoiYTangTa() {
        SetRepResult r = calculator.resolveFinalSetsReps(
                FitnessCalculator.FsLevel.WEAK, Goal.MUSCLE_GAIN, FitnessCalculator.BodyType.CAO_GAY);

        assertEquals(12, r.reps(), "rep thô 12+2=14 phải bị clamp về trần 12");
        assertEquals(3, r.sets());
        assertEquals(LoadHint.INCREASE_WEIGHT, r.loadHint());
    }

    @Test
    void tangCo_XuatSac_VanDongVien_repBiClampVeSanVaGoiYGiamTa() {
        SetRepResult r = calculator.resolveFinalSetsReps(
                FitnessCalculator.FsLevel.EXCELLENT, Goal.MUSCLE_GAIN, FitnessCalculator.BodyType.VAN_DONG_VIEN);

        assertEquals(5, r.reps(), "rep thô 6-2=4 phải bị clamp về sàn 5");
        assertEquals(4, r.sets());
        assertEquals(LoadHint.DECREASE_WEIGHT, r.loadHint());
    }

    @Test
    void giamCan_Tot_CanDoi_khongBiClamp() {
        SetRepResult r = calculator.resolveFinalSetsReps(
                FitnessCalculator.FsLevel.GOOD, Goal.WEIGHT_LOSS, FitnessCalculator.BodyType.CAN_DOI);

        assertEquals(4, r.sets());
        assertEquals(14, r.reps());
        assertEquals(LoadHint.NONE, r.loadHint());
    }

    // ============================================================
    // 3. Quét toàn bộ ma trận — regression test, tự chạy lại mỗi lần sửa bảng
    // ============================================================

    @Test
    void quetToanBoMaTran_RepLuonNamTrongZoneVaSetLuonDuong() {
        for (Goal goal : Goal.values()) {
            for (FitnessCalculator.FsLevel fsLevel : FitnessCalculator.FsLevel.values()) {
                for (FitnessCalculator.BodyType bodyType : FitnessCalculator.BodyType.values()) {
                    SetRepResult r = calculator.resolveFinalSetsReps(fsLevel, goal, bodyType);
                    var zone = com.example.gymmanagement.service.setrep.TrainingZone.of(goal);

                    assertTrue(r.reps() >= zone.repFloor() && r.reps() <= zone.repCeiling(),
                            () -> "Vỡ zone tại " + goal + "/" + fsLevel + "/" + bodyType
                                    + " -> rep=" + r.reps());
                    assertTrue(r.sets() >= 1,
                            "Set không được <= 0 tại " + goal + "/" + fsLevel + "/" + bodyType);
                }
            }
        }
    }

    // ============================================================
    // 4. calculateFS — clamp + gender
    // ============================================================

    @Test
    void fs_luonNamTrong0_100_voiInputBatThuong() {
        double fs = calculator.calculateFS(90, 150.0, 200.0, "FEMALE"); // input cực đoan
        assertTrue(fs >= 0 && fs <= 100, "FS phải luôn trong [0,100], hiện tại = " + fs);
    }

    @Test
    void fs_nuVaNam_khacNhauKhiCungChieuCaoCanNang() {
        double fsNam = calculator.calculateFS(25, 165.0, 62.0, "MALE");
        double fsNu = calculator.calculateFS(25, 165.0, 62.0, "FEMALE");
        assertTrue(fsNam != fsNu, "Gender phải ảnh hưởng đến FS qua W_chuan, hiện đang bằng nhau");
    }

    // ---- Thêm case mới của bạn ở đây khi phát hiện lỗi thực tế ----
}