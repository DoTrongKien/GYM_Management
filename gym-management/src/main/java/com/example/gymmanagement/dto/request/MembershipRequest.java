package com.example.gymmanagement.dto.request;
import com.example.gymmanagement.enums.MembershipType;
import lombok.*;
import java.time.LocalDate;
@Data @NoArgsConstructor @AllArgsConstructor
public class MembershipRequest {
    private MembershipType membershipType;
    private LocalDate startDate;
    private String paymentMethod;
    private String notes;
}