package com.example.gymmanagement.config;

import com.example.gymmanagement.entity.Exercise;
import com.example.gymmanagement.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Backfill 1 lần: các bài tập cũ (tạo trước khi có field staminaCost) đang có
 * stamina_cost = NULL trong DB thật (Hibernate ddl-auto=update không tự set default
 * cho dữ liệu cũ). Runner này set về 10 cho các dòng NULL, chạy mỗi lần start app
 * nhưng chỉ update khi thực sự còn NULL nên vô hại nếu chạy nhiều lần.
 */
@Component
@RequiredArgsConstructor
public class DataBackfillRunner implements CommandLineRunner {

    private final ExerciseRepository exerciseRepo;

    @Override
    public void run(String... args) {
        List<Exercise> toFix = exerciseRepo.findAll().stream()
                .filter(e -> e.getStaminaCost() == null)
                .collect(Collectors.toList());

        if (!toFix.isEmpty()) {
            toFix.forEach(e -> e.setStaminaCost(10));
            exerciseRepo.saveAll(toFix);
            System.out.println("[Backfill] Đã set staminaCost=10 cho " + toFix.size() + " bài tập cũ.");
        }
    }
}