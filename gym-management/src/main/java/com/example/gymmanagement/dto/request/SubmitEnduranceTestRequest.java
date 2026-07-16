package com.example.gymmanagement.dto.request;

import lombok.Data;

@Data
public class SubmitEnduranceTestRequest {
    private Integer pushupReps;
    private Integer plankSeconds;
    private Integer squatReps;
}