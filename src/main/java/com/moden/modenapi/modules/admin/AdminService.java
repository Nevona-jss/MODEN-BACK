package com.moden.modenapi.modules.admin;

import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.common.utils.IdGenerator;
import com.moden.modenapi.modules.auth.model.AuthLocal;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.AuthLocalRepository;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
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

    @Value("${file.upload-dir:/home/hyona/IdeaProjects/MODEN/uploads}")
    private String uploadPath;

    // ----------------------------------------------------------------------
    // 🔹 CREATE STUDIO (ADMIN) – auto-creates/uses owner User by ownerPhone
    // ----------------------------------------------------------------------
    // src/main/java/com/moden/modenapi/modules/admin/AdminService.java
    public StudioRes createStudio(StudioCreateReq req,
                                  MultipartFile logoFile,
                                  MultipartFile bannerFile,
                                  MultipartFile profileFile) {

        // 1) Minimal validation
        if (req.fullName() == null || req.fullName().isBlank()
                || req.businessNo() == null || req.businessNo().isBlank()
                || req.ownerPhone() == null || req.ownerPhone().isBlank()
                || req.password() == null || req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields");
        }

        // 2) Owner user (by phone)
        String ownerName = req.fullName(); // create time: fallback to studio name
        User owner = userRepository.findByPhone(req.ownerPhone()).orElseGet(() -> {
            User u = new User();
            u.setFullName(ownerName);
            u.setPhone(req.ownerPhone());
            u.setRole(Role.HAIR_STUDIO);
            return userRepository.save(u);
        });

        // 3) Password (AuthLocal)
        AuthLocal authLocal = authLocalRepository.findByUserId(owner.getId()).orElse(null);
        if (authLocal == null) {
            authLocal = AuthLocal.builder()
                    .userId(owner.getId())
                    .passwordHash(passwordEncoder.encode(req.password()))
                    .passwordUpdatedAt(Instant.now())
                    .build();
        } else {
            authLocal.setPasswordHash(passwordEncoder.encode(req.password()));
            authLocal.setPasswordUpdatedAt(Instant.now());
        }
        authLocalRepository.save(authLocal);

        // 4) Auto-generate login code from fullName
        String studioCode = IdGenerator.generateId(req.fullName());

        // 5) Optional files now (or you can ignore and set later in update)
        String logoUrl = null, bannerUrl = null, profileUrl = null;
        try {
            if (logoFile != null && !logoFile.isEmpty())      logoUrl = fileStorageService.saveFile(logoFile);
            if (bannerFile != null && !bannerFile.isEmpty())  bannerUrl = fileStorageService.saveFile(bannerFile);
            if (profileFile != null && !profileFile.isEmpty())profileUrl = fileStorageService.saveFile(profileFile);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed: " + e.getMessage());
        }

        // 6) Create Studio with minimal fields, others = null (to be updated later)
        HairStudioDetail studio = HairStudioDetail.builder()
                .userId(owner.getId())
                .idForLogin(studioCode)
                .businessNo(req.businessNo())
                .ownerName(ownerName)    // may be updated later
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

        // 7) Response (StudioRes — first 4 are required)
        return new StudioRes(
                studio.getId(),
                owner.getId(),
                owner.getFullName(),
                owner.getPhone(),
                // optional (left null for now, will be filled by update)
                studio.getIdForLogin(),
                studio.getBusinessNo(),
                studio.getOwnerName(),
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
