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
import com.moden.modenapi.modules.product.dto.*;
import org.springframework.web.bind.annotation.*;


@Tag(name = "HAIR-STUDIO PRODUCTS", description = "Studio/Designer manage products")
@RestController
@RequestMapping("/studios/products")
@RequiredArgsConstructor
public class StudioProductController {

    private final StudioProductService productService;

    // CREATE
    @Operation(summary = "Create product (studioId comes in body)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','ADMIN')")
    @PostMapping
    public ResponseEntity<StudioProductRes> createProduct(
            @RequestBody StudioProductCreateReq req
    ) {
        return ResponseEntity.ok(productService.create(req));
    }

    // UPDATE — partial
    @Operation(summary = "Update product (partial)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','ADMIN')")
    @PatchMapping("/{productId}")
    public ResponseEntity<StudioProductRes> updateProduct(
            @PathVariable UUID productId,
            @RequestBody StudioProductUpdateReq req
    ) {
        return ResponseEntity.ok(productService.update(productId, req));
    }

    // DELETE (soft)
    @Operation(summary = "Soft delete product")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','ADMIN')")
    @DeleteMapping("/{productId}")
    public ResponseEntity<Map<String,String>> deleteProduct(@PathVariable UUID productId) {
        productService.softDelete(productId);
        return ResponseEntity.ok(Map.of("message","Deleted"));
    }

    // GET ONE
    @Operation(summary = "Get product detail")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','ADMIN')")
    @GetMapping("/{productId}")
    public ResponseEntity<StudioProductRes> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    // LIST — studioId query param orqali (?studioId=...)
    @Operation(summary = "List products by studioId (query param)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER','ADMIN')")
    @GetMapping
    public ResponseEntity<List<StudioProductRes>> listProducts(
            @RequestParam(name = "studioId") UUID studioId
    ) {
        return ResponseEntity.ok(productService.getAllByStudio(studioId));
    }
}
