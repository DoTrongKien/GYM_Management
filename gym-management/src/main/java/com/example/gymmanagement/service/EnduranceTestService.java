package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.SubmitEnduranceTestRequest;
import com.example.gymmanagement.dto.response.EnduranceTestResponse;
import com.example.gymmanagement.entity.EnduranceTest;
import com.example.gymmanagement.entity.User;
import com.example.gymmanagement.repository.EnduranceTestRepository;
import com.example.gymmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

// ── Patch 6: EnduranceTestService chỉ chịu trách nhiệm UPSERT/GET EnduranceTest.
// KHÔNG đọc, KHÔNG ghi WorkoutPlan/WorkoutPlanRepository/WorkoutPlanService.
// Không tạo phụ thuộc chéo giữa EnduranceTest và WorkoutPlan (theo LOCKED). ──
@Service
@RequiredArgsConstructor
public class EnduranceTestService {

    private final EnduranceTestRepository enduranceTestRepo;
    private final UserRepository userRepo;

    @Transactional
    public EnduranceTestResponse submitTest(String email, SubmitEnduranceTestRequest req) {
        User user = getUser(email);

        EnduranceTest test = enduranceTestRepo.findByUserId(user.getId())
                .orElseGet(() -> EnduranceTest.builder().user(user).build());

        test.setPushupReps(req.getPushupReps());
        test.setPlankSeconds(req.getPlankSeconds());
        test.setSquatReps(req.getSquatReps());
        test.setTestedAt(LocalDateTime.now());

        enduranceTestRepo.save(test);
        return toResponse(test);
    }

    public Optional<EnduranceTestResponse> getMyTest(String email) {
        User user = getUser(email);
        return enduranceTestRepo.findByUserId(user.getId()).map(this::toResponse);
    }

    private EnduranceTestResponse toResponse(EnduranceTest test) {
        return EnduranceTestResponse.builder()
                .id(test.getId())
                .pushupReps(test.getPushupReps())
                .plankSeconds(test.getPlankSeconds())
                .squatReps(test.getSquatReps())
                .testedAt(test.getTestedAt())
                .build();
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}