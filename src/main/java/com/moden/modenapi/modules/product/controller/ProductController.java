package com.moden.modenapi.modules.product.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.product.dto.*;
import com.moden.modenapi.modules.product.service.StudioProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "HAIR STUDIO-PRODUCTS")
@RestController
@RequestMapping("/api/studios/{studioId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final StudioProductService productService;

    // 🔹 GET ALL (non-deleted only)
    @Operation(summary = "Get all active (non-deleted) products for a studio")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<StudioProductRes>>> getAllProducts(@PathVariable UUID studioId) {
        List<StudioProductRes> products = productService.getAllByStudio(studioId);
        return ResponseEntity.ok(ResponseMessage.success("Active product list retrieved", products));
    }

    // 🔹 GET ONE (non-deleted only)
    @Operation(summary = "Get product details by ID (non-deleted only)")
    @GetMapping("/{productId}")
    public ResponseEntity<ResponseMessage<StudioProductRes>> getProduct(@PathVariable UUID productId) {
        StudioProductRes product = productService.getProduct(productId);
        return ResponseEntity.ok(ResponseMessage.success("Product retrieved", product));
    }

    // 🔹 CREATE
    @Operation(summary = "Create a new product (Studio only)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ResponseMessage<StudioProductRes>> createProduct(
            @PathVariable UUID studioId,
            @RequestBody StudioProductCreateReq req
    ) {
        StudioProductRes created = productService.createProduct(studioId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Product created successfully", created));
    }

    // 🔹 UPDATE
    @Operation(summary = "Update existing product (Studio only)")
    @PutMapping("/{productId}")
    public ResponseEntity<ResponseMessage<StudioProductRes>> updateProduct(
            @PathVariable UUID productId,
            @RequestBody StudioProductUpdateReq req
    ) {
        StudioProductRes updated = productService.updateProduct(productId, req);
        return ResponseEntity.ok(ResponseMessage.success("Product updated successfully", updated));
    }

    // 🔹 DELETE (soft)
    @Operation(summary = "Soft delete product (Studio only)")
    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseMessage<Void>> deleteProduct(@PathVariable UUID productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ResponseMessage.success("Product deleted successfully", null));
    }
}
