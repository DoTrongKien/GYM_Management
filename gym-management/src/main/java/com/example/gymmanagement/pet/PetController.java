package com.example.gymmanagement.pet;
// ============================================================
// FILE MỚI: src/main/java/com/example/gymmanagement/controller/PetController.java
// ============================================================


import com.example.gymmanagement.dto.response.ApiResponse;
import com.example.gymmanagement.pet.PetResponse;
import com.example.gymmanagement.pet.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @GetMapping
    public ResponseEntity<ApiResponse<PetResponse>> getPet(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success(petService.getPet(ud.getUsername())));
    }
}
