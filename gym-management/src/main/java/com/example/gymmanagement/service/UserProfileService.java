package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.UserProfileRequest;
import com.example.gymmanagement.entity.User;
import com.example.gymmanagement.entity.UserProfile;
import com.example.gymmanagement.repository.UserProfileRepository;
import com.example.gymmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    private final UserProfileRepository userProfileRepository;

    public UserProfile createOrUpdateProfile(
            Long userId,
            UserProfileRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(new UserProfile());

        profile.setUser(user);

        profile.setHeight(request.getHeight());

        profile.setWeight(request.getWeight());

        profile.setAge(request.getAge());

        profile.setGender(request.getGender());

        profile.setGoal(request.getGoal());

        profile.setFitnessLevel(request.getFitnessLevel());

        double bmi = request.getWeight()
                / (request.getHeight() * request.getHeight());

        profile.setBmi(bmi);

        return userProfileRepository.save(profile);
    }
}
