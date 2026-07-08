package com.example.gymmanagement.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CheckOutRequest {
    // ĐÃ BỎ: completionRate (Integer) - giờ tính tự động từ exerciseLogs
    private Double checkoutWeight;
    private Double checkoutBodyFat;
    private String notes;
    private List<ExerciseLogRequest> exerciseLogs;
}