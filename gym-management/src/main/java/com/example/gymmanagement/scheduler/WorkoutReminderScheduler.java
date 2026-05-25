package com.example.gymmanagement.scheduler;

import com.example.gymmanagement.entity.Membership;
import com.example.gymmanagement.entity.WorkoutSession;
import com.example.gymmanagement.repository.MembershipRepository;
import com.example.gymmanagement.repository.WorkoutSessionRepository;
import com.example.gymmanagement.service.EmailService;
import com.example.gymmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkoutReminderScheduler {

    private final WorkoutSessionRepository sessionRepository;
    private final MembershipRepository membershipRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // 7:00 AM — remind today's scheduled sessions (no scheduled time)
    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyWorkoutReminders() {
        List<WorkoutSession> sessions = sessionRepository.findScheduledSessionsForDate(LocalDate.now());
        for (WorkoutSession s : sessions) {
            if (s.getScheduledTime() != null) continue; // handled by 30-min reminder
            try {
                String plan = s.getWorkoutPlan() != null ? s.getWorkoutPlan().getPlanName()
                        : s.getCustomSessionName() != null ? s.getCustomSessionName() : "Buổi tập";
                emailService.sendWorkoutReminder(s.getUser().getEmail(), s.getUser().getFullName(),
                        s.getSessionDate().toString(), plan);
                notificationService.sendToUser(s.getUser().getId(),
                        "💪 Nhắc nhở tập luyện hôm nay",
                        "Bạn có buổi tập " + plan + " vào hôm nay. Hãy cố gắng lên!",
                        "WORKOUT_REMINDER");
            } catch (Exception e) {
                log.error("Reminder error session {}: {}", s.getId(), e.getMessage());
            }
        }
        log.info("Sent {} daily reminders", sessions.size());
    }

    // Every 15 min — remind sessions starting in ~30 min
    @Scheduled(fixedRate = 900_000)
    public void sendUpcomingSessionReminders() {
        LocalDate today = LocalDate.now();
        LocalTime from  = LocalTime.now().plusMinutes(25);
        LocalTime to    = LocalTime.now().plusMinutes(35);
        List<WorkoutSession> sessions = sessionRepository.findAllUpcomingSessions(today, from, to);
        for (WorkoutSession s : sessions) {
            try {
                String plan = s.getCustomSessionName() != null ? s.getCustomSessionName()
                        : s.getWorkoutPlan() != null ? s.getWorkoutPlan().getPlanName() : "Buổi tập";
                notificationService.sendToUser(s.getUser().getId(),
                        "⏰ Còn 30 phút nữa đến giờ tập!",
                        "Buổi tập \"" + plan + "\" bắt đầu lúc " + s.getScheduledTime() + ". Chuẩn bị thôi!",
                        "WORKOUT_REMINDER");
            } catch (Exception e) {
                log.error("Upcoming reminder error: {}", e.getMessage());
            }
        }
    }

    // 9:00 AM — membership expiry
    @Scheduled(cron = "0 0 9 * * *")
    public void sendMembershipExpiryReminders() {
        LocalDate expiryDate = LocalDate.now().plusDays(3);
        List<Membership> expiring = membershipRepository.findExpiringOnDate(expiryDate);
        for (Membership m : expiring) {
            try {
                emailService.sendMembershipExpiryReminder(m.getUser().getEmail(),
                        m.getUser().getFullName(), m.getEndDate().toString());
                notificationService.sendToUser(m.getUser().getId(),
                        "⚠️ Gói tập sắp hết hạn",
                        "Gói " + m.getMembershipType() + " của bạn hết hạn vào " + m.getEndDate() + ". Gia hạn ngay!",
                        "SYSTEM");
            } catch (Exception e) {
                log.error("Expiry reminder error: {}", e.getMessage());
            }
        }
    }
}