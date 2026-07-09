package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.response.NutritionResponse;
import com.example.gymmanagement.entity.*;
import com.example.gymmanagement.enums.*;
import com.example.gymmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NutritionService {

    private final NutritionPlanRepository nutritionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final MembershipRepository membershipRepository;

    public NutritionResponse generateNutritionPlan(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Please complete your profile first."));

        boolean isVip = membershipRepository.findByUserIdAndIsActiveTrue(user.getId())
                .map(m -> m.getMembershipType() == MembershipType.VIP)
                .orElse(false);

        Goal goal = profile.getGoal() != null ? profile.getGoal() : Goal.MAINTENANCE;
        double weight = profile.getWeight() != null ? profile.getWeight() : 70.0;
        double height = profile.getHeight() != null ? profile.getHeight() : 170.0;
        int age = profile.getAge() != null ? profile.getAge() : 25;
        String gender = profile.getGender() != null ? profile.getGender() : "male";

        // Mifflin-St Jeor BMR
        double bmr;
        if ("female".equalsIgnoreCase(gender)) {
            bmr = 10 * weight + 6.25 * height - 5 * age - 161;
        } else {
            bmr = 10 * weight + 6.25 * height - 5 * age + 5;
        }

        // Activity multiplier (moderate activity)
        double tdee = bmr * 1.55;

        int calories;
        int protein, carbs, fat;
        String planName;
        String mealSuggestions;

        switch (goal) {
            case WEIGHT_LOSS:
                calories = (int) (tdee - 500); // 500 cal deficit
                protein = (int) (weight * 2.2); // high protein
                fat = (int) (calories * 0.25 / 9);
                carbs = (int) ((calories - protein * 4 - fat * 9) / 4);
                planName = "Fat Loss Nutrition Plan";
                mealSuggestions = buildMealSuggestions("WEIGHT_LOSS", isVip);
                break;
            case MUSCLE_GAIN:
                calories = (int) (tdee + 300); // slight surplus
                protein = (int) (weight * 2.5);
                fat = (int) (calories * 0.25 / 9);
                carbs = (int) ((calories - protein * 4 - fat * 9) / 4);
                planName = "Muscle Building Nutrition Plan";
                mealSuggestions = buildMealSuggestions("MUSCLE_GAIN", isVip);
                break;
            default:
                calories = (int) tdee;
                protein = (int) (weight * 1.8);
                fat = (int) (calories * 0.3 / 9);
                carbs = (int) ((calories - protein * 4 - fat * 9) / 4);
                planName = "Maintenance Nutrition Plan";
                mealSuggestions = buildMealSuggestions("MAINTENANCE", isVip);
        }

        NutritionPlan plan = NutritionPlan.builder()
                .user(user)
                .planName(planName)
                .dailyCalories(calories)
                .proteinGrams(Math.max(protein, 0))
                .carbsGrams(Math.max(carbs, 0))
                .fatGrams(Math.max(fat, 0))
                .goal(goal)
                .mealSuggestions(mealSuggestions)
                .isAiGenerated(true)
                .build();
        nutritionRepository.save(plan);

        return buildResponse(plan);
    }

    public NutritionResponse getLatestPlan(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return nutritionRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .map(this::buildResponse)
                .orElseThrow(() -> new RuntimeException("No nutrition plan found. Generate one first."));
    }

    public List<NutritionResponse> getAllPlans(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return nutritionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::buildResponse).collect(Collectors.toList());
    }

    private String buildMealSuggestions(String goalType, boolean isVip) {
        if (!isVip) {
            // Gói Free: gợi ý cơ bản, 1 phương án cố định cho mỗi mục tiêu
            return switch (goalType) {
                case "WEIGHT_LOSS" -> "{\"breakfast\":\"Oatmeal with berries + 2 boiled eggs\",\"lunch\":\"Grilled chicken breast + salad + brown rice\",\"dinner\":\"Steamed fish + vegetables + quinoa\",\"snacks\":\"Greek yogurt, almonds, fruits\"}";
                case "MUSCLE_GAIN" -> "{\"breakfast\":\"Eggs + whole grain toast + banana + protein shake\",\"lunch\":\"Beef + rice + broccoli\",\"dinner\":\"Chicken + sweet potato + greens\",\"snacks\":\"Protein bar, peanut butter, cottage cheese\"}";
                default -> "{\"breakfast\":\"Whole grain cereal + milk + fruits\",\"lunch\":\"Mixed protein + complex carbs + vegetables\",\"dinner\":\"Light protein + salad\",\"snacks\":\"Fruits, nuts, yogurt\"}";
            };
        }

        // Gói VIP: cá nhân hóa chi tiết hơn - có thêm phương án thay thế mỗi bữa + lưu ý riêng
        return switch (goalType) {
            case "WEIGHT_LOSS" -> "{\"breakfast\":\"Oatmeal with berries + 2 boiled eggs\",\"breakfastAlt\":\"Greek yogurt + chia seeds + granola\",\"lunch\":\"Grilled chicken breast + salad + brown rice\",\"lunchAlt\":\"Grilled salmon + quinoa + steamed broccoli\",\"dinner\":\"Steamed fish + vegetables + quinoa\",\"dinnerAlt\":\"Turkey breast + zucchini noodles\",\"snacks\":\"Greek yogurt, almonds, fruits\",\"note\":\"Uống đủ 2.5-3L nước/ngày, ưu tiên protein nạc và rau xanh để giữ cơ bắp trong lúc giảm cân.\"}";
            case "MUSCLE_GAIN" -> "{\"breakfast\":\"Eggs + whole grain toast + banana + protein shake\",\"breakfastAlt\":\"Oatmeal + whey protein + peanut butter\",\"lunch\":\"Beef + rice + broccoli\",\"lunchAlt\":\"Salmon + sweet potato + asparagus\",\"dinner\":\"Chicken + sweet potato + greens\",\"dinnerAlt\":\"Lean beef + quinoa + mixed vegetables\",\"snacks\":\"Protein bar, peanut butter, cottage cheese\",\"note\":\"Ăn thêm 1 bữa phụ giàu protein trước khi ngủ (casein/cottage cheese) để hỗ trợ phục hồi cơ qua đêm.\"}";
            default -> "{\"breakfast\":\"Whole grain cereal + milk + fruits\",\"breakfastAlt\":\"Avocado toast + poached egg\",\"lunch\":\"Mixed protein + complex carbs + vegetables\",\"lunchAlt\":\"Grilled tofu/chicken bowl + brown rice\",\"dinner\":\"Light protein + salad\",\"dinnerAlt\":\"Soup + steamed vegetables + lean protein\",\"snacks\":\"Fruits, nuts, yogurt\",\"note\":\"Duy trì bữa ăn đa dạng nhóm chất, ưu tiên thực phẩm nguyên chất để giữ mức năng lượng ổn định cả ngày.\"}";
        };
    }

    private NutritionResponse buildResponse(NutritionPlan p) {
        return NutritionResponse.builder()
                .id(p.getId()).planName(p.getPlanName())
                .dailyCalories(p.getDailyCalories()).proteinGrams(p.getProteinGrams())
                .carbsGrams(p.getCarbsGrams()).fatGrams(p.getFatGrams())
                .goal(p.getGoal()).mealSuggestions(p.getMealSuggestions())
                .isAiGenerated(p.getIsAiGenerated()).createdAt(p.getCreatedAt())
                .build();
    }
}