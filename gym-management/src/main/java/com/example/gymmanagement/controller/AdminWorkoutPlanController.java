//package com.example.gymmanagement.controller;
//
//import com.example.gymmanagement.dto.request.WorkoutPlanRequest;
//import com.example.gymmanagement.dto.request.WorkoutTemplateRequest;
//import com.example.gymmanagement.dto.response.ApiResponse;
//import com.example.gymmanagement.dto.response.WorkoutPlanResponse;
//import com.example.gymmanagement.service.AdminWorkoutPlanService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/admin/workout-plans")
//@RequiredArgsConstructor
//public class AdminWorkoutPlanController {
//    private final AdminWorkoutPlanService adminWorkoutPlanService;
//    @GetMapping
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getALLPlans(){
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        adminWorkoutPlanService.getAllPlans()
//                )
//        );
//    }
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getPlanById(
//            @PathVariable Long id) {
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        adminWorkoutPlanService.getPlanById(id)
//                )
//        );
//    }
//
//
//    @PostMapping
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createPlan(
//            @RequestBody WorkoutTemplateRequest request) {
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        adminWorkoutPlanService.createTemplate(request),
//                        "Template created successfully"
//                )
//        );
//    }
//    @PutMapping("/{id}")
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> updatePlan(
//            @PathVariable Long id,
//            @RequestBody WorkoutTemplateRequest request) {
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        adminWorkoutPlanService.updateTemplate(id, request),
//                        "Template updated successfully"
//                )
//        );
//    }
//
////    @PostMapping
////    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
////    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createPlan(
////            @RequestBody WorkoutPlanRequest request) {
////
////        return ResponseEntity.ok(
////                ApiResponse.success(
////                        adminWorkoutPlanService.createPlan(request),
////                        "Plan created successfully"
////                )
////        );
////    }
////    @PutMapping("/{id}")
////    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
////    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> updatePlan(
////            @PathVariable Long id,
////            @RequestBody WorkoutPlanRequest request) {
////
////        return ResponseEntity.ok(
////                ApiResponse.success(
////                        adminWorkoutPlanService.updatePlan(id, request),
////                        "Plan updated successfully"
////                )
////        );
////    }
//// sửa lại để tên method bên adminworkoutplancontroller đồng bộ với adminworkoutplanservice mới
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    public ResponseEntity<ApiResponse<Void>> deletePlan(
//            @PathVariable Long id) {
//
//        adminWorkoutPlanService.deletePlan(id);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        null,
//                        "Plan deleted successfully"
//                )
//        );
//    }
//}
package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.request.WorkoutTemplateRequest;
import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.dto.response.WorkoutPlanResponse;
import com.example.gymmanagement.service.AdminWorkoutPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/workout-plans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminWorkoutPlanController {

    private final AdminWorkoutPlanService adminWorkoutPlanService;

    // ============================================================
    // QUAN TRỌNG: route cụ thể "/templates" PHẢI khai báo TRƯỚC
    // route có biến "/{id}", nếu không Spring sẽ match "templates"
    // vào @PathVariable Long id và ném lỗi convert String -> Long.
    // ============================================================

    // ─── Templates (giáo án mẫu do admin tạo tay) ───────────────
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success(adminWorkoutPlanService.getAllTemplates()));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> createTemplate(
            @RequestBody WorkoutTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminWorkoutPlanService.createTemplate(request), "Tạo giáo án mẫu thành công"));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> updateTemplate(
            @PathVariable Long id,
            @RequestBody WorkoutTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminWorkoutPlanService.updateTemplate(id, request), "Cập nhật giáo án mẫu thành công"));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        adminWorkoutPlanService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa giáo án mẫu"));
    }

    // ─── Plan tổng quát (list toàn bộ, gồm cả plan của user) ────
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkoutPlanResponse>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.success(adminWorkoutPlanService.getAllPlans()));
    }

    // ─── Các route có "/{id}" trần phải đứng SAU "/templates" ───
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkoutPlanResponse>> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminWorkoutPlanService.getPlanById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        adminWorkoutPlanService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Plan deleted successfully"));
    }
}