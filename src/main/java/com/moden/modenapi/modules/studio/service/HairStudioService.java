package com.moden.modenapi.modules.studio.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.common.service.FileStorageService;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.dto.StudioUpdateReq;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import com.moden.modenapi.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class HairStudioService extends BaseService<HairStudioDetail> {

    private final HairStudioDetailRepository studioRepository;
    private final JwtProvider jwtProvider;
    private final HttpServletRequest request;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepo; // ✅ owner info 조회용

    @Override
    protected HairStudioDetailRepository getRepository() {
        return studioRepository;
    }

    @Transactional(readOnly = true)
    public StudioRes getCurrentStudio() {
        UUID userId = extractUserIdFromToken();
        HairStudioDetail studio = studioRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio profile not found"));
        return mapToRes(studio);
    }

    public StudioRes updateStudio(UUID studioId,
                                  StudioUpdateReq req,
                                  MultipartFile logoFile,
                                  MultipartFile bannerFile,
                                  MultipartFile profileFile) {

        HairStudioDetail studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));

        if (studio.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio has been deleted");
        }

        if (req.studioPhone() != null) studio.setStudioPhone(req.studioPhone());
        if (req.address() != null)     studio.setAddress(req.address());
        if (req.description() != null) studio.setDescription(req.description());
        if (req.parkingInfo() != null) studio.setParkingInfo(req.parkingInfo());
        if (req.latitude() != null)    studio.setLatitude(req.latitude());
        if (req.longitude() != null)   studio.setLongitude(req.longitude());

        if (req.instagram() != null) studio.setInstagramUrl(req.instagram());
        if (req.naver() != null)     studio.setNaverUrl(req.naver());
        if (req.kakao() != null)     studio.setKakaoUrl(req.kakao());

        try {
            if (logoFile != null && !logoFile.isEmpty())    studio.setLogoImageUrl(fileStorageService.saveFile(logoFile));
            if (bannerFile != null && !bannerFile.isEmpty())studio.setBannerImageUrl(fileStorageService.saveFile(bannerFile));
            if (profileFile != null && !profileFile.isEmpty()) studio.setProfileImageUrl(fileStorageService.saveFile(profileFile));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed: " + e.getMessage());
        }

        studio.setUpdatedAt(Instant.now());
        studioRepository.save(studio);

        return mapToRes(studio);
    }

    public void deleteStudio(UUID studioId) {
        HairStudioDetail studio = studioRepository.findById(studioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Studio not found"));

        if (studio.getDeletedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Studio already deleted");

        studio.setDeletedAt(Instant.now());
        studioRepository.save(studio);
    }

    private UUID extractUserIdFromToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7).trim();
        if (!jwtProvider.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }

        return UUID.fromString(jwtProvider.getUserId(token));
    }

    // ✅ StudioRes 생성 통일: User에서 fullName/phone 로드, 생성자 순서 맞춤
    private StudioRes mapToRes(HairStudioDetail s) {
        User owner = userRepo.findById(s.getUserId())
                .orElse(null);

        String ownerFullName = owner != null ? owner.getFullName() : null;
        String ownerPhone    = owner != null ? owner.getPhone()    : null;

        return new StudioRes(
                // required
                s.getId(),
                s.getUserId(),
                ownerFullName,
                ownerPhone,
                // optional (순서 고정)
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
