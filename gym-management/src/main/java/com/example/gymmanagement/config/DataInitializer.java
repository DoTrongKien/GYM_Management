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
                    .fullName("Admin").email("admin@gym.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("0900000000").status(true).emailVerified(true)
                    .role(adminRole).build();
            userRepository.save(admin);
            log.info("Admin created: admin@gym.com / admin123");
        }
    }

    private void initExercises() {
        if (exerciseRepository.count() == 0) {
            // muscleGain, weightLoss, endurance, flexibility, maintenance (0-10)
            List<Exercise> exercises = List.of(

                    // ── CHEST ─────────────────────────────────────────────────
                    Exercise.builder().name("Push Up").description("Hít đất cơ bản, vừa tăng cơ vừa tốt cho sức bền")
                            .muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.EASY)
                            .caloriesBurned(8).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(6).weightLossScore(5).enduranceScore(7).flexibilityScore(2).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Bench Press").description("Đẩy tạ nằm - bài tập tăng cơ ngực hàng đầu")
                            .muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(12).defaultSets(4).defaultReps(10).restSeconds(90)
                            .muscleGainScore(10).weightLossScore(4).enduranceScore(4).flexibilityScore(1).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Incline Dumbbell Press").description("Đẩy tạ đôi trên ghế nghiêng - phần trên ngực")
                            .muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(10).defaultSets(3).defaultReps(12).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(4).enduranceScore(3).flexibilityScore(2).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Cable Fly").description("Kéo cáp chéo - co cơ ngực sâu")
                            .muscleGroup(MuscleGroup.CHEST).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(7).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(3).enduranceScore(3).flexibilityScore(4).maintenanceScore(6)
                            .isActive(true).build(),

                    // ── BACK ──────────────────────────────────────────────────
                    Exercise.builder().name("Pull Up").description("Xà đơn - lưng và bắp tay")
                            .muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(10).defaultSets(3).defaultReps(8).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(5).enduranceScore(7).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Deadlift").description("Kéo tạ đất - bài compound toàn thân")
                            .muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.HARD)
                            .caloriesBurned(15).defaultSets(3).defaultReps(6).restSeconds(120)
                            .muscleGainScore(10).weightLossScore(6).enduranceScore(5).flexibilityScore(2).maintenanceScore(8)
                            .isActive(true).build(),

                    Exercise.builder().name("Seated Cable Row").description("Kéo cáp ngồi - lưng giữa và dày")
                            .muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.EASY)
                            .caloriesBurned(8).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(4).enduranceScore(5).flexibilityScore(2).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Lat Pulldown").description("Kéo xà xuống - lưng to và rộng")
                            .muscleGroup(MuscleGroup.BACK).difficulty(Difficulty.EASY)
                            .caloriesBurned(8).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(4).enduranceScore(5).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    // ── SHOULDERS ─────────────────────────────────────────────
                    Exercise.builder().name("Overhead Press").description("Đẩy tạ trên đầu - vai trước và giữa")
                            .muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(10).defaultSets(3).defaultReps(10).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(4).enduranceScore(4).flexibilityScore(2).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Lateral Raise").description("Nâng tạ ngang - vai giữa")
                            .muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.EASY)
                            .caloriesBurned(6).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(7).weightLossScore(3).enduranceScore(5).flexibilityScore(3).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Face Pull").description("Kéo dây về mặt - vai sau và cuff xoay")
                            .muscleGroup(MuscleGroup.SHOULDERS).difficulty(Difficulty.EASY)
                            .caloriesBurned(5).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(6).weightLossScore(3).enduranceScore(4).flexibilityScore(6).maintenanceScore(7)
                            .isActive(true).build(),

                    // ── ARMS ──────────────────────────────────────────────────
                    Exercise.builder().name("Barbell Curl").description("Cuộn tạ thẳng - bắp tay trước")
                            .muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.EASY)
                            .caloriesBurned(6).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(3).enduranceScore(4).flexibilityScore(2).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Tricep Pushdown").description("Đẩy cáp xuống - bắp tay sau")
                            .muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.EASY)
                            .caloriesBurned(6).defaultSets(3).defaultReps(15).restSeconds(60)
                            .muscleGainScore(8).weightLossScore(3).enduranceScore(4).flexibilityScore(2).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Hammer Curl").description("Cuộn búa - bắp tay và cẳng tay")
                            .muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.EASY)
                            .caloriesBurned(6).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(7).weightLossScore(3).enduranceScore(4).flexibilityScore(2).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Skull Crusher").description("Tạ đầu - bắp tay sau chuyên sâu")
                            .muscleGroup(MuscleGroup.ARMS).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(7).defaultSets(3).defaultReps(10).restSeconds(75)
                            .muscleGainScore(9).weightLossScore(3).enduranceScore(3).flexibilityScore(1).maintenanceScore(6)
                            .isActive(true).build(),

                    // ── LEGS ──────────────────────────────────────────────────
                    Exercise.builder().name("Squat").description("Ngồi xuống đứng lên - bài tập cơ bản chân")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(12).defaultSets(4).defaultReps(12).restSeconds(90)
                            .muscleGainScore(10).weightLossScore(7).enduranceScore(6).flexibilityScore(4).maintenanceScore(8)
                            .isActive(true).build(),

                    Exercise.builder().name("Romanian Deadlift").description("Kéo tạ Romania - đùi sau và mông")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(11).defaultSets(3).defaultReps(10).restSeconds(90)
                            .muscleGainScore(9).weightLossScore(6).enduranceScore(5).flexibilityScore(5).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Lunge").description("Bước lunge - đùi và mông đều tác động")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.EASY)
                            .caloriesBurned(9).defaultSets(3).defaultReps(12).restSeconds(60)
                            .muscleGainScore(7).weightLossScore(7).enduranceScore(6).flexibilityScore(5).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Leg Press").description("Đẩy chân máy - đùi trước an toàn")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.EASY)
                            .caloriesBurned(10).defaultSets(4).defaultReps(15).restSeconds(75)
                            .muscleGainScore(8).weightLossScore(5).enduranceScore(5).flexibilityScore(2).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Calf Raise").description("Nâng gót - bắp chân")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.EASY)
                            .caloriesBurned(5).defaultSets(4).defaultReps(20).restSeconds(45)
                            .muscleGainScore(6).weightLossScore(3).enduranceScore(6).flexibilityScore(3).maintenanceScore(5)
                            .isActive(true).build(),

                    // ── CORE ──────────────────────────────────────────────────
                    Exercise.builder().name("Plank").description("Chống đẩy tĩnh - ổn định cơ lõi")
                            .muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.EASY)
                            .caloriesBurned(5).defaultSets(3).defaultDurationSeconds(60).restSeconds(30)
                            .muscleGainScore(5).weightLossScore(5).enduranceScore(8).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Crunch").description("Gập bụng cơ bản")
                            .muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.EASY)
                            .caloriesBurned(5).defaultSets(3).defaultReps(20).restSeconds(30)
                            .muscleGainScore(5).weightLossScore(5).enduranceScore(6).flexibilityScore(3).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Russian Twist").description("Xoay người với tạ - cơ chéo bụng")
                            .muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(7).defaultSets(3).defaultReps(20).restSeconds(45)
                            .muscleGainScore(5).weightLossScore(7).enduranceScore(6).flexibilityScore(5).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Leg Raise").description("Nâng chân nằm - cơ bụng dưới")
                            .muscleGroup(MuscleGroup.CORE).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(6).defaultSets(3).defaultReps(15).restSeconds(45)
                            .muscleGainScore(5).weightLossScore(6).enduranceScore(6).flexibilityScore(4).maintenanceScore(6)
                            .isActive(true).build(),

                    // ── CARDIO ────────────────────────────────────────────────
                    Exercise.builder().name("Treadmill Run").description("Chạy bộ máy - cardio hiệu quả nhất")
                            .muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(12).defaultSets(1).defaultDurationSeconds(1800).restSeconds(0)
                            .muscleGainScore(1).weightLossScore(10).enduranceScore(10).flexibilityScore(1).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Jump Rope").description("Nhảy dây - đốt calories nhanh")
                            .muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.EASY)
                            .caloriesBurned(13).defaultSets(3).defaultDurationSeconds(300).restSeconds(60)
                            .muscleGainScore(1).weightLossScore(9).enduranceScore(9).flexibilityScore(3).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Cycling").description("Đạp xe tại chỗ - cardio khớp gối an toàn")
                            .muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.EASY)
                            .caloriesBurned(10).defaultSets(1).defaultDurationSeconds(1800).restSeconds(0)
                            .muscleGainScore(2).weightLossScore(8).enduranceScore(9).flexibilityScore(2).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Rowing Machine").description("Máy chèo thuyền - toàn thân và cardio")
                            .muscleGroup(MuscleGroup.CARDIO).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(11).defaultSets(1).defaultDurationSeconds(1200).restSeconds(0)
                            .muscleGainScore(4).weightLossScore(9).enduranceScore(9).flexibilityScore(3).maintenanceScore(7)
                            .isActive(true).build(),

                    // ── FULL BODY ─────────────────────────────────────────────
                    Exercise.builder().name("Burpee").description("Bài tập toàn thân - sức mạnh và cardio kết hợp")
                            .muscleGroup(MuscleGroup.FULL_BODY).difficulty(Difficulty.HARD)
                            .caloriesBurned(15).defaultSets(3).defaultReps(10).restSeconds(90)
                            .muscleGainScore(5).weightLossScore(10).enduranceScore(9).flexibilityScore(4).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Kettlebell Swing").description("Xoay tạ ấm - hông, lưng dưới, cardio")
                            .muscleGroup(MuscleGroup.FULL_BODY).difficulty(Difficulty.MEDIUM)
                            .caloriesBurned(13).defaultSets(4).defaultReps(15).restSeconds(60)
                            .muscleGainScore(6).weightLossScore(9).enduranceScore(8).flexibilityScore(4).maintenanceScore(7)
                            .isActive(true).build(),

                    Exercise.builder().name("Clean and Press").description("Kéo và đẩy tạ - sức mạnh bùng nổ")
                            .muscleGroup(MuscleGroup.FULL_BODY).difficulty(Difficulty.HARD)
                            .caloriesBurned(14).defaultSets(3).defaultReps(8).restSeconds(120)
                            .muscleGainScore(9).weightLossScore(7).enduranceScore(7).flexibilityScore(3).maintenanceScore(8)
                            .isActive(true).build(),

                    // ── FLEXIBILITY ───────────────────────────────────────────
                    Exercise.builder().name("Yoga Sun Salutation").description("Chào mặt trời - kéo giãn và thở")
                            .muscleGroup(MuscleGroup.FULL_BODY).difficulty(Difficulty.EASY)
                            .caloriesBurned(4).defaultSets(3).defaultDurationSeconds(300).restSeconds(30)
                            .muscleGainScore(1).weightLossScore(3).enduranceScore(4).flexibilityScore(10).maintenanceScore(6)
                            .isActive(true).build(),

                    Exercise.builder().name("Hip Flexor Stretch").description("Kéo giãn hông - giảm đau lưng")
                            .muscleGroup(MuscleGroup.LEGS).difficulty(Difficulty.EASY)
                            .caloriesBurned(2).defaultSets(2).defaultDurationSeconds(60).restSeconds(30)
                            .muscleGainScore(1).weightLossScore(1).enduranceScore(2).flexibilityScore(10).maintenanceScore(5)
                            .isActive(true).build()
            );

            exerciseRepository.saveAll(exercises);
            log.info("Exercises initialized: {} bài tập với chỉ số benefit", exercises.size());
        }
    }
}