package com.moden.modenapi.modules.customer.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.coupon.service.CouponService;
import com.moden.modenapi.modules.customer.dto.CustomerProfileUpdateReq;
import com.moden.modenapi.modules.customer.dto.CustomerSignUpRequest;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.auth.model.UserSession;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.auth.repository.UserSessionRepository;
import com.moden.modenapi.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService extends BaseService<CustomerDetail> {

    private final UserRepository userRepository;
    private final CustomerDetailRepository customerDetailRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtProvider jwtProvider;
    private final HttpServletRequest request;
    private final CouponService couponService;

    // 🔹 CREATE USER + SESSION
    @Transactional
    public User createUser(CustomerSignUpRequest req,
                           String deviceId,
                           String deviceType,
                           String ipAddress,
                           String userAgent,
                           String appVersion) {

        userRepository.findByPhone(req.phone()).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone number already registered");
        });

        User user = User.builder()
                .fullName(req.fullName())
                .phone(req.phone())
                .build();
        userRepository.save(user);

        UserSession session = UserSession.builder()
                .userId(user.getId())
                .deviceId(deviceId)
                .deviceType(deviceType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .appVersion(appVersion)
                .isPrimarySession(true)
                .revoked(false)
                .lastActivityAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60L * 60 * 24 * 30))
                .build();
        userSessionRepository.save(session);

        // Issue first-visit coupon
        couponService.issueFirstVisitCoupon(req.studioId(), user.getId());
        return user;
    }

    @Transactional(readOnly = true)
    public User getUser(UUID id) {
        return userRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or deleted"));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllActive();
    }

    @Transactional
    public CustomerDetail updateUserDetail(UUID userId, CustomerProfileUpdateReq req) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or deleted"));

        CustomerDetail detail = customerDetailRepository.findByUserId(userId)
                .orElse(CustomerDetail.builder().userId(userId).build());

        if (req.email() != null) detail.setEmail(req.email());
        if (req.birthdate() != null) detail.setBirthdate(req.birthdate());
        if (req.gender() != null) detail.setGender(req.gender());
        if (req.address() != null) detail.setAddress(req.address());
        if (req.consentMarketing() != null) detail.setConsentMarketing(req.consentMarketing());
        if (req.profileImageUrl() != null) detail.setProfileImageUrl(req.profileImageUrl());
        if (req.visitReason() != null) detail.setVisitReason(req.visitReason());

        return customerDetailRepository.save(detail);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or already deleted"));

        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        var sessions = userSessionRepository.findByUserId(userId);
        sessions.forEach(s -> {
            s.setRevoked(true);
            s.setLastActivityAt(Instant.now());
        });
        userSessionRepository.saveAll(sessions);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        return getAuthenticatedUser();
    }

    private User getAuthenticatedUser() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        UUID userId = UUID.fromString(jwtProvider.getUserId(token));
        return userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or deleted"));
    }

    @Override
    protected JpaRepository<CustomerDetail, UUID> getRepository() {
        return customerDetailRepository;
    }
}
