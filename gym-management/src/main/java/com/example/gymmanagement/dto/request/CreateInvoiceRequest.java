package com.example.gymmanagement.dto.request;

import com.example.gymmanagement.enums.MembershipType;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateInvoiceRequest {
    private MembershipType membershipType;
}