package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
}
