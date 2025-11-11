package com.moden.modenapi.modules.product.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.product.dto.*;
import com.moden.modenapi.modules.product.model.StudioProduct;
import com.moden.modenapi.modules.product.repository.StudioProductRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    private final HairStudioDetailRepository studioDetailRepository; // ✅ token'dan studio aniqlash uchun

    @Override
    protected StudioProductRepository getRepository() { return productRepository; }

    // 🔹 CREATE — studioId ni token’dan avtomatik qo'yamiz (body’dagi studioId e’tiborga olinmaydi)
    public StudioProductRes create(StudioProductCreateReq req) {
        UUID studioId = resolveMyStudioId();  // ✅ token → studioId

        if (req.productName() == null || req.productName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productName is required");
        if (req.price() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price is required");

        StudioProduct p = StudioProduct.builder()
                .studioId(studioId)
                .productName(req.productName().trim())
                .price(req.price())
                .notes(req.notes())
                .volumeLiters(req.volumeLiters())
                .designerTipPercent(req.designerTipPercent())
                .build();

        return mapToRes(productRepository.save(p));
    }

    // 🔹 UPDATE — null bo‘lmaganlar yangilanadi (save() YO'Q!)
    public StudioProductRes update(UUID productId, StudioProductUpdateReq req) {
        StudioProduct p = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or deleted"));

        if (req.productName() != null)        p.setProductName(req.productName().trim());
        if (req.price() != null)              p.setPrice(req.price());
        if (req.notes() != null)              p.setNotes(req.notes());
        if (req.volumeLiters() != null)       p.setVolumeLiters(req.volumeLiters());
        if (req.designerTipPercent() != null) p.setDesignerTipPercent(req.designerTipPercent());


        return mapToRes(p);
    }


    // 🔹 DELETE (soft)
    public void softDelete(UUID productId) {
        StudioProduct p = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or already deleted"));

        // (ixtiyoriy) egasi tekshiruvi kerak bo‘lsa, shu yerda tekshiring.

        p.setDeletedAt(Instant.now());
        productRepository.save(p);
    }

    @Transactional(readOnly = true)
    public StudioProductRes getProduct(UUID productId) {
        StudioProduct product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or deleted"));
        return mapToRes(product);
    }

    // 🔹 LIST — studioId berilmasa, token’dan avtomatik aniqlaymiz
    @Transactional(readOnly = true)
    public List<StudioProductRes> getAllByStudio() {
        UUID sid = resolveMyStudioId(); // ✅ token → studioId
        return productRepository
                .findAllByStudioIdAndDeletedAtIsNullOrderByProductNameAsc(sid)
                .stream().map(this::mapToRes).collect(Collectors.toList());
    }

    private StudioProductRes mapToRes(StudioProduct p) {
        return new StudioProductRes(
                p.getId(), p.getStudioId(), p.getProductName(), p.getPrice(),
                p.getNotes(), p.getVolumeLiters(), p.getDesignerTipPercent(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }

    /* ================= Helpers ================= */

    /** Joriy foydalanuvchi userId (UUID) — OAuth2siz, Authentication#getName() dan */
    private UUID currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a.getName() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        try {
            return UUID.fromString(a.getName());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Principal name is not a UUID: " + a.getName());
        }
    }

    /** Token’dagi userId orqali bog‘langan studioning ID sini topish */
    private UUID resolveMyStudioId() {
        UUID userId = currentUserId();
        return studioDetailRepository.findByUserId(userId)
                .map(s -> s.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user"));
    }
}
