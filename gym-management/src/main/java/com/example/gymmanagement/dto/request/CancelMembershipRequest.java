package com.example.gymmanagement.dto.request;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CancelMembershipRequest {
    private String reason;
}