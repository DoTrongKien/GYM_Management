package com.example.gymmanagement.dto.request;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class EnrollSessionRequest {
    private Long planDayId;       // ngày mẫu muốn đăng ký (null nếu custom)
    private Long planId;          // plan đang theo
    private LocalDate sessionDate;
    private LocalTime scheduledTime;
    private Integer weekNumber;   // tuần mấy trong plan
    private String customSessionName; // nếu tự đặt lịch
}