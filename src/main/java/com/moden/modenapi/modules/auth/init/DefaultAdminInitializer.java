//package com.moden.modenapi.modules.auth.init;
//
//import com.moden.modenapi.common.enums.Role;
//import com.moden.modenapi.modules.auth.model.AuthLocal;
//import com.moden.modenapi.modules.auth.model.User;
//import com.moden.modenapi.modules.auth.repository.AuthLocalRepository;
//import com.moden.modenapi.modules.auth.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Instant;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DefaultAdminInitializer implements CommandLineRunner {
//
//    private final UserRepository userRepository;
//    private final AuthLocalRepository authLocalRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    // 🔐 환경변수 / yml 에서 주입
//    @Value("${app.admin.phone}")
//    private String adminPhone;
//
//    @Value("${app.admin.password}")
//    private String adminPassword;
//
//
//    @Override
//    @Transactional
//    public void run(String... args) {
//        // 이미 있으면 아무 것도 안 함 (한 번만 생성)
//        userRepository.findByPhone(adminPhone).ifPresentOrElse(
//                existing -> log.info("✅ Default admin already exists. phone={}", adminPhone),
//                () -> createDefaultAdmin()
//        );
//    }
//
//    private void createDefaultAdmin() {
//        log.info("⚙️ Creating default admin user. phone={}", adminPhone);
//
//        // 1) User 생성 (ROLE = ADMIN)
//        User admin = User.builder()
//                .fullName("admin")     // 필요하면 이름 바꿔도 됨
//                .phone(adminPhone)
//                .role(Role.ADMIN)
//                .phoneVerified(true)
//                .phoneVerifiedAt(Instant.now())
//                .build();
//
//        admin = userRepository.save(admin);
//
//        // 2) AuthLocal 생성 (비밀번호 BCrypt)
//        AuthLocal authLocal = AuthLocal.builder()
//                .userId(admin.getId())
//                .passwordHash(passwordEncoder.encode(adminPassword))
//                .passwordUpdatedAt(Instant.now())
//                .failedAttempts(0)
//                .forceReset(false)
//                .build();
//
//        authLocalRepository.save(authLocal);
//
//        log.info("🎉 Default admin created. userId={} phone={}", admin.getId(), adminPhone);
//    }
//}
