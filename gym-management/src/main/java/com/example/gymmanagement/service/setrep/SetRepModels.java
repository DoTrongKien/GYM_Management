package com.example.gymmanagement.service.setrep;

public final class SetRepModels {
    private SetRepModels() {}

    /** Gợi ý điều chỉnh tạ khi rep bị chặn ở biên training zone thay vì phá zone. */
    public enum LoadHint {
        NONE, INCREASE_WEIGHT, DECREASE_WEIGHT
    }

    /** Kết quả set/rep cuối cùng sau khi cộng base + adjustment + clamp. */
    public record SetRepResult(int sets, int reps, LoadHint loadHint) {}
}