package com.example.gymmanagement.dto;

import lombok.Data;

@Data
public class UserProfileRequest {

    private Double height;

    private Double weight;

    private Integer age;

    private String gender;

    private String goal;

    private String fitnessLevel;
}