package com.moden.modenapi.common.service;

import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@Component
public class StudioContextService {

    private final HairStudioDetailRepository studioDetailRepository;
    private final DesignerDetailRepository designerDetailRepository;

    public StudioContextService(HairStudioDetailRepository studioDetailRepository,
                                DesignerDetailRepository designerDetailRepository) {
        this.studioDetailRepository = studioDetailRepository;
        this.designerDetailRepository = designerDetailRepository;
    }

    /**
     * 현재 로그인한 actor 기준으로 studioId(= studioUserId)를 resolve
     *
     *  - HAIR_STUDIO:
     *      studioDetail.userId = currentUserId 이어야 함
     *      business studioId = currentUserId (studioUserId)
     *
     *  - DESIGNER:
     *      DesignerDetail.hairStudioId = studioUserId
     *      studioDetail.userId = studioUserId 이 존재해야 함
     *
     *  ⚠️ 중요한 전제:
     *    - StudioProduct.studioId      = studioUserId
     *    - DesignerDetail.hairStudioId = studioUserId
     *    - CustomerDetail.studioId     = studioUserId
     */
    public UUID resolveStudioIdForCurrentUser() {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        return resolveStudioIdForActor(currentUserId);
    }

    public UUID resolveStudioIdForActor(UUID currentUserId) {
        // HAIR_STUDIO 계정
        if (hasRole("HAIR_STUDIO")) {
            studioDetailRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.FORBIDDEN,
                                    "Studio not found for current user"
                            )
                    );
            return currentUserId; // ✅ studioUserId
        }

        // DESIGNER 계정
        if (hasRole("DESIGNER")) {
            var dd = designerDetailRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.FORBIDDEN,
                                    "Designer profile not found"
                            )
                    );

            UUID studioUserId = dd.getHairStudioId(); // ✅ studioUserId (HAIR_STUDIO.user.id)

            studioDetailRepository.findByUserIdAndDeletedAtIsNull(studioUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.FORBIDDEN,
                                    "Studio not found for designer"
                            )
                    );

            return studioUserId;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only studio or designer can access this resource"
        );
    }

    // === 여기서부터 네가 준 hasRole 구현 ===
    private boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        final String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(target::equals);
    }
}
