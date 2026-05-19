package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.SessionExerciseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SessionExerciseLogRepository extends JpaRepository<SessionExerciseLog, Long> {
    List<SessionExerciseLog> findBySessionId(Long sessionId);
}