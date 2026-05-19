package com.example.gymmanagement.dto.request;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor
public class CheckInRequest {
    private Long sessionId;
    private List<ExerciseLogRequest> exerciseLogs;
}