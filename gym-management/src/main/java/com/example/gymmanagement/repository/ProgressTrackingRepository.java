package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.ProgressTracking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressTrackingRepository extends JpaRepository<ProgressTracking, Long> {
}
