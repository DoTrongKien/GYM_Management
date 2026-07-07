// ============================================================
// GHI ĐÈ FILE: src/main/java/com/example/gymmanagement/pet/PetService.java
// ============================================================
package com.example.gymmanagement.pet;

import com.example.gymmanagement.entity.User;
import com.example.gymmanagement.entity.UserProfile;
import com.example.gymmanagement.entity.WorkoutSession;
import com.example.gymmanagement.enums.SessionStatus;
import com.example.gymmanagement.repository.UserRepository;
import com.example.gymmanagement.repository.UserProfileRepository;
import com.example.gymmanagement.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetProfileRepository petRepo;
    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final WorkoutSessionRepository sessionRepo;

    private static final int MAX_TIER_COUNT = 30; // vượt mốc 30 giữ nguyên bậc cao nhất, không tăng thêm

    @Transactional
    public PetResponse recalculate(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        PetProfile pet = petRepo.findByUserId(user.getId())
                .orElseGet(() -> PetProfile.builder().userId(user.getId()).user(user).build());

        // ── 1. Thể hình: CHỈ dựa vào BMI, không liên quan gì tới tập luyện ──
        pet.setStage(calcStageFromBmi(user.getId()));

        // ── 2. Streak tập liên tục & streak bỏ tập liên tục ──
        int[] streaks = calcStreaks(user.getId()); // [completedStreak, missedStreak]
        int completedStreak = Math.min(streaks[0], MAX_TIER_COUNT);
        int missedStreak     = Math.min(streaks[1], MAX_TIER_COUNT);

        pet.setCurrentStreak(completedStreak);
        pet.setMissedStreak(missedStreak);
        pet.setAuraTier(mapStreakToAura(completedStreak));
        pet.setWebCount(mapMissedToWebCount(missedStreak));

        pet.setLastCalculatedAt(java.time.LocalDateTime.now());
        petRepo.save(pet);

        return toResponse(pet);
    }

    // Cron 00:05 hằng ngày - bắt các buổi SCHEDULED của hôm qua mà user không check-in
    // (đúng định nghĩa "bỏ tập" đã chốt: tính cả buổi trôi qua ngày, không chỉ bấm nút Bỏ)
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void dailyPenaltyJob() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<WorkoutSession> missed = sessionRepo.findScheduledSessionsForDate(yesterday);
        for (WorkoutSession s : missed) {
            recalculate(s.getUser().getEmail());
        }
    }

    public PetResponse getPet(String email) {
        User user = userRepo.findByEmail(email).orElseThrow();
        return petRepo.findByUserId(user.getId())
                .map(this::toResponse)
                .orElseGet(() -> recalculate(email));
    }

    // ── Thể hình từ BMI ─────────────────────────────────────
    private PetStage calcStageFromBmi(Long userId) {
        return profileRepo.findByUserId(userId).map(p -> {
            if (p.getWeight() == null || p.getHeight() == null) return PetStage.AVERAGE;
            double h = p.getHeight() / 100.0;
            double bmi = p.getWeight() / (h * h);
            if (bmi < 17)   return PetStage.SLIM;
            if (bmi < 18.5) return PetStage.LEAN;
            if (bmi < 25)   return PetStage.FIT;
            if (bmi < 30)   return PetStage.AVERAGE;
            return PetStage.OVERWEIGHT;
        }).orElse(PetStage.AVERAGE);
    }

    // ── Streak tập / streak bỏ tập ──────────────────────────
    // Quét từ session mới nhất trở về trước:
    //  - Bỏ qua session tương lai (chưa tới ngày, chưa xảy ra)
    //  - Gặp COMPLETED thì cộng vào completedStreak (nếu đang đếm missed thì dừng)
    //  - Gặp SKIPPED hoặc SCHEDULED đã quá ngày thì cộng vào missedStreak (nếu đang đếm completed thì dừng)
    private int[] calcStreaks(Long userId) {
        List<WorkoutSession> recent = sessionRepo.findByUserIdOrderBySessionDateDesc(userId);
        int completed = 0, missed = 0;
        LocalDate today = LocalDate.now();

        for (WorkoutSession s : recent) {
            if (s.getSessionDate() == null || s.getSessionDate().isAfter(today)) continue;

            boolean isCompleted = s.getStatus() == SessionStatus.COMPLETED;
            boolean isMissed = s.getStatus() == SessionStatus.SKIPPED
                    || (s.getStatus() == SessionStatus.SCHEDULED && s.getSessionDate().isBefore(today));

            if (isCompleted) {
                if (missed > 0) break;
                completed++;
            } else if (isMissed) {
                if (completed > 0) break;
                missed++;
            }
            // CHECKED_IN dở dang hoặc SCHEDULED đúng hôm nay -> bỏ qua, chưa tính
        }
        return new int[]{completed, missed};
    }

    private AuraTier mapStreakToAura(int streak) {
        if (streak >= 30) return AuraTier.BLACK;
        if (streak >= 25) return AuraTier.GREEN;
        if (streak >= 20) return AuraTier.YELLOW;
        if (streak >= 15) return AuraTier.PURPLE;
        if (streak >= 10) return AuraTier.RED;
        if (streak >= 5)  return AuraTier.BLUE;
        return AuraTier.NONE;
    }

    private int mapMissedToWebCount(int missed) {
        return Math.min(missed / 5, 6);
    }

    private PetResponse toResponse(PetProfile p) {
        return PetResponse.builder()
                .stage(p.getStage())
                .currentStreak(p.getCurrentStreak())
                .missedStreak(p.getMissedStreak())
                .auraTier(p.getAuraTier())
                .webCount(p.getWebCount())
                .build();
    }
}