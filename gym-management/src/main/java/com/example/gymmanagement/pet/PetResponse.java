// ============================================================
// GHI ĐÈ FILE: src/main/java/com/example/gymmanagement/pet/PetResponse.java
// ============================================================
package com.example.gymmanagement.pet;

import lombok.*;

@Data @Builder
public class PetResponse {
    private PetStage stage;
    private Integer currentStreak;
    private Integer missedStreak;
    private AuraTier auraTier;
    private Integer webCount;
}