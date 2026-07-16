package com.example.gymmanagement.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EnduranceTestResponse {
    private Long id;
    private Integer pushupReps;
    private Integer plankSeconds;
    private Integer squatReps;
    private LocalDateTime testedAt;
}