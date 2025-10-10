package com.moden.modenapi.modules.product.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.product.model.StudioProduct;
import com.moden.modenapi.modules.product.service.StudioProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing salon products (shampoos, kits, etc.).
 */
@Tag(name = "Product", description = "Hair studio product management APIs")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final StudioProductService service;

    @Operation(summary = "List all products")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<StudioProduct>>> getAll() {
        return ResponseEntity.ok(ResponseMessage.success("Product list retrieved", service.getAll()));
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage<StudioProduct>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseMessage.success("Product retrieved", service.getById(id)));
    }

    @Operation(summary = "Create product")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ResponseMessage<StudioProduct>> create(@RequestBody StudioProduct req) {
        return ResponseEntity.ok(ResponseMessage.success("Product created", service.save(req)));
    }

    @Operation(summary = "Update product stock")
    @PutMapping("/{id}/stock")
    public ResponseEntity<ResponseMessage<StudioProduct>> updateStock(
            @PathVariable UUID id,          // ✅ fixed type to UUID
            @RequestParam int newStock
    ) {
        return ResponseEntity.ok(ResponseMessage.success("Stock updated", service.updateStock(id, newStock)));
    }

    @Operation(summary = "Delete product")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ResponseMessage.success("Product deleted", null));
    }
}
