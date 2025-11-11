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

@Tag(name = "HAIR-STUDIO PRODUCTS", description = "Only HAIR_STUDIO can manage products")
@RestController
@RequestMapping("/studios/products")
@RequiredArgsConstructor
public class StudioProductController {

    private final StudioProductService productService;

    // CREATE — studioId body ichida majburiy
    @Operation(summary = "Create product (studioId in body, HAIR_STUDIO only)")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @PostMapping("/add")
    public ResponseEntity<StudioProductRes> createProduct(@RequestBody StudioProductCreateReq req) {
        return ResponseEntity.ok(productService.create(req));
    }

    // UPDATE — partial
    @Operation(summary = "Update product (partial, HAIR_STUDIO only)")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @PatchMapping("/edit/{productId}")
    public ResponseEntity<StudioProductRes> updateProduct(
            @PathVariable UUID productId,
            @RequestBody StudioProductUpdateReq req
    ) {
        return ResponseEntity.ok(productService.update(productId, req));
    }

    // DELETE (soft)
    @Operation(summary = "Soft delete product (HAIR_STUDIO only)")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<Map<String,String>> deleteProduct(@PathVariable UUID productId) {
        productService.softDelete(productId);
        return ResponseEntity.ok(Map.of("message","Deleted"));
    }

    // GET ONE
    @Operation(summary = "Get product detail (HAIR_STUDIO only)")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @GetMapping("/get/{productId}")
    public ResponseEntity<StudioProductRes> getProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(productService.getProduct(productId));
    }

    // LIST — studioId query param (?studioId=...)
    @Operation(summary = "List products by studioId (query param, HAIR_STUDIO only)")
    @PreAuthorize("hasRole('HAIR_STUDIO')")
    @GetMapping("/list")
    public ResponseEntity<List<StudioProductRes>> listMyProducts() {
        return ResponseEntity.ok(productService.getAllByStudio());
    }
}
