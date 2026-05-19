package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.response.DashboardResponse;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.SessionStatus;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final ProgressTrackingRepository progressRepository;
    private final MembershipRepository membershipRepository;
    private final ServiceRatingRepository ratingRepository;
    private final WorkoutPlanRepository planRepository;

    public DashboardResponse getUserDashboard(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();

        Long totalSessions = (long) sessionRepository.findByUserIdOrderBySessionDateDesc(userId).size();
        Long completedSessions = sessionRepository.countCompletedByUserId(userId);
        Long totalCalories = sessionRepository.sumCaloriesByUserId(userId);

        // Weight progress
        List<ProgressTracking> progressList = progressRepository.findByUserIdOrderByDateAsc(userId);
        Double startWeight = progressList.isEmpty() ? null : progressList.get(0).getWeight();
        Double currentWeight = progressList.isEmpty() ? null : progressList.get(progressList.size() - 1).getWeight();
        Double currentBmi = progressList.isEmpty() ? null : progressList.get(progressList.size() - 1).getBmi();
        Double weightChange = (startWeight != null && currentWeight != null) ? currentWeight - startWeight : null;

        // Streak calculation
        int[] streaks = calculateStreaks(userId);

        // Weekly data
        Map<String, Integer> weeklyCalories = getWeeklyCalories(userId);
        Map<String, Integer> weeklyWorkouts = getWeeklyWorkouts(userId);

        return DashboardResponse.builder()
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .totalCaloriesBurned(totalCalories)
                .currentWeight(currentWeight)
                .startingWeight(startWeight)
                .weightChange(weightChange != null ? Math.round(weightChange * 10.0) / 10.0 : null)
                .currentBmi(currentBmi)
                .currentStreak(streaks[0])
                .longestStreak(streaks[1])
                .weeklyCalories(weeklyCalories)
                .weeklyWorkouts(weeklyWorkouts)
                .build();
    }

    public DashboardResponse getAdminDashboard() {
        long totalUsers = userRepository.findAllActiveUsers().size();
        long allUsers = userRepository.count();

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Double totalRevenue = membershipRepository.sumRevenueBetween(
                LocalDate.of(2020, 1, 1).atStartOfDay(), LocalDateTime.now());
        Double monthlyRevenue = membershipRepository.sumRevenueBetween(monthStart, LocalDateTime.now());

        Long totalPlans = planRepository.count();
        Double avgRating = ratingRepository.getAverageRatingByType("WORKOUT_PLAN");

        return DashboardResponse.builder()
                .totalUsers(allUsers)
                .activeUsers(totalUsers)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : 0.0)
                .totalWorkoutPlans(totalPlans)
                .averageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0)
                .build();
    }

    private int[] calculateStreaks(Long userId) {
        List<WorkoutSession> completed = sessionRepository.findByUserIdAndStatus(userId, SessionStatus.COMPLETED);
        if (completed.isEmpty()) return new int[]{0, 0};

        Set<LocalDate> dates = new HashSet<>();
        completed.forEach(s -> dates.add(s.getSessionDate()));

        List<LocalDate> sortedDates = new ArrayList<>(dates);
        Collections.sort(sortedDates);

        int currentStreak = 0;
        int longestStreak = 0;
        int streak = 1;

        for (int i = 1; i < sortedDates.size(); i++) {
            if (sortedDates.get(i).minusDays(1).equals(sortedDates.get(i - 1))) {
                streak++;
            } else {
                longestStreak = Math.max(longestStreak, streak);
                streak = 1;
            }
        }
        longestStreak = Math.max(longestStreak, streak);

        // Check if streak is current
        LocalDate lastDate = sortedDates.get(sortedDates.size() - 1);
        if (lastDate.equals(LocalDate.now()) || lastDate.equals(LocalDate.now().minusDays(1))) {
            currentStreak = streak;
        }

        return new int[]{currentStreak, longestStreak};
    }

    private Map<String, Integer> getWeeklyCalories(Long userId) {
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        Map<String, Integer> result = new LinkedHashMap<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            result.put(days[i], 0);
        }

        List<WorkoutSession> sessions = sessionRepository
                .findByUserIdAndSessionDateBetweenOrderBySessionDate(userId, monday, monday.plusDays(6));
        sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED && s.getTotalCaloriesBurned() != null)
                .forEach(s -> {
                    int dayIndex = s.getSessionDate().getDayOfWeek().getValue() - 1;
                    result.put(days[dayIndex], s.getTotalCaloriesBurned());
                });
        return result;
    }

    private Map<String, Integer> getWeeklyWorkouts(Long userId) {
        Map<String, Integer> result = new LinkedHashMap<>();
        String[] weeks = {"Week 1", "Week 2", "Week 3", "Week 4"};
        for (String week : weeks) result.put(week, 0);

        LocalDate now = LocalDate.now();
        for (int i = 0; i < 4; i++) {
            LocalDate start = now.minusWeeks(3 - i).with(java.time.DayOfWeek.MONDAY);
            LocalDate end = start.plusDays(6);
            long count = sessionRepository.findByUserIdAndSessionDateBetweenOrderBySessionDate(userId, start, end)
                    .stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
            result.put(weeks[i], (int) count);
        }
        return result;
    }
}