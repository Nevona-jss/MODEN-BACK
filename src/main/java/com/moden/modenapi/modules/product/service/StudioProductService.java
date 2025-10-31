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
    protected StudioProductRepository getRepository() {
        return productRepository;
    }

    // 🔹 CREATE
    public StudioProductRes createProduct(UUID studioId, StudioProductCreateReq req) {
        StudioProduct product = StudioProduct.builder()
                .studioId(studioId)
                .name(req.name())
                .category(req.category())
                .type(req.type())
                .price(req.price())
                .stock(req.stock())
                .image(req.image())
                .build();

        create(product);
        return mapToRes(product);
    }

    // 🔹 UPDATE
    public StudioProductRes updateProduct(UUID productId, StudioProductUpdateReq req) {
        StudioProduct product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or deleted"));

        if (req.name() != null) product.setName(req.name());
        if (req.category() != null) product.setCategory(req.category());
        if (req.type() != null) product.setType(req.type());
        if (req.price() != null) product.setPrice(req.price());
        if (req.stock() != null) product.setStock(req.stock());
        if (req.image() != null) product.setImage(req.image());

        product.setUpdatedAt(Instant.now());
        update(product);
        return mapToRes(product);
    }

    // 🔹 DELETE (soft delete)
    public void deleteProduct(UUID productId) {
        StudioProduct product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or already deleted"));
        product.setDeletedAt(Instant.now());
        update(product);
    }

    // 🔹 GET ONE (only non-deleted)
    @Transactional(readOnly = true)
    public StudioProductRes getProduct(UUID productId) {
        StudioProduct product = productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found or deleted"));
        return mapToRes(product);
    }

    // 🔹 GET ALL (only non-deleted)
    @Transactional(readOnly = true)
    public List<StudioProductRes> getAllByStudio(UUID studioId) {
        return productRepository.findAllActiveByStudioId(studioId)
                .stream()
                .map(this::mapToRes)
                .collect(Collectors.toList());
    }

    // 🔹 Mapper
    private StudioProductRes mapToRes(StudioProduct p) {
        return new StudioProductRes(
                p.getId(),
                p.getStudioId(),
                p.getName(),
                p.getCategory(),
                p.getType(),
                p.getPrice(),
                p.getStock(),
                p.getImage(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
