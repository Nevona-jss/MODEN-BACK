package com.moden.modenapi.modules.designer.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.designer.model.DesignerPortfolioItem;
import com.moden.modenapi.modules.designer.service.DesignerPortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Designer Portfolio", description = "Manage designer portfolio (IDs stored on DesignerDetail)")
@RestController
@RequestMapping("/api/designers/{designerId}/portfolio")
@RequiredArgsConstructor
public class DesignerPortfolioController {

    private final DesignerPortfolioService service;

    // ---------------------------
    // Create one item and attach
    // ---------------------------
    @Operation(summary = "Create portfolio item and attach")
    @PostMapping("/items")
    public ResponseEntity<ResponseMessage<DesignerPortfolioItem>> createAndAttach(
            @PathVariable UUID designerId,
            @Valid @RequestBody DesignerPortfolioItem req
    ) {
        var saved = service.createAndAttach(designerId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Portfolio item created & attached", saved));
    }

    // ---------------------------
    // Bulk create & attach
    // ---------------------------
    @Operation(summary = "Bulk create portfolio items and attach")
    @PostMapping("/items/bulk")
    public ResponseEntity<ResponseMessage<List<DesignerPortfolioItem>>> bulkCreateAndAttach(
            @PathVariable UUID designerId,
            @Valid @RequestBody List<DesignerPortfolioItem> items
    ) {
        var saved = service.bulkCreateAndAttach(designerId, items);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseMessage.success("Portfolio items created & attached", saved));
    }

    // ---------------------------
    // Replace whole list with IDs
    // ---------------------------
    @Operation(summary = "Set portfolio by IDs (replace all)")
    @PutMapping("/ids")
    public ResponseEntity<ResponseMessage<Void>> setPortfolio(
            @PathVariable UUID designerId,
            @RequestBody List<UUID> itemIds
    ) {
        service.setPortfolio(designerId, itemIds);
        return ResponseEntity.ok(ResponseMessage.success("Portfolio replaced", null));
    }

    // ---------------------------
    // Append existing IDs to tail
    // ---------------------------
    @Operation(summary = "Add existing items by IDs (append)")
    @PostMapping("/ids")
    public ResponseEntity<ResponseMessage<Void>> addItems(
            @PathVariable UUID designerId,
            @RequestBody List<UUID> itemIds
    ) {
        service.addItems(designerId, itemIds);
        return ResponseEntity.ok(ResponseMessage.success("Portfolio updated (items appended)", null));
    }

    // ---------------------------
    // Remove one item reference
    // ---------------------------
    @Operation(summary = "Detach one item (optionally soft-delete item)")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable UUID designerId,
            @PathVariable UUID itemId,
            @RequestParam(name = "deleteItem", defaultValue = "false") boolean deleteItem
    ) {
        service.removeItem(designerId, itemId, deleteItem);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------
    // Get ordered portfolio
    // ---------------------------
    @Operation(summary = "Get ordered portfolio")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<DesignerPortfolioItem>>> getPortfolio(
            @PathVariable UUID designerId
    ) {
        var list = service.getPortfolio(designerId);
        return ResponseEntity.ok(ResponseMessage.success("Portfolio fetched", list));
    }

    // ---------------------------
    // Reorder
    // ---------------------------
    @Operation(summary = "Reorder portfolio (same IDs, new order)")
    @PatchMapping("/reorder")
    public ResponseEntity<ResponseMessage<Void>> reorder(
            @PathVariable UUID designerId,
            @RequestBody List<UUID> newOrder
    ) {
        service.reorder(designerId, newOrder);
        return ResponseEntity.ok(ResponseMessage.success("Portfolio reordered", null));
    }
}
