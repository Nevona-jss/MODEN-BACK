package com.moden.modenapi.modules.product.controller;

import com.moden.modenapi.modules.product.dto.*;
import com.moden.modenapi.modules.product.service.StudioProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Studio Products", description = "Studio/Designer manage products")
@RestController
@RequestMapping("/studios")
@RequiredArgsConstructor
public class StudioProductController {

    private final StudioProductService productService;

    // CREATE
    @Operation(summary = "Create product for a studio")
    @PreAuthorize("hasAnyRole('ADMIN','HAIR_STUDIO','DESIGNER')")
    @PostMapping("/{studioId}/products")
    public ResponseEntity<StudioProductRes> createProduct(
            @PathVariable UUID studioId,
            @RequestBody StudioProductCreateReq req
    ) {
        StudioProductCreateReq fixed = req.studioId() == null
                ? new StudioProductCreateReq(
                studioId,
                req.productName(),
                req.price(),
                req.notes(),
                req.volumeLiters(),
                req.designerTipPercent()
        )
                : req;

        return ResponseEntity.ok(productService.create(fixed));
    }

    // UPDATE
    @Operation(summary = "Update product (partial)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PatchMapping("/products/{productId}")
    public ResponseEntity<StudioProductRes> updateProduct(
            @PathVariable UUID productId,
            @RequestBody StudioProductUpdateReq req
    ) {
        return ResponseEntity.ok(productService.update(productId, req));
    }

    // DELETE (soft)
    @Operation(summary = "Soft delete product")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Map<String,String>> deleteProduct(@PathVariable UUID productId) {
        productService.softDelete(productId);
        return ResponseEntity.ok(Map.of("message","Deleted"));
    }

    // GET ONE
    @Operation(summary = "Get product detail")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/products/{productId}")
    public ResponseEntity<StudioProductRes> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    // LIST by studio
    @Operation(summary = "List products of a studio")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/{studioId}/products")
    public ResponseEntity<List<StudioProductRes>> listProducts(@PathVariable UUID studioId) {
        return ResponseEntity.ok(productService.getAllByStudio(studioId));
    }
}
