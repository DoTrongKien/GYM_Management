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
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkoutReminderScheduler {

    private final WorkoutSessionRepository sessionRepository;
    private final MembershipRepository membershipRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // Run every day at 7:00 AM — remind users of today's scheduled sessions
    @Scheduled(cron = "0 0 7 * * *")
    public void sendDailyWorkoutReminders() {
        log.info("Running daily workout reminder job...");
        List<WorkoutSession> todaySessions = sessionRepository.findScheduledSessionsForDate(LocalDate.now());
        for (WorkoutSession session : todaySessions) {
            try {
                String planName = session.getWorkoutPlan() != null ? session.getWorkoutPlan().getPlanName() : "your workout";
                String dayName  = session.getPlanDay()    != null ? session.getPlanDay().getDayName()    : "";

                emailService.sendWorkoutReminder(
                        session.getUser().getEmail(),
                        session.getUser().getFullName(),
                        session.getSessionDate().toString(),
                        planName + " – " + dayName
                );

                notificationService.sendToUser(
                        session.getUser().getId(),
                        "💪 Workout Reminder",
                        "You have a " + planName + " session scheduled today (" + dayName + "). Let's go!",
                        "WORKOUT_REMINDER"
                );
            } catch (Exception e) {
                log.error("Failed to send reminder for session {}: {}", session.getId(), e.getMessage());
            }
        }
        log.info("Sent {} workout reminders.", todaySessions.size());
    }

    // Run every day at 9:00 AM — warn users whose membership expires in 3 days
    @Scheduled(cron = "0 0 9 * * *")
    public void sendMembershipExpiryReminders() {
        log.info("Running membership expiry reminder job...");
        LocalDate expiryDate = LocalDate.now().plusDays(3);
        List<Membership> expiring = membershipRepository.findExpiringOnDate(expiryDate);
        for (Membership m : expiring) {
            try {
                emailService.sendMembershipExpiryReminder(
                        m.getUser().getEmail(),
                        m.getUser().getFullName(),
                        m.getEndDate().toString()
                );
                notificationService.sendToUser(
                        m.getUser().getId(),
                        "⚠️ Membership Expiring Soon",
                        "Your " + m.getMembershipType().name() + " membership expires on " + m.getEndDate() + ". Renew now to keep your progress going!",
                        "SYSTEM"
                );
            } catch (Exception e) {
                log.error("Failed to send expiry reminder for membership {}: {}", m.getId(), e.getMessage());
            }
        }
        log.info("Sent {} membership expiry reminders.", expiring.size());
    }
}