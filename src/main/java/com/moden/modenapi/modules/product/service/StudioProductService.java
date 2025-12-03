package com.moden.modenapi.modules.product.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.product.dto.StudioProductCreateReq;
import com.moden.modenapi.modules.product.dto.StudioProductRes;
import com.moden.modenapi.modules.product.dto.StudioProductUpdateReq;
import com.moden.modenapi.modules.product.model.StudioProduct;
import com.moden.modenapi.modules.product.repository.StudioProductRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StudioProductService extends BaseService<StudioProduct> {

    private final StudioProductRepository productRepository;
    private final HairStudioDetailRepository studioDetailRepository;
    private final DesignerDetailRepository designerDetailRepository;

    @Override
    protected StudioProductRepository getRepository() {
        return productRepository;
    }

    // ================= CREATE =================
    public StudioProductRes create(UUID currentUserId, StudioProductCreateReq req) {
        UUID studioId = resolveStudioIdForActor(currentUserId);  // ✅ studioUserId

        if (req.productName() == null || req.productName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productName is required");
        }
        if (req.price() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price is required");
        }

        StudioProduct p = StudioProduct.builder()
                .productName(req.productName())
                .studioId(studioId)  // 🔹 StudioProduct.studioId = studioUserId
                .price(req.price())
                .notes(req.notes())
                .volumeLiters(req.volumeLiters())
                .designerTipPercent(req.designerTipPercent())
                .build();

        return mapToRes(productRepository.save(p));
    }

    // ================= UPDATE =================
    public StudioProductRes update(UUID currentUserId, UUID productId, StudioProductUpdateReq req) {
        UUID studioId = resolveStudioIdForActor(currentUserId);      // studioUserId
        StudioProduct p = getProductForStudio(studioId, productId);  // ✅ faqat o‘z studio’si

        if (req.productName() != null)        p.setProductName(req.productName().trim());
        if (req.price() != null)              p.setPrice(req.price());
        if (req.notes() != null)              p.setNotes(req.notes());
        if (req.volumeLiters() != null)       p.setVolumeLiters(req.volumeLiters());
        if (req.designerTipPercent() != null) p.setDesignerTipPercent(req.designerTipPercent());

        p.setUpdatedAt(Instant.now());
        return mapToRes(p);
    }

    // ================= DELETE (soft) =================
    public void softDelete(UUID currentUserId, UUID productId) {
        UUID studioId = resolveStudioIdForActor(currentUserId);
        StudioProduct p = getProductForStudio(studioId, productId);  // ✅ studio tekshiruv

        p.setDeletedAt(Instant.now());
        productRepository.save(p);
    }

    // ================= GET ONE =================
    @Transactional(readOnly = true)
    public StudioProductRes getProduct(UUID currentUserId, UUID productId) {
        UUID studioId = resolveStudioIdForActor(currentUserId);
        StudioProduct product = getProductForStudio(studioId, productId);
        return mapToRes(product);
    }

    // ================= LIST =================
    @Transactional(readOnly = true)
    public List<StudioProductRes> getAllByStudio(UUID currentUserId) {
        UUID studioId = resolveStudioIdForActor(currentUserId);
        return productRepository
                .findAllByStudioIdAndDeletedAtIsNullOrderByProductNameAsc(studioId)
                .stream()
                .map(this::mapToRes)
                .collect(Collectors.toList());
    }

    // ================= MAPPER =================
    private StudioProductRes mapToRes(StudioProduct p) {
        return new StudioProductRes(
                p.getId(),
                p.getStudioId(),
                p.getProductName(),
                p.getPrice(),
                p.getNotes(),
                p.getVolumeLiters(),
                p.getDesignerTipPercent(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    // ================= Helpers =================

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
     *    - StudioProduct.studioId = studioUserId
     *    - DesignerDetail.hairStudioId = studioUserId
     */
    private UUID resolveStudioIdForActor(UUID currentUserId) {
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

            // 여기서도 userId 기준으로 스튜디오 존재 확인
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
                "Only studio or designer can manage products"
        );
    }

    /** productId 해당 row 가 studioUserId 에 속하는지 확인 */
    private StudioProduct getProductForStudio(UUID studioUserId, UUID productId) {
        StudioProduct p = productRepository.findActiveById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found or deleted"
                        )
                );

        if (!p.getStudioId().equals(studioUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Product does not belong to your studio"
            );
        }

        return p;
    }

    private boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        final String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(target::equals);
    }
}
