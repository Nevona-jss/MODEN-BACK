package com.moden.modenapi.common.controller;

import com.moden.modenapi.common.dto.UploadResponse;
import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.FileNameUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
@Tag(name = "IMAGE UPLOAD", description = "Universal image uploader")
@RestController
@RequestMapping("/api/universalUploads")
@RequiredArgsConstructor
public class UploadController {

    @Value("${file.upload-dir:uploads/services}")
    private String uploadRoot; // e.g. /home/hyona/IdeaProjects/MODEN/uploads

    @Operation(
            summary = "Universal uploads (images)",
            description = """
            Multipart upload for images.
            - form field: file
            - optional query: folder (default: misc)
            Returns public URL and relative path to save in DB.
            """
    )
    @PreAuthorize("isAuthenticated()")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseMessage<UploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "misc") String folder
    ) throws Exception {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseMessage.failure("File is empty"));
        }

        // sanitize folder (letters, numbers, dash, underscore, slash)
        folder = sanitizeFolder(folder);
        if (!StringUtils.hasText(folder)) folder = "misc";

        // content-type guard (allow only images)
        String ct = Objects.toString(file.getContentType(), "");
        if (!ct.startsWith("image/")) {
            // (optional) try probe
            String probed = Files.probeContentType(Path.of(Objects.requireNonNullElse(file.getOriginalFilename(), "file")));
            if (probed == null || !probed.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ResponseMessage.failure("Only image uploads are allowed"));
            }
            ct = probed;
        }

        Instant now = Instant.now();
        String generated = FileNameUtil.generate(file.getOriginalFilename(), now);
        String datePart  = FileNameUtil.datePartition(now); // yyyy/MM/dd

        // Build safe absolute path and ensure it stays inside root
        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path dir  = root.resolve(folder).resolve(datePart).normalize();
        if (!dir.startsWith(root)) {
            return ResponseEntity.badRequest().body(ResponseMessage.failure("Invalid folder path"));
        }
        Files.createDirectories(dir);

        Path dst = dir.resolve(generated).normalize();
        if (!dst.startsWith(root)) {
            return ResponseEntity.badRequest().body(ResponseMessage.failure("Invalid target path"));
        }

        file.transferTo(dst.toFile());

        String relPath = folder + "/" + datePart + "/" + generated;   // store in DB
        String url     = "/uploads/" + relPath;                        // public URL

        var body = new UploadResponse(url, relPath, generated, file.getSize(), ct);
        return ResponseEntity.status(201).body(ResponseMessage.success("Uploaded", body));
    }

    private String sanitizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) return "misc";
        // keep alnum, _, -, / ; collapse others to _
        folder = folder.replaceAll("[^a-zA-Z0-9_\\-/]", "_");
        // remove any ".." sequences defensively
        folder = folder.replace("..", "");
        // trim leading slashes
        while (folder.startsWith("/")) folder = folder.substring(1);
        return folder;
    }
}
