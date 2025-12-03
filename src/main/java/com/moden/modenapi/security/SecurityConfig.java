package com.moden.modenapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger 전체 허용 (springdoc 설정이 /docs 사용)
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/docs/**",
                                "/error"
                        ).permitAll()

                        // 정적 파일(이미지 업로드 등) 공개
                        .requestMatchers("/uploads/**").permitAll()

                        // 인증/로그인 공개
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/signin",
                                "/api/auth/id-login",   // ★ 스튜디오/디자이너 idForLogin 로그인
                                "/api/auth/refresh"
                        ).permitAll()

                        // 역할별 보호
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/designers/**").hasRole("DESIGNER")
                        .requestMatchers("/api/studios/**").hasAnyRole("DESIGNER","HAIR_STUDIO")
                        .requestMatchers("/api/customers/**").hasRole("CUSTOMER")
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        // 그 외
                        .anyRequest().permitAll()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var source = new UrlBasedCorsConfigurationSource();

        // ⚠️ 쿠키(리프레시 토큰)를 쓸 경우 allowCredentials(true) + Origin을 * 대신 구체값으로!
        var cfg = new CorsConfiguration();
        // 예시: 필요한 도메인만 명시
        cfg.setAllowedOriginPatterns(List.of(
                "http://192.168.1.24:5173",
                "http://localhost:5173",
                "http://122.37.246.79:7006",
                "http://122.37.246.79:7007"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of(HttpHeaders.SET_COOKIE, HttpHeaders.AUTHORIZATION));
        cfg.setAllowCredentials(true); // ← 쿠키 사용 시 true
        cfg.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}