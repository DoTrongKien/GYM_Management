package com.example.gymmanagement.dto.request;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class EnrollSessionRequest {
    private Long      planDayId;
    private Long      planId;
    private LocalDate sessionDate;
    private LocalTime scheduledTime;
    private Integer   weekNumber;
    private Boolean   isLastSessionOfWeek;  // true nếu đây là buổi cuối tuần
    private String    customSessionName;
}