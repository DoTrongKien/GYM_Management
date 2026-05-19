package com.example.gymmanagement.config;

import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MembershipRepository membershipRepository;

    @Override
    public void run(String... args) {
        initRoles();
        initAdminUser();
        initExercises();
        log.info("Data initialization complete.");
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.saveAll(List.of(
                    new Role(null, "ROLE_ADMIN"),
                    new Role(null, "ROLE_USER")
            ));
            log.info("Roles initialized.");
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByEmail("admin@gym.com")) {
            Role adminRole = roleRepository.findByRoleName("ROLE_ADMIN").orElseThrow();
            User admin = User.builder()
                    .fullName("Admin")
                    .email("admin@gym.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("0900000000")
                    .status(true)
                    .emailVerified(true)
                    .role(adminRole)
                    .build();
            userRepository.save(admin);
            log.info("Admin user created: admin@gym.com / admin123");
        }
    }

    private void initExercises() {
        if (exerciseRepository.count() == 0) {
            List<Exercise> exercises = List.of(
                    Exercise.builder().name("Push Up").description("Classic chest exercise").muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.EASY).caloriesBurned(8).defaultSets(3).defaultReps(15).restSeconds(60).isActive(true).build(),
                    Exercise.builder().name("Bench Press").description("Barbell bench press for chest strength").muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.MEDIUM).caloriesBurned(12).defaultSets(4).defaultReps(10).restSeconds(90).isActive(true).build(),
                    Exercise.builder().name("Pull Up").description("Upper back and bicep exercise").muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.MEDIUM).caloriesBurned(10).defaultSets(3).defaultReps(8).restSeconds(90).isActive(true).build(),
                    Exercise.builder().name("Deadlift").description("Full body compound movement").muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.HARD).caloriesBurned(15).defaultSets(3).defaultReps(6).restSeconds(120).isActive(true).build(),
                    Exercise.builder().name("Squat").description("Primary lower body exercise").muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.MEDIUM).caloriesBurned(12).defaultSets(4).defaultReps(12).restSeconds(90).isActive(true).build(),
                    Exercise.builder().name("Lunge").description("Unilateral leg exercise").muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.EASY).caloriesBurned(9).defaultSets(3).defaultReps(12).restSeconds(60).isActive(true).build(),
                    Exercise.builder().name("Shoulder Press").description("Overhead pressing movement").muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.MEDIUM).caloriesBurned(10).defaultSets(3).defaultReps(12).restSeconds(90).isActive(true).build(),
                    Exercise.builder().name("Lateral Raise").description("Shoulder isolation exercise").muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.EASY).caloriesBurned(6).defaultSets(3).defaultReps(15).restSeconds(60).isActive(true).build(),
                    Exercise.builder().name("Bicep Curl").description("Bicep isolation movement").muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.EASY).caloriesBurned(6).defaultSets(3).defaultReps(15).restSeconds(60).isActive(true).build(),
                    Exercise.builder().name("Tricep Dip").description("Tricep compound exercise").muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.MEDIUM).caloriesBurned(8).defaultSets(3).defaultReps(12).restSeconds(60).isActive(true).build(),
                    Exercise.builder().name("Plank").description("Core stability exercise").muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.EASY).caloriesBurned(5).defaultSets(3).defaultDurationSeconds(60).restSeconds(30).isActive(true).build(),
                    Exercise.builder().name("Crunch").description("Abdominal exercise").muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.EASY).caloriesBurned(5).defaultSets(3).defaultReps(20).restSeconds(30).isActive(true).build(),
                    Exercise.builder().name("Treadmill Run").description("Cardio running on treadmill").muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.MEDIUM).caloriesBurned(10).defaultSets(1).defaultDurationSeconds(1800).restSeconds(0).isActive(true).build(),
                    Exercise.builder().name("Jump Rope").description("Cardio with jump rope").muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.EASY).caloriesBurned(12).defaultSets(3).defaultDurationSeconds(300).restSeconds(60).isActive(true).build(),
                    Exercise.builder().name("Burpee").description("Full body cardio exercise").muscleGroup(MuscleGroup.FULL_BODY).difficulty(Difficulty.HARD).caloriesBurned(15).defaultSets(3).defaultReps(10).restSeconds(90).isActive(true).build()
            );
            exerciseRepository.saveAll(exercises);
            log.info("Exercises initialized: {} exercises", exercises.size());
        }
    }
}