package com.moden.modenapi.common.controller;

import com.moden.modenapi.common.dto.UploadResponse;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.FileNameUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Tag(name = "IMAGE UPLOAD", description = "Universal image uploader")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    // application.yml:
    // file:
    //   upload-dir: uploads
    @Value("${file.upload-dir:uploads}")
    private String uploadRoot; // e.g. /home/.../uploads

    @Operation(
            summary = "Image upload (single or multiple)",
            description = """
            Universal image upload endpoint.
            - Single: form field name = file
            - Multiple: form field name = files (array)
            Response: List<UploadResponse> (even for single file)
            All files are stored directly under /uploads.
            """
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseMessage<List<UploadResponse>>> uploadUniversal(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws Exception {

        List<MultipartFile> targetFiles = new ArrayList<>();

        // 단일 업로드(file)도 List에 합치기
        if (file != null && !file.isEmpty()) {
            targetFiles.add(file);
        }

        // 다중 업로드(files)도 추가
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    targetFiles.add(f);
                }
            }
        }

        if (targetFiles.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ResponseMessage.failure("No files provided"));
        }

        List<UploadResponse> results = new ArrayList<>();
        for (MultipartFile f : targetFiles) {
            UploadResponse res = processAndSaveImage(f);
            results.add(res);
        }

        return ResponseEntity.status(201)
                .body(ResponseMessage.success("Uploaded", results));
    }

    // ============================
    //  🔽 common image + compress
    // ============================
    private UploadResponse processAndSaveImage(MultipartFile file) throws Exception {

        // content-type guard (allow only images)
        String ct = Objects.toString(file.getContentType(), "");
        if (!ct.startsWith("image/")) {
            // (optional) try probe
            String probed = Files.probeContentType(
                    Path.of(Objects.requireNonNullElse(file.getOriginalFilename(), "file"))
            );
            if (probed == null || !probed.startsWith("image/")) {
                throw new IllegalArgumentException("Only image uploads are allowed");
            }
            ct = probed;
        }

        // ⏱ unique file name (time + random + original ext)
        Instant now = Instant.now();
        String generated = FileNameUtil.generate(file.getOriginalFilename(), now);

        // root directory: faqat bitta papka (uploadRoot)
        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        Path dst = root.resolve(generated).normalize();
        if (!dst.startsWith(root)) {
            throw new IllegalArgumentException("Invalid target path");
        }

        // 🔻 IMAGE COMPRESS + SAVE
        String format = "jpg";
        if ("image/png".equalsIgnoreCase(ct)) {
            format = "png";
        } else if ("image/webp".equalsIgnoreCase(ct)) {
            // Thumbnailator webp 지원이 애매해서 안전하게 jpg로 변환
            format = "jpg";
            ct = "image/jpeg";
        } else {
            ct = "image/jpeg";
        }

        try (InputStream in = file.getInputStream()) {
            Thumbnails.of(in)
                    .size(1920, 1920)       // maksimal kenglik 1920px
                    .outputFormat(format)
                    .outputQuality(0.7f)    // web uchun ideal
                    .toFile(dst.toFile());
        }


        long compressedSize = Files.size(dst);

        String relPath = generated;               // DB 저장용
        String url     = "/uploads/" + generated; // static resource 매핑 필요

        return new UploadResponse(url, relPath, generated, compressedSize, ct);
    }
}
