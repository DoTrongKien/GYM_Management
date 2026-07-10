package com.example.gymmanagement.dto.request;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor
public class RatingRequest {
    private Integer rating;
    private String title;
    private String comment;
    private String serviceType;
    private Boolean isPublic = true;
}