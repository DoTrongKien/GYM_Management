package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.ProgressRequest;
import com.example.gymmanagement.dto.response.ProgressResponse;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressTrackingRepository progressRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public ProgressResponse addProgress(String email, ProgressRequest request) {
        User user = getUser(email);

        double bmi = 0;
        if (request.getWeight() != null && request.getHeight() != null && request.getHeight() > 0) {
            double h = request.getHeight() / 100.0;
            bmi = Math.round(request.getWeight() / (h * h) * 10.0) / 10.0;
        } else {
            // Use height from profile
            Optional<UserProfile> profile = profileRepository.findByUserId(user.getId());
            if (profile.isPresent() && profile.get().getHeight() != null && request.getWeight() != null) {
                double h = profile.get().getHeight() / 100.0;
                bmi = Math.round(request.getWeight() / (h * h) * 10.0) / 10.0;
            }
        }

        ProgressTracking pt = ProgressTracking.builder()
                .user(user)
                .weight(request.getWeight())
                .height(request.getHeight())
                .bmi(bmi > 0 ? bmi : null)
                .bodyFatPercentage(request.getBodyFatPercentage())
                .muscleMassKg(request.getMuscleMassKg())
                .chestCm(request.getChestCm())
                .waistCm(request.getWaistCm())
                .hipCm(request.getHipCm())
                .armCm(request.getArmCm())
                .thighCm(request.getThighCm())
                .recordedDate(request.getRecordedDate() != null ? request.getRecordedDate() : LocalDate.now())
                .notes(request.getNotes())
                .build();
        progressRepository.save(pt);

        // Update profile weight/bmi
        profileRepository.findByUserId(user.getId()).ifPresent(p -> {
            if (request.getWeight() != null) p.setWeight(request.getWeight());
            if (bmi > 0) p.setBmi(bmi);
            profileRepository.save(p);
        });

        return buildResponse(pt, null);
    }

    public List<ProgressResponse> getMyProgress(String email) {
        User user = getUser(email);
        List<ProgressTracking> list = progressRepository.findByUserIdOrderByDateAsc(user.getId());

        return java.util.stream.IntStream.range(0, list.size()).mapToObj(i -> {
            ProgressTracking pt = list.get(i);
            Double weightChange = null;
            if (i > 0) {
                ProgressTracking prev = list.get(i - 1);
                if (pt.getWeight() != null && prev.getWeight() != null) {
                    weightChange = Math.round((pt.getWeight() - prev.getWeight()) * 10.0) / 10.0;
                }
            }
            return buildResponse(pt, weightChange);
        }).collect(Collectors.toList());
    }

    public ProgressResponse getLatestProgress(String email) {
        User user = getUser(email);
        return progressRepository.findFirstByUserIdOrderByRecordedDateDesc(user.getId())
                .map(p -> buildResponse(p, null))
                .orElseThrow(() -> new RuntimeException("No progress records found. Start tracking today!"));
    }

    public ProgressResponse updateProgress(String email, Long id, ProgressRequest request) {
        User user = getUser(email);
        ProgressTracking pt = progressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
        if (!pt.getUser().getId().equals(user.getId())) throw new RuntimeException("Access denied");

        if (request.getWeight() != null) pt.setWeight(request.getWeight());
        if (request.getBodyFatPercentage() != null) pt.setBodyFatPercentage(request.getBodyFatPercentage());
        if (request.getWaistCm() != null) pt.setWaistCm(request.getWaistCm());
        if (request.getChestCm() != null) pt.setChestCm(request.getChestCm());
        if (request.getNotes() != null) pt.setNotes(request.getNotes());
        progressRepository.save(pt);
        return buildResponse(pt, null);
    }

    private ProgressResponse buildResponse(ProgressTracking pt, Double weightChange) {
        return ProgressResponse.builder()
                .id(pt.getId())
                .weight(pt.getWeight())
                .height(pt.getHeight())
                .bmi(pt.getBmi())
                .bodyFatPercentage(pt.getBodyFatPercentage())
                .muscleMassKg(pt.getMuscleMassKg())
                .chestCm(pt.getChestCm())
                .waistCm(pt.getWaistCm())
                .hipCm(pt.getHipCm())
                .armCm(pt.getArmCm())
                .thighCm(pt.getThighCm())
                .recordedDate(pt.getRecordedDate())
                .notes(pt.getNotes())
                .weightChange(weightChange)
                .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }
}