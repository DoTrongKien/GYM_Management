package com.example.gymmanagement.dto.response;
import lombok.*;
import java.util.Map;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardResponse {
    // User stats
    private Long totalSessions;
    private Long completedSessions;
    private Long totalCaloriesBurned;
    private Double currentWeight;
    private Double startingWeight;
    private Double weightChange;
    private Double currentBmi;
    private Integer currentStreak;
    private Integer longestStreak;
    // Weekly progress
    private Map<String, Integer> weeklyCalories;
    private Map<String, Integer> weeklyWorkouts;
    // Admin stats
    private Long totalUsers;
    private Long activeUsers;
    private Double totalRevenue;
    private Double monthlyRevenue;
    private Long totalWorkoutPlans;
    private Double averageRating;
}