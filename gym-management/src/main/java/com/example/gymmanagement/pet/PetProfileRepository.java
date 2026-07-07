package com.example.gymmanagement.pet;

// ============================================================
// FILE MỚI: src/main/java/packagecom/example/gymmanagement/repository/PetProfileRepository.java
// ============================================================

import com.example.gymmanagement.pet.PetProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {
    Optional<PetProfile> findByUserId(Long userId);
}