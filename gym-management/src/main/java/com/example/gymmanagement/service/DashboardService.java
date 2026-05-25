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
        Long uid = user.getId();

        List<WorkoutSession> allSessions = sessionRepository.findByUserIdOrderBySessionDateDesc(uid);
        long totalSessions     = allSessions.size();
        long completedSessions = allSessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();

        Long totalCal = sessionRepository.sumCaloriesByUserId(uid);

        List<ProgressTracking> progList = progressRepository.findByUserIdOrderByDateAsc(uid);
        Double startWeight   = progList.isEmpty() ? null : progList.get(0).getWeight();
        Double currentWeight = progList.isEmpty() ? null : progList.get(progList.size() - 1).getWeight();
        Double currentBmi    = progList.isEmpty() ? null : progList.get(progList.size() - 1).getBmi();
        Double weightChange  = (startWeight != null && currentWeight != null)
                ? Math.round((currentWeight - startWeight) * 10.0) / 10.0 : null;

        int[] streaks = calculateStreaks(allSessions);

        Map<String, Integer> weeklyCalories = getWeeklyCalories(uid);
        Map<String, Integer> weeklyWorkouts = getWeeklyWorkouts(uid);

        return DashboardResponse.builder()
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .totalCaloriesBurned(totalCal)
                .currentWeight(currentWeight)
                .startingWeight(startWeight)
                .weightChange(weightChange)
                .currentBmi(currentBmi)
                .currentStreak(streaks[0])
                .longestStreak(streaks[1])
                .weeklyCalories(weeklyCalories)
                .weeklyWorkouts(weeklyWorkouts)
                .build();
    }

    public DashboardResponse getAdminDashboard() {
        long totalUsers  = userRepository.count();
        long activeUsers = userRepository.findAllActiveUsers().size();
        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart  = LocalDate.now().withDayOfYear(1).atStartOfDay();

        Double totalRevenue   = membershipRepository.sumRevenueBetween(LocalDate.of(2020,1,1).atStartOfDay(), now);
        Double monthlyRevenue = membershipRepository.sumRevenueBetween(monthStart, now);

        return DashboardResponse.builder()
                .totalUsers(totalUsers).activeUsers(activeUsers)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : 0.0)
                .totalWorkoutPlans(planRepository.count())
                .build();
    }

    private int[] calculateStreaks(List<WorkoutSession> allSessions) {
        // collect unique dates of COMPLETED sessions
        Set<LocalDate> doneSet = new HashSet<>();
        allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED && s.getSessionDate() != null)
                .forEach(s -> doneSet.add(s.getSessionDate()));

        if (doneSet.isEmpty()) return new int[]{0, 0};

        List<LocalDate> sorted = new ArrayList<>(doneSet);
        Collections.sort(sorted);

        int longestStreak = 1, streak = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).minusDays(1).equals(sorted.get(i - 1))) {
                streak++;
                longestStreak = Math.max(longestStreak, streak);
            } else {
                streak = 1;
            }
        }

        // current streak: count back from today
        int currentStreak = 0;
        LocalDate check = LocalDate.now();
        while (doneSet.contains(check) || doneSet.contains(check.minusDays(1))) {
            if (doneSet.contains(check)) {
                currentStreak++;
                check = check.minusDays(1);
            } else {
                break;
            }
        }

        return new int[]{currentStreak, longestStreak};
    }

    private Map<String, Integer> getWeeklyCalories(Long uid) {
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String d : days) result.put(d, 0);

        sessionRepository.findByUserIdAndSessionDateBetweenOrderBySessionDate(uid, monday, monday.plusDays(6))
                .stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED && s.getTotalCaloriesBurned() != null)
                .forEach(s -> {
                    int idx = s.getSessionDate().getDayOfWeek().getValue() - 1; // 0=Mon
                    result.put(days[idx], result.getOrDefault(days[idx], 0) + s.getTotalCaloriesBurned());
                });
        return result;
    }

    private Map<String, Integer> getWeeklyWorkouts(Long uid) {
        Map<String, Integer> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 3; i >= 0; i--) {
            LocalDate start = today.minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate end   = start.plusDays(6);
            String label    = "W" + start.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            long count = sessionRepository
                    .findByUserIdAndSessionDateBetweenOrderBySessionDate(uid, start, end)
                    .stream().filter(s -> s.getStatus() == SessionStatus.SCHEDULED
                            || s.getStatus() == SessionStatus.COMPLETED
                            || s.getStatus() == SessionStatus.CHECKED_IN)
                    .count();
            result.put(label, (int) count);
        }
        return result;
    }
}