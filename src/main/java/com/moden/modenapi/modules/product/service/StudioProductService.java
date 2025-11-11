package com.moden.modenapi.modules.product.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.product.dto.*;
import com.moden.modenapi.modules.product.model.StudioProduct;
import com.moden.modenapi.modules.product.repository.StudioProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @Override
    protected StudioProductRepository getRepository() { return productRepository; }

    // CREATE — studioId majburiy
    public StudioProductRes create(StudioProductCreateReq req) {
        if (req.studioId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studioId is required");
        if (req.productName() == null || req.productName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productName is required");
        if (req.price() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price is required");

        StudioProduct p = StudioProduct.builder()
                .studioId(req.studioId())
                .productName(req.productName().trim())
                .price(req.price())
                .notes(req.notes())
                .volumeLiters(req.volumeLiters())
                .designerTipPercent(req.designerTipPercent())
                .build();

        return mapToRes(productRepository.save(p));
    }

    // UPDATE — null bo‘lmaganlar yangilanadi
    public StudioProductRes update(UUID productId, StudioProductUpdateReq req) {
        StudioProduct p = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or deleted"));

        if (req.productName() != null)        p.setProductName(req.productName().trim());
        if (req.price() != null)              p.setPrice(req.price());
        if (req.notes() != null)              p.setNotes(req.notes());
        if (req.volumeLiters() != null)       p.setVolumeLiters(req.volumeLiters());
        if (req.designerTipPercent() != null) p.setDesignerTipPercent(req.designerTipPercent());

        return mapToRes(productRepository.save(p));
    }

    // DELETE (soft)
    public void softDelete(UUID productId) {
        StudioProduct p = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or already deleted"));
        p.setDeletedAt(Instant.now());
        productRepository.save(p);
    }

    @Transactional(readOnly = true)
    public StudioProductRes getProduct(UUID productId) {
        StudioProduct product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or deleted"));
        return mapToRes(product);
    }

    @Transactional(readOnly = true)
    public List<StudioProductRes> getAllByStudio(UUID studioId) {
        if (studioId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studioId is required");
        return productRepository
                .findAllByStudioIdAndDeletedAtIsNullOrderByCreatedAtDesc(studioId)
                .stream().map(this::mapToRes).collect(Collectors.toList());
    }

    private StudioProductRes mapToRes(StudioProduct p) {
        return new StudioProductRes(
                p.getId(), p.getStudioId(), p.getProductName(), p.getPrice(),
                p.getNotes(), p.getVolumeLiters(), p.getDesignerTipPercent(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
