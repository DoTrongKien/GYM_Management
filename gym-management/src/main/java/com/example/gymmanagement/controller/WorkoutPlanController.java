package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.*;
import com.example.gymmanagement.dto.response.*;
import com.example.gymmanagement.enums.Goal;
import com.example.gymmanagement.enums.FitnessLevel;
import com.example.gymmanagement.service.WorkoutPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workout-plans")
@RequiredArgsConstructor
public class WorkoutPlanController {

    private final WorkoutPlanService planService;

    // Tạo giáo án AI theo hồ sơ + mục tiêu + số ngày
    @PostMapping("/generate-with-goal")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> generateWithGoal(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody GeneratePlanWithGoalRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.generateAIPlanWithGoal(
                        ud.getUsername(), req.getGoal(),
                        req.getFitnessLevel(), req.getDaysPerWeek()),
                "Giáo án AI đã được tạo!"));
    }
    // Điều chỉnh giáo án sau khi hoàn thành 1 tuần
    @PostMapping("/{id}/adjust-week")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> adjustWeek(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        try {
            Double weight = null;
            Double bodyFat = null;

            if (body.get("newWeight") != null) {
                weight = Double.parseDouble(body.get("newWeight").toString());
            }
            if (body.get("newBodyFat") != null) {
                bodyFat = Double.parseDouble(body.get("newBodyFat").toString());
            }

            WorkoutPlanResponse updatedPlan = planService.adjustPlanAfterWeek(
                    id, ud.getUsername(), weight, bodyFat);

            return ResponseEntity.ok(ApiResponse.success(
                    updatedPlan,
                    "Giáo án đã được AI điều chỉnh thành công cho tuần tiếp theo!"));

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Dữ liệu cân nặng hoặc body fat không hợp lệ"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Không thể điều chỉnh giáo án: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getActive(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(planService.getActivePlan(ud.getUsername())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getAll(
            @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(planService.getAllPlans(ud.getUsername())));
    }

    // Gợi ý ngày tập theo mục tiêu
    @GetMapping("/suggest-days")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestDays(
            @RequestParam String goal,
            @RequestParam int sessions) {
        Goal g = Goal.valueOf(goal);
        List<String> days = planService.suggestDays(g, sessions);
        return ResponseEntity.ok(ApiResponse.success(Map.of("suggestedDays", days)));
    }

    // Goals list cho FE
    @GetMapping("/goals")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getGoals() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("value","MUSCLE_GAIN", "label","💪 Tăng cơ / Sức mạnh", "minDays","4"),
                Map.of("value","WEIGHT_LOSS", "label","🔥 Giảm cân / Đốt mỡ",  "minDays","4"),
                Map.of("value","ENDURANCE",   "label","🏃 Tăng sức bền",        "minDays","3"),
                Map.of("value","FLEXIBILITY", "label","🤸 Tăng linh hoạt",      "minDays","2"),
                Map.of("value","MAINTENANCE", "label","⚖️ Duy trì thể hình",    "minDays","2")
        )));
    }
}