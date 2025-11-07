package com.moden.modenapi.modules.admin;

import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.common.utils.IdGenerator;
import com.moden.modenapi.modules.auth.dto.AuthResponse;
import com.moden.modenapi.modules.auth.model.AuthLocal;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.AuthLocalRepository;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import com.moden.modenapi.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final HairStudioDetailRepository studioRepository;
    private final AuthLocalRepository authLocalRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final JwtProvider jwtProvider;
    private final HairStudioDetailRepository hairStudioDetailRepository;

    @Value("${file.upload-dir:/home/hyona/IdeaProjects/MODEN/uploads}")
    private String uploadPath;


    // 🔹 CREATE STUDIO (ADMIN) – auto-creates/uses owner User by ownerPhone

    @Transactional
    public StudioRes createStudio(StudioCreateReq req,
                                  MultipartFile logoFile,
                                  MultipartFile bannerFile,
                                  MultipartFile profileFile) {

        // 1) minimal validation
        if (isBlank(req.fullName()) || isBlank(req.businessNo())
                || isBlank(req.ownerPhone()) || isBlank(req.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields");
        }
        String rawPwd = req.password().trim();
        if (rawPwd.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }

        // 2) USER: studio account (fullName = studio name, phone = owner phone)
        User owner = userRepository.findByPhone(req.ownerPhone()).orElse(null);
        if (owner == null) {
            owner = User.builder()
                    .fullName(req.fullName())      // ← studio nomi
                    .phone(req.ownerPhone())       // ← owner phone
                    .role(Role.HAIR_STUDIO)
                    .build();
        } else {
            owner.setFullName(req.fullName());     // ← mavjud bo'lsa ham studioning nomiga tekislaymiz
            owner.setRole(Role.HAIR_STUDIO);       // ← studio account
        }
        owner = userRepository.save(owner);

        // 3) AuthLocal (BCrypt)
        AuthLocal authLocal = authLocalRepository.findByUserId(owner.getId()).orElse(null);
        if (authLocal == null) {
            authLocal = AuthLocal.builder()
                    .userId(owner.getId())
                    .passwordHash(passwordEncoder.encode(rawPwd))
                    .passwordUpdatedAt(Instant.now())
                    .build();
        } else if (!passwordEncoder.matches(rawPwd, authLocal.getPasswordHash())) {
            authLocal.setPasswordHash(passwordEncoder.encode(rawPwd));
            authLocal.setPasswordUpdatedAt(Instant.now());
        }
        authLocalRepository.save(authLocal);

        // 4) unique idForLogin (studioCode)
        String studioCode;
        int guard = 0;
        do {
            studioCode = IdGenerator.generateId(req.fullName());
            if (++guard > 10) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot generate unique studio code");
            }
        } while (studioRepository.existsByIdForLogin(studioCode));

        // 5) optional files
        String logoUrl = null, bannerUrl = null, profileUrl = null;
        try {
            if (logoFile != null && !logoFile.isEmpty())     logoUrl = fileStorageService.saveFile(logoFile);
            if (bannerFile != null && !bannerFile.isEmpty()) bannerUrl = fileStorageService.saveFile(bannerFile);
            if (profileFile != null && !profileFile.isEmpty()) profileUrl = fileStorageService.saveFile(profileFile);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed: " + e.getMessage());
        }

        // 6) HairStudioDetail: ownerName hozircha null (keyin update’da kiritasiz)
        HairStudioDetail studio = HairStudioDetail.builder()
                .userId(owner.getId())
                .idForLogin(studioCode)
                .businessNo(req.businessNo())
                .ownerName(null)               // ← keyin update’da kiritasiz
                .studioPhone(null)
                .address(null)
                .description(null)
                .parkingInfo(null)
                .instagramUrl(null)
                .naverUrl(null)
                .kakaoUrl(null)
                .logoImageUrl(logoUrl)
                .bannerImageUrl(bannerUrl)
                .profileImageUrl(profileUrl)
                .latitude(null)
                .longitude(null)
                .position(Position.STUDIO_OWNER)
                .build();


        studio = studioRepository.save(studio);

        // 7) response
        return new StudioRes(
                studio.getId(),
                owner.getId(),
                owner.getFullName(),     // = studio name
                owner.getPhone(),        // = owner phone
                studio.getIdForLogin(),
                studio.getBusinessNo(),
                studio.getOwnerName(),   // null (later update)
                studio.getStudioPhone(),
                studio.getAddress(),
                studio.getDescription(),
                studio.getProfileImageUrl(),
                studio.getLogoImageUrl(),
                studio.getBannerImageUrl(),
                studio.getInstagramUrl(),
                studio.getNaverUrl(),
                studio.getKakaoUrl(),
                studio.getParkingInfo(),
                studio.getLatitude(),
                studio.getLongitude()
        );
    }


    private static boolean isBlank(String s) { return s == null || s.isBlank(); }



    @Transactional
    public StudioRes adminUpdateStudio(UUID studioId, StudioUpdateReq req) {
        HairStudioDetail s = hairStudioDetailRepository.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));
        if (s.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio deleted");
        }

        // ❗ADMIN: may update read-only (if required by policy)
        if (req.businessNo() != null && !req.businessNo().isBlank()) {
            s.setBusinessNo(req.businessNo().trim());
        }
        if (req.fullName() != null && !req.fullName().isBlank()) {
            userRepository.findById(s.getUserId()).ifPresent(u -> {
                u.setFullName(req.fullName().trim());
                userRepository.save(u);
            });
        }

        // ✅ Editable for admin
        if (req.studioPhone() != null)     s.setStudioPhone(req.studioPhone());
        if (req.address() != null)         s.setAddress(req.address());
        if (req.description() != null)     s.setDescription(req.description());
        if (req.parkingInfo() != null)     s.setParkingInfo(req.parkingInfo());

        if (req.logoImageUrl() != null)    s.setLogoImageUrl(req.logoImageUrl());
        if (req.bannerImageUrl() != null)  s.setBannerImageUrl(req.bannerImageUrl());
        if (req.profileImageUrl() != null) s.setProfileImageUrl(req.profileImageUrl());

        if (req.instagram() != null)       s.setInstagramUrl(req.instagram());
        if (req.naver() != null)           s.setNaverUrl(req.naver());
        if (req.kakao() != null)           s.setKakaoUrl(req.kakao());

        if (req.latitude() != null)        s.setLatitude(req.latitude());
        if (req.longitude() != null)       s.setLongitude(req.longitude());

        s.setUpdatedAt(Instant.now());
        hairStudioDetailRepository.saveAndFlush(s);

        var owner = userRepository.findById(s.getUserId()).orElse(null);
        return new StudioRes(
                s.getId(), s.getUserId(),
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getPhone()    : null,
                s.getIdForLogin(), s.getBusinessNo(), s.getOwnerName(), s.getStudioPhone(),
                s.getAddress(), s.getDescription(), s.getProfileImageUrl(), s.getLogoImageUrl(),
                s.getBannerImageUrl(), s.getInstagramUrl(), s.getNaverUrl(), s.getKakaoUrl(),
                s.getParkingInfo(), s.getLatitude(), s.getLongitude()
        );
    }


    // ----------------------------------------------------------------------
    // 🔹 READ / DELETE (soft)
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public StudioRes getStudio(UUID studioId) {
        HairStudioDetail s = studioRepository.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));
        if (s.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio has been deleted");
        }
        return mapToRes(s);
    }

    @Transactional(readOnly = true)
    public java.util.List<StudioRes> getAllStudios() {
        return studioRepository.findAll().stream()
                .filter(s -> s.getDeletedAt() == null)
                .map(this::mapToRes)
                .toList();
    }

    public void deleteStudio(UUID studioId) {
        HairStudioDetail s = studioRepository.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));
        if (s.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio already deleted");
        }
        s.setDeletedAt(Instant.now());
        studioRepository.save(s);
    }

    // ✅ single, correct mapper
    private StudioRes mapToRes(HairStudioDetail s) {
        var owner = userRepository.findById(s.getUserId()).orElse(null);
        String ownerFullName = owner != null ? owner.getFullName() : null;
        String ownerPhone    = owner != null ? owner.getPhone()    : null;

        return new StudioRes(
                // required
                s.getId(),
                s.getUserId(),
                ownerFullName,
                ownerPhone,
                // optional
                s.getIdForLogin(),
                s.getBusinessNo(),
                s.getOwnerName(),
                s.getStudioPhone(),
                s.getAddress(),
                s.getDescription(),
                s.getProfileImageUrl(),
                s.getLogoImageUrl(),
                s.getBannerImageUrl(),
                s.getInstagramUrl(),
                s.getNaverUrl(),
                s.getKakaoUrl(),
                s.getParkingInfo(),
                s.getLatitude(),
                s.getLongitude()
        );
    }
}
