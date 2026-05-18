package com.example.gymmanagement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // bật @PreAuthorize cho từng method nếu cần sau này
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // Public: ai cũng gọi được
                        .requestMatchers("/api/auth/**").permitAll()

                        // ROLE_USER: chỉ tự cập nhật/xem profile của mình (không cần userId trên URL)
                        .requestMatchers(HttpMethod.POST, "/api/profile").hasAuthority("ROLE_USER")
                        .requestMatchers(HttpMethod.GET,  "/api/profile").hasAuthority("ROLE_USER")

                        // ROLE_ADMIN: xem profile của bất kỳ user nào theo id
                        .requestMatchers("/api/profile/**").hasAuthority("ROLE_ADMIN")

                        // Mọi endpoint còn lại yêu cầu đăng nhập
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}