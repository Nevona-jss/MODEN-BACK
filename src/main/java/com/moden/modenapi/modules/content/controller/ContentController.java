package com.moden.modenapi.modules.content.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.modules.content.model.Content;
import com.moden.modenapi.modules.content.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing content such as articles, banners, and posts.
 */
@Tag(name = "Content", description = "Content management APIs (Admin only)")
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService service;

    @Operation(summary = "List all content")
    @GetMapping
    public ResponseEntity<ResponseMessage<List<Content>>> list() {
        return ResponseEntity.ok(ResponseMessage.success("Content list retrieved", service.getAll()));
    }

    @Operation(summary = "Get content by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseMessage<Content>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ResponseMessage.success("Content retrieved", service.getById(id)));
    }

    @Operation(summary = "Create or publish content")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ResponseMessage<Content>> create(@RequestBody Content req) {
        return ResponseEntity.ok(ResponseMessage.success("Content published", service.save(req)));
    }

    @Operation(summary = "Delete content")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseMessage<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ResponseMessage.success("Content deleted", null));
    }
}
