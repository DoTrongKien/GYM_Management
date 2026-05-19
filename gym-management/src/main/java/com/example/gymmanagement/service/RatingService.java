package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.RatingRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final ServiceRatingRepository ratingRepository;
    private final UserRepository userRepository;

    public ServiceRating addRating(String email, RatingRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5)
            throw new RuntimeException("Rating must be between 1 and 5");

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        ServiceRating rating = ServiceRating.builder()
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .serviceType(request.getServiceType())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .build();
        return ratingRepository.save(rating);
    }

    public List<ServiceRating> getPublicRatings() {
        return ratingRepository.findByIsPublicTrueOrderByCreatedAtDesc();
    }

    public List<ServiceRating> getMyRatings(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return ratingRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Map<String, Double> getAverageRatings() {
        return Map.of(
                "WORKOUT_PLAN", getAvg("WORKOUT_PLAN"),
                "NUTRITION", getAvg("NUTRITION"),
                "FACILITY", getAvg("FACILITY"),
                "TRAINER", getAvg("TRAINER")
        );
    }

    private double getAvg(String type) {
        Double avg = ratingRepository.getAverageRatingByType(type);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }
}