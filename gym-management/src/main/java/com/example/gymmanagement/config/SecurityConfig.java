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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                // --- THÊM DÒNG NÀY ĐỂ H2 CONSOLE CHẠY ĐƯỢC TRONG IFRAME ---
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SePay gọi server-to-server, không có JWT của user -> phải mở public
                        .requestMatchers("/api/bank/webhook/**").permitAll()
                        .requestMatchers("/api/invoices/**").authenticated()
                        // --- THÊM DÒNG NÀY ĐỂ CHO PHÉP TRUY CẬP H2 ---
                        .requestMatchers("/h2-console/**").permitAll()

                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/exercises").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ratings/public").permitAll()
                        // File đính kèm phục vụ tĩnh (tên UUID không đoán được)
                        .requestMatchers(HttpMethod.GET, "/api/files/**").permitAll()

                        // Admin only
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                        // User routes
                        .requestMatchers("/api/profile/**").authenticated()
                        .requestMatchers("/api/workout-plans/**").authenticated()
                        .requestMatchers("/api/sessions/**").authenticated()
                        .requestMatchers("/api/progress/**").authenticated()
                        .requestMatchers("/api/memberships/**").authenticated()
                        .requestMatchers("/api/nutrition/**").authenticated()
                        .requestMatchers("/api/ratings/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/ratings/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/exercises/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}