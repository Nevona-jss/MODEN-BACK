package com.moden.modenapi.modules.admin;

import com.moden.modenapi.common.enums.Position;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.common.utils.IdGenerator;
import com.moden.modenapi.modules.studio.dto.StudioCreateReq;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import com.moden.modenapi.modules.auth.model.AuthLocal;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.AuthLocalRepository;
import com.moden.modenapi.modules.auth.repository.UserRepository; // ✅ FIXED import
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final HairStudioDetailRepository studioRepository;
    private final AuthLocalRepository authLocalRepository;
    private final UserRepository userRepository; // ✅ Correct reference
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Value("${file.upload-dir:/home/hyona/IdeaProjects/MODEN/uploads}")
    private String uploadPath;


    // ----------------------------------------------------------------------
    // 🔹 CREATE STUDIO
    // ----------------------------------------------------------------------
    public StudioRes createStudio(UUID userId,
                                  StudioCreateReq req,
                                  MultipartFile logoFile,
                                  MultipartFile bannerFile,
                                  MultipartFile profileFile) {

        // 1️⃣ Find or create user
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            user = User.builder()
                    .phone("UNKNOWN")
                    .build();
            userRepository.save(user);
        } else {
            userRepository.save(user);
        }

        // 2️⃣ Prevent duplicate studio_code (idForLogin)
        if (req.idForLogin() != null && !req.idForLogin().isBlank()) {
            studioRepository.findByIdForLogin(req.idForLogin()).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Studio login ID already exists: " + req.idForLogin());
            });
        }

        // 3️⃣ Generate studio login code
        String studioCode = (req.idForLogin() == null || req.idForLogin().isBlank())
                ? IdGenerator.generateId(req.fullName())
                : req.idForLogin();

        // 4️⃣ Upload files if present
        String logoUrl = req.logoImageUrl();
        String bannerUrl = req.bannerImageUrl();
        String profileUrl = req.profileImageUrl();

        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = fileStorageService.saveFile(logoFile);
        }
        if (bannerFile != null && !bannerFile.isEmpty()) {
            bannerUrl = fileStorageService.saveFile(bannerFile);
        }
        if (profileFile != null && !profileFile.isEmpty()) {
            profileUrl = fileStorageService.saveFile(profileFile);
        }

        // 5️⃣ Create HairStudioDetail
        HairStudioDetail studio = HairStudioDetail.builder()
                .userId(user.getId())
                .idForLogin(studioCode)
                .businessNo(req.businessNo())
                .ownerName(req.ownerName())
                .logoImageUrl(logoUrl)
                .bannerImageUrl(bannerUrl)
                .profileImageUrl(profileUrl)
                .latitude(req.latitude())
                .longitude(req.longitude())
                .position(Position.STUDIO_OWNER)
                .build();

        studioRepository.save(studio);

        // 6️⃣ Create AuthLocal for password login
        AuthLocal auth = AuthLocal.builder()
                // .id(UUID.randomUUID()) ✅ removed: handled by BaseEntity
                .userId(user.getId())
                .passwordHash(passwordEncoder.encode(req.password()))
                .passwordUpdatedAt(Instant.now())
                .build();

        authLocalRepository.save(auth);

        return mapToRes(studio);
    }

    // ----------------------------------------------------------------------
    // 🔹 READ / DELETE STUDIOS
    // ----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public StudioRes getStudio(UUID studioId) {
        HairStudioDetail studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));

        if (studio.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio has been deleted");

        return mapToRes(studio);
    }

    @Transactional(readOnly = true)
    public List<StudioRes> getAllStudios() {
        return studioRepository.findAll().stream()
                .filter(s -> s.getDeletedAt() == null)
                .map(this::mapToRes)
                .collect(Collectors.toList());
    }

    public void deleteStudio(UUID studioId) {
        HairStudioDetail studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));

        if (studio.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio already deleted");

        studio.setDeletedAt(Instant.now());
        studioRepository.save(studio);
    }

    // ----------------------------------------------------------------------
    // 🔹 MAPPER
    // ----------------------------------------------------------------------
    private StudioRes mapToRes(HairStudioDetail s) {
        return new StudioRes(
                s.getId(),
                s.getIdForLogin(),
                s.getBusinessNo(),
                s.getOwnerName(),
                s.getStudioPhone(),
                null,
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
