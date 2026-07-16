package com.example.gymmanagement.repository;

import com.example.gymmanagement.entity.EnduranceTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnduranceTestRepository extends JpaRepository<EnduranceTest, Long> {
    Optional<EnduranceTest> findByUserId(Long userId);
}