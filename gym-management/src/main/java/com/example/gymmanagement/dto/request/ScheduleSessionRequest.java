package com.example.gymmanagement.dto.request;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
@Data @NoArgsConstructor @AllArgsConstructor
public class ScheduleSessionRequest {
    private LocalDate sessionDate;
    private LocalTime scheduledTime;
    private String customSessionName;
    private Long planDayId;   // optional: link to a plan day
}