package com.example.gymmanagement.dto.response;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String role;
    private Long userId;
    private String fullName;
    private String email;
    private Boolean emailVerified;
}