package com.moden.modenapi.modules.product.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.CurrentUserUtil;
import com.moden.modenapi.modules.product.dto.StudioProductCreateReq;
import com.moden.modenapi.modules.product.dto.StudioProductRes;
import com.moden.modenapi.modules.product.dto.StudioProductUpdateReq;
import com.moden.modenapi.modules.product.service.StudioProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "PRODUCT",
        description = "HAIR_STUDIO 및 DESIGNER 계정이 현재 소속된 샵의 상품을 관리합니다."
)
@RestController
@RequestMapping("/api/studios/products")
@RequiredArgsConstructor
public class StudioProductController {

    private final StudioProductService productService;

    // CREATE — 현재 로그인한 HAIR_STUDIO 또는 DESIGNER 의 스튜디오에 상품 생성
    @Operation(summary = "Create product for current studio (HAIR_STUDIO or DESIGNER)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PostMapping("/add")
    public ResponseEntity<ResponseMessage<StudioProductRes>> create(
            @RequestBody StudioProductCreateReq req
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        StudioProductRes result = productService.create(currentUserId, req);
        return ResponseEntity.ok(ResponseMessage.success("Product created successfully", result));
    }

    // UPDATE — partial
    @Operation(summary = "Update product (current studio, HAIR_STUDIO or DESIGNER)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @PatchMapping("/edit/{productId}")
    public ResponseEntity<ResponseMessage<StudioProductRes>> update(
            @PathVariable UUID productId,
            @RequestBody StudioProductUpdateReq req
    ) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        StudioProductRes result = productService.update(currentUserId, productId, req);
        return ResponseEntity.ok(ResponseMessage.success("Product updated successfully", result));
    }

    // DELETE (soft)
    @Operation(summary = "Soft delete product (current studio, HAIR_STUDIO or DESIGNER)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<ResponseMessage<Void>> delete(@PathVariable UUID productId) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        productService.softDelete(currentUserId, productId);   // ✅ productId 전달
        return ResponseEntity.ok(ResponseMessage.success("Product deleted successfully", null));
    }

    // GET ONE
    @Operation(summary = "Get product detail (current studio, HAIR_STUDIO or DESIGNER)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/get/{productId}")
    public ResponseEntity<ResponseMessage<StudioProductRes>> getOne(@PathVariable UUID productId) {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        StudioProductRes result = productService.getProduct(currentUserId, productId);
        return ResponseEntity.ok(ResponseMessage.success(result));
    }

    // LIST — 현재 로그인한 스튜디오 기준 전체 목록
    @Operation(summary = "List products for current studio (HAIR_STUDIO or DESIGNER)")
    @PreAuthorize("hasAnyRole('HAIR_STUDIO','DESIGNER')")
    @GetMapping("/list")
    public ResponseEntity<ResponseMessage<List<StudioProductRes>>> list() {
        UUID currentUserId = CurrentUserUtil.currentUserId();
        List<StudioProductRes> result = productService.getAllByStudio(currentUserId);
        return ResponseEntity.ok(ResponseMessage.success(result));
    }
}
