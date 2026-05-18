package com.example.gymmanagement.service;

import com.example.gymmanagement.dto.AuthResponse;
import com.example.gymmanagement.dto.LoginRequest;
import com.example.gymmanagement.dto.RegisterRequest;
import com.example.gymmanagement.entity.User;
import com.example.gymmanagement.repository.UserRepository;
import com.example.gymmanagement.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.gymmanagement.entity.Role;
import com.example.gymmanagement.repository.RoleRepository;
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RoleRepository roleRepository;

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {

        Role role = roleRepository
                .findByRoleName(request.getRole())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        User user = new User();

        user.setFullName(request.getFullName());

        user.setEmail(request.getEmail());

        user.setPhone(request.getPhone());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(role);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                role.getRoleName()
        );
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email not found"));

        boolean checkPassword = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!checkPassword) {
            throw new RuntimeException("Wrong password");
        }

        String token = jwtService.generateToken(user.getEmail());

        String roleName = user.getRole().getRoleName();

        return new AuthResponse(token, roleName);
    }
}