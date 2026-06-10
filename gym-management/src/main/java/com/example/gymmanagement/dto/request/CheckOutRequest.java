package com.example.gymmanagement.dto.request;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CheckOutRequest {
    // Bắt buộc: tỉ lệ hoàn thành 0-100
    private Integer completionRate;
    private String  notes;

    // Chỉ bắt buộc nếu là buổi cuối tuần
    private Double  checkoutWeight;
    private Double  checkoutBodyFat;

    private java.util.List<com.example.gymmanagement.dto.request.ExerciseLogRequest> exerciseLogs;
}