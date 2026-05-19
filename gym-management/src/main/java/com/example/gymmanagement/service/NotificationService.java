package com.example.gymmanagement.service;

import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<Notification> getMyNotifications(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public long getUnreadCount(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    public void markAllRead(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(user.getId());
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    public void sendToUser(Long userId, String title, String message, String type) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Notification notification = Notification.builder()
                .user(user).title(title).message(message).type(type)
                .sentAt(LocalDateTime.now()).build();
        notificationRepository.save(notification);
    }

    public void sendBroadcast(String title, String message, String type) {
        List<User> users = userRepository.findAllActiveUsers();
        List<Notification> notifications = users.stream().map(u ->
                Notification.builder().user(u).title(title).message(message)
                        .type(type).sentAt(LocalDateTime.now()).build()
        ).collect(Collectors.toList());
        notificationRepository.saveAll(notifications);
    }
}