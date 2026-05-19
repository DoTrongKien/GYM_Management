package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.UserProfileRequest;
import com.example.gymmanagement.dto.response.UserProfileResponse;
import com.example.gymmanagement.entity.User;
import com.example.gymmanagement.entity.UserProfile;
import com.example.gymmanagement.repository.UserProfileRepository;
import com.example.gymmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    public UserProfileResponse saveOrUpdate(String email, UserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElse(UserProfile.builder().user(user).build());

        profile.setHeight(request.getHeight());
        profile.setWeight(request.getWeight());
        profile.setAge(request.getAge());
        profile.setGender(request.getGender());
        profile.setGoal(request.getGoal());
        profile.setFitnessLevel(request.getFitnessLevel());
        profile.setAvailableDaysPerWeek(request.getAvailableDaysPerWeek());
        profile.setPreferredSessionDuration(request.getPreferredSessionDuration());
        profile.setMedicalConditions(request.getMedicalConditions());

        // Calculate BMI
        if (request.getHeight() != null && request.getWeight() != null && request.getHeight() > 0) {
            double heightM = request.getHeight() / 100.0;
            double bmi = request.getWeight() / (heightM * heightM);
            profile.setBmi(Math.round(bmi * 10.0) / 10.0);
        }

        profileRepository.save(profile);
        return buildResponse(profile, user);
    }

    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not set up yet. Please complete your profile."));

        return buildResponse(profile, user);
    }

    public UserProfileResponse getProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for this user"));

        return buildResponse(profile, user);
    }

    private UserProfileResponse buildResponse(UserProfile profile, User user) {
        String bmiCategory = getBmiCategory(profile.getBmi());
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .age(profile.getAge())
                .gender(profile.getGender())
                .bmi(profile.getBmi())
                .bmiCategory(bmiCategory)
                .goal(profile.getGoal())
                .fitnessLevel(profile.getFitnessLevel())
                .availableDaysPerWeek(profile.getAvailableDaysPerWeek())
                .preferredSessionDuration(profile.getPreferredSessionDuration())
                .medicalConditions(profile.getMedicalConditions())
                .build();
    }

    private String getBmiCategory(Double bmi) {
        if (bmi == null) return "Unknown";
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal";
        if (bmi < 30.0) return "Overweight";
        return "Obese";
    }
}