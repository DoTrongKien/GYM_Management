package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.request.AdminReplyRequest;
import com.example.gymmanagement.dto.request.RatingRequest;
import com.example.gymmanagement.dto.response.RatingResponse;
import com.example.gymmanagement.entity.ServiceRating;
import com.example.gymmanagement.entity.User;
import com.example.gymmanagement.repository.ServiceRatingRepository;
import com.example.gymmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final ServiceRatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ─────────────────────────────────────────────
    // USER ADD RATING
    // ─────────────────────────────────────────────
    public RatingResponse addRating(String email, RatingRequest request) {

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ServiceRating rating = ServiceRating.builder()
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .serviceType(request.getServiceType())
                .isPublic(
                        request.getIsPublic() != null
                                ? request.getIsPublic()
                                : true
                )
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(ratingRepository.save(rating));
    }

    // ─────────────────────────────────────────────
    // PUBLIC RATINGS
    // ─────────────────────────────────────────────
    public List<RatingResponse> getPublicRatings() {

        return ratingRepository
                .findByIsPublicTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // MY RATINGS
    // ─────────────────────────────────────────────
    public List<RatingResponse> getMyRatings(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ratingRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // ADMIN GET ALL RATINGS
    // ─────────────────────────────────────────────
    public List<RatingResponse> getAllRatings() {

        return ratingRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                ServiceRating::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // ADMIN REPLY
    // ─────────────────────────────────────────────
    public RatingResponse adminReply(
            Long ratingId,
            AdminReplyRequest request
    ) {

        ServiceRating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Rating not found"));

        rating.setAdminReply(request.getReply());
        rating.setRepliedAt(LocalDateTime.now());

        ratingRepository.save(rating);

        // gửi notification
        notificationService.sendToUser(
                rating.getUser().getId(),
                "💬 Admin đã phản hồi đánh giá của bạn",
                request.getReply(),
                "SYSTEM"
        );

        return toResponse(rating);
    }

    // ─────────────────────────────────────────────
    // AVERAGE RATINGS
    // ─────────────────────────────────────────────
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

        return avg != null
                ? Math.round(avg * 10.0) / 10.0
                : 0.0;
    }

    // ─────────────────────────────────────────────
    // CONVERT ENTITY -> RESPONSE
    // ─────────────────────────────────────────────
    public RatingResponse toResponse(ServiceRating r) {

        return RatingResponse.builder()
                .id(r.getId())

                .userId(
                        r.getUser() != null
                                ? r.getUser().getId()
                                : null
                )

                .userName(
                        r.getUser() != null
                                ? r.getUser().getFullName()
                                : "Ẩn danh"
                )

                .userEmail(
                        r.getUser() != null
                                ? r.getUser().getEmail()
                                : ""
                )

                .rating(r.getRating())
                .comment(r.getComment())
                .serviceType(r.getServiceType())
                .isPublic(r.getIsPublic())

                .adminReply(r.getAdminReply())
                .repliedAt(r.getRepliedAt())

                .createdAt(r.getCreatedAt())

                .build();
    }
}