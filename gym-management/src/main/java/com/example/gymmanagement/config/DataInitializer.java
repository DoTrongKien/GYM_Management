package com.example.gymmanagement.config;

import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final BCryptPasswordEncoder passwordEncoder;

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
            log.info("Admin created: admin@gym.com / admin123");
        }
    }

    private void initExercises() {
        if (exerciseRepository.count() == 0) {
            // muscleGain, weightLoss, endurance, flexibility, maintenance (0-10)
            List<Exercise> exercises = List.of(

                    // ── CHEST ─────────────────────────────────────────────────
                    Exercise.builder().name("Push Up")
                            .description("Bài tập ngực cơ bản không cần dụng cụ")
                            .muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.EASY)
                            .caloriesBurned(8).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(6).weightLossScore(7).enduranceScore(7).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Bench Press")
                            .description("Đẩy tạ nằm - bài tập ngực chủ lực tăng cơ")
                            .muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(12).defaultSets(4).defaultReps(10).restSeconds(90)
                            .muscleGainScore(10).weightLossScore(5).enduranceScore(4).flexibilityScore(2).maintenanceScore(8)
                            .isActive(true).build(),

                    Exercise.builder().name("Incline Dumbbell Press")
                            .description("Đẩy tạ đôi nghiêng - phát triển ngực trên")
                            .muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(11).defaultSets(3).defaultReps(12).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(5).enduranceScore(4).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Cable Fly")
                            .description("Kéo cáp căng cơ ngực - isolation tốt")
                            .muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(8).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(4).enduranceScore(3).flexibilityScore(4).maintenanceScore(6)
                            .isActive(true).build(),

                    // ── BACK ──────────────────────────────────────────────────
                    Exercise.builder().name("Pull Up")
                            .description("Kéo xà đơn - phát triển lưng và tay")
                            .muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(10).defaultSets(3).defaultReps(8).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(7).enduranceScore(7).flexibilityScore(4).maintenanceScore(8)
                            .isActive(true).build(),

                    Exercise.builder().name("Deadlift")
                            .description("Cúi nhặt tạ - bài tập compound toàn thân tăng cơ tối đa")
                            .muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.HARD)
                            .caloriesBurned(15).defaultSets(3).defaultReps(6).restSeconds(120)
                            .muscleGainScore(10).weightLossScore(7).enduranceScore(5).flexibilityScore(3).maintenanceScore(8)
                            .isActive(true).build(),

                    Exercise.builder().name("Barbell Row")
                            .description("Kéo tạ đòn - phát triển lưng giữa")
                            .muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(12).defaultSets(4).defaultReps(10).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(6).enduranceScore(5).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Lat Pulldown")
                            .description("Kéo cáp trên - thay thế Pull Up cho người mới")
                            .muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.EASY)
                            .caloriesBurned(9).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(7).weightLossScore(5).enduranceScore(5).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    // ── SHOULDERS ─────────────────────────────────────────────
                    Exercise.builder().name("Overhead Press")
                            .description("Đẩy tạ đứng - phát triển vai toàn diện")
                            .muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(10).defaultSets(3).defaultReps(10).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(5).enduranceScore(5).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Lateral Raise")
                            .description("Nâng tạ ngang - phát triển vai ngang")
                            .muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.EASY)
                            .caloriesBurned(6).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(7).weightLossScore(4).enduranceScore(4).flexibilityScore(3).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Face Pull")
                            .description("Kéo cáp vào mặt - vai sau và tư thế")
                            .muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.EASY)
                            .caloriesBurned(5).defaultSets(3).defaultReps(15).restSeconds(45)
                            .muscleGainScore(6).weightLossScore(4).enduranceScore(5).flexibilityScore(5).maintenanceScore(7)
                            .isActive(true).build(),

                    // ── ARMS ──────────────────────────────────────────────────
                    Exercise.builder().name("Bicep Curl")
                            .description("Cuộn tạ đôi - cô lập cơ bắp tay trước")
                            .muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.EASY)
                            .caloriesBurned(6).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(4).enduranceScore(4).flexibilityScore(2).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Tricep Dip")
                            .description("Chống đẩy tay sau - phát triển cơ tam đầu")
                            .muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(9).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(5).enduranceScore(5).flexibilityScore(3).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Hammer Curl")
                            .description("Cuộn tạ đứng - cơ bắp tay và cẳng tay")
                            .muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.EASY)
                            .caloriesBurned(6).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(7).weightLossScore(4).enduranceScore(4).flexibilityScore(2).maintenanceScore(6)
                            .isActive(true).build(),

                    // ── LEGS ──────────────────────────────────────────────────
                    Exercise.builder().name("Squat")
                            .description("Ngồi xuống đứng lên - bài tập chân số 1 tăng cơ")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(12).defaultSets(4).defaultReps(12).restSeconds(90)
                            .muscleGainScore(10).weightLossScore(8).enduranceScore(6).flexibilityScore(4).maintenanceScore(9)
                            .isActive(true).build(),

                    Exercise.builder().name("Romanian Deadlift")
                            .description("Cúi nhặt tạ chân thẳng - cơ đùi sau và mông")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(11).defaultSets(3).defaultReps(12).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(6).enduranceScore(5).flexibilityScore(6).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Lunge")
                            .description("Bước chân đơn - cân bằng và phát triển chân")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.EASY)
                            .caloriesBurned(9).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(7).weightLossScore(7).enduranceScore(6).flexibilityScore(5).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Leg Press")
                            .description("Đẩy chân máy - an toàn cho người mới tập chân")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.EASY)
                            .caloriesBurned(10).defaultSets(4).defaultReps(15).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(6).enduranceScore(5).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    // ── CORE ──────────────────────────────────────────────────
                    Exercise.builder().name("Plank")
                            .description("Tấm ván - ổn định cơ lõi")
                            .muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.EASY)
                            .caloriesBurned(5).defaultSets(3).defaultDurationSeconds(60).restSeconds(30)
                            .muscleGainScore(5).weightLossScore(6).enduranceScore(7).flexibilityScore(4).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Crunch")
                            .description("Gập bụng - cơ bụng trên")
                            .muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.EASY)
                            .caloriesBurned(5).defaultSets(3).defaultReps(20).restSeconds(30)
                            .muscleGainScore(5).weightLossScore(6).enduranceScore(6).flexibilityScore(3).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Hanging Leg Raise")
                            .description("Nâng chân treo xà - cơ bụng dưới")
                            .muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.HARD)
                            .caloriesBurned(7).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(7).weightLossScore(6).enduranceScore(6).flexibilityScore(4).maintenanceScore(6)
                            .isActive(true).build(),

                    // ── CARDIO ────────────────────────────────────────────────
                    Exercise.builder().name("Treadmill Run")
                            .description("Chạy bộ máy - cardio đốt mỡ hiệu quả")
                            .muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(10).defaultSets(1).defaultDurationSeconds(1800).restSeconds(0)
                            .muscleGainScore(2).weightLossScore(10).enduranceScore(10).flexibilityScore(2).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Jump Rope")
                            .description("Nhảy dây - cardio toàn thân đốt mỡ cao")
                            .muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.EASY)
                            .caloriesBurned(12).defaultSets(3).defaultDurationSeconds(300).restSeconds(60)
                            .muscleGainScore(2).weightLossScore(9).enduranceScore(9).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Cycling")
                            .description("Đạp xe máy - cardio nhẹ nhàng khớp gối")
                            .muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.EASY)
                            .caloriesBurned(8).defaultSets(1).defaultDurationSeconds(1800).restSeconds(0)
                            .muscleGainScore(2).weightLossScore(8).enduranceScore(9).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    // ── FULL BODY ─────────────────────────────────────────────
                    Exercise.builder().name("Burpee")
                            .description("Bài tập toàn thân đốt calo cao nhất")
                            .muscleGroup(MuscleGroup.FULL_BODY).difficulty(Difficulty.HARD)
                            .caloriesBurned(15).defaultSets(3).defaultReps(10).restSeconds(90)
                            .muscleGainScore(5).weightLossScore(10).enduranceScore(9).flexibilityScore(4).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Kettlebell Swing")
                            .description("Vung tạ ấm - toàn thân, sức mạnh và cardio")
                            .muscleGroup(MuscleGroup.FULL_BODY).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(13).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(6).weightLossScore(9).enduranceScore(8).flexibilityScore(4).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Mountain Climber")
                            .description("Leo núi tại chỗ - core và cardio")
                            .muscleGroup(MuscleGroup.FULL_BODY).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(11).defaultSets(3).defaultDurationSeconds(45).restSeconds(30)
                            .muscleGainScore(4).weightLossScore(9).enduranceScore(8).flexibilityScore(4).maintenanceScore(6)
                            .isActive(true).build(),

                    // ── FLEXIBILITY ───────────────────────────────────────────
                    Exercise.builder().name("Yoga Cat-Cow")
                            .description("Tư thế mèo-bò - linh hoạt cột sống")
                            .muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.EASY)
                            .caloriesBurned(2).defaultSets(2).defaultDurationSeconds(60).restSeconds(0)
                            .muscleGainScore(1).weightLossScore(2).enduranceScore(2).flexibilityScore(10).maintenanceScore(5)
                            .isActive(true).build(),

                    Exercise.builder().name("Hip Flexor Stretch")
                            .description("Giãn cơ hông - linh hoạt và phòng chấn thương")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.EASY)
                            .caloriesBurned(2).defaultSets(2).defaultDurationSeconds(60).restSeconds(0)
                            .muscleGainScore(1).weightLossScore(2).enduranceScore(2).flexibilityScore(10).maintenanceScore(5)
                            .isActive(true).build(),

                    Exercise.builder().name("Shoulder Stretch")
                            .description("Giãn cơ vai - phòng chấn thương vai")
                            .muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.EASY)
                            .caloriesBurned(1).defaultSets(2).defaultDurationSeconds(45).restSeconds(0)
                            .muscleGainScore(1).weightLossScore(1).enduranceScore(2).flexibilityScore(9).maintenanceScore(5)
                            .isActive(true).build()
            );

            exerciseRepository.saveAll(exercises);
            log.info("Seeded {} exercises with goal scores", exercises.size());
        }
    }
}