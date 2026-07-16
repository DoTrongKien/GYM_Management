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

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.success(planService.getAllTemplates(true)));
    }

    @PostMapping("/templates/{id}/select")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> selectTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {

        String email = ud.getUsername();
        return ResponseEntity.ok(ApiResponse.success(
                planService.selectTemplate(email, id), "Đã áp dụng giáo án mẫu"));
    }

    @PostMapping("/generate-with-goal")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> generateWithGoal(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody GeneratePlanWithGoalRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.generateAIPlanWithGoal(
                        ud.getUsername(),
                        req.getGoal(),
                        req.getFitnessLevel(),
                        req.getDaysPerWeek(),
                        req.getTargetDeltaKg(),
                        req.getEnduranceMetric(),
                        req.getEnduranceTargetValue()
                ),
                "Giáo án cá nhân hóa đã được tạo!"));
    }

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
                    "Giáo án đã cá nhân hóa đã được điều chỉnh thành công cho tuần tiếp theo!"));

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

    // ── SỬA: bỏ tham số goal — lịch tập giờ chỉ phụ thuộc số buổi/tuần (mục 8.2 I.docx) ──
    @GetMapping("/suggest-days")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggestDays(
            @RequestParam int sessions) {
        List<List<Integer>> options = planService.suggestDays(sessions);
        return ResponseEntity.ok(ApiResponse.success(Map.of("scheduleOptions", options)));
    }

    // ── MỚI: xác nhận lịch tập chuẩn khi hệ thống không còn tự xác định được
    // (mục 8.3 I.docx) — body: { "dayOfWeek": [1,2,4,5] } ──
    @PostMapping("/{id}/confirm-schedule")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> confirmSchedule(
            @AuthenticationPrincipal UserDetails ud,
            @PathVariable Long id,
            @RequestBody Map<String, List<Integer>> body) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.confirmSchedule(ud.getUsername(), id, body.get("dayOfWeek")),
                "Đã lưu lịch tập chuẩn"));
    }

    @GetMapping("/goals")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getGoals() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("value","MUSCLE_GAIN", "label","💪 Tăng cơ / Sức mạnh", "minDays","4"),
                Map.of("value","WEIGHT_LOSS", "label","🔥 Giảm cân / Đốt mỡ",  "minDays","4"),
                Map.of("value","ENDURANCE",   "label","🏃 Tăng sức bền",        "minDays","3"),
                Map.of("value","MAINTENANCE", "label","⚖️ Duy trì thể hình",    "minDays","3")
        )));
    }

    // ── Nhập tạ khởi điểm cho 1 bài tập trong giáo án (chỉ 1 lần) ──
    @PatchMapping("/plan-exercises/{id}/base-weight")
    public ResponseEntity<ApiResponse<WorkoutPlanExerciseResponse>> setBaseWeight(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body) {
        return ResponseEntity.ok(ApiResponse.success(
                planService.setBaseWeight(id, body.get("weight")), "Đã lưu tạ khởi điểm"));
    }
}