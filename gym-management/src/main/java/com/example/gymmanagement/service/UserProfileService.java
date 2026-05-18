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

    // Dùng cho ROLE_USER: lấy user từ email trong token
    public UserProfile createOrUpdateProfile(
            String email,
            UserProfileRequest request
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return saveProfile(user, request);
    }

    // Dùng cho ROLE_ADMIN: lấy user từ userId trên URL
    public UserProfile createOrUpdateProfile(
            Long userId,
            UserProfileRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return saveProfile(user, request);
    }

    // Logic lưu profile dùng chung
    private UserProfile saveProfile(User user, UserProfileRequest request) {

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElse(new UserProfile());

        profile.setUser(user);
        profile.setHeight(request.getHeight());
        profile.setWeight(request.getWeight());
        profile.setAge(request.getAge());
        profile.setGender(request.getGender());
        profile.setGoal(request.getGoal());
        profile.setFitnessLevel(request.getFitnessLevel());

        // height đơn vị là mét (ví dụ: 1.75)
        double bmi = request.getWeight()
                / (request.getHeight() * request.getHeight());
        profile.setBmi(Math.round(bmi * 100.0) / 100.0); // làm tròn 2 chữ số

        return userProfileRepository.save(profile);
    }
}