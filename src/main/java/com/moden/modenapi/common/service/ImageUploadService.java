package com.moden.modenapi.common.service;

import com.moden.modenapi.common.dto.UploadResponse;
import com.moden.modenapi.common.utils.FileNameUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;

@Service
public class ImageUploadService {

    @Value("${file.upload-dir:uploads/services}")
    private String uploadRoot;

    /** events 폴더에 이미지 업로드 */
    public UploadResponse uploadEventImage(MultipartFile file) throws Exception {
        return uploadImage(file, "events");
    }

    /** 공통 이미지 업로드 로직 (UploadController logic 복사) */
    private UploadResponse uploadImage(MultipartFile file, String folder) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        folder = sanitizeFolder(folder);
        if (!StringUtils.hasText(folder)) folder = "misc";

        // only image /*
        String ct = Objects.toString(file.getContentType(), "");
        if (!ct.startsWith("image/")) {
            String probed = Files.probeContentType(
                    Path.of(Objects.requireNonNullElse(file.getOriginalFilename(), "file"))
            );
            if (probed == null || !probed.startsWith("image/")) {
                throw new IllegalArgumentException("Only image uploads are allowed");
            }
            ct = probed;
        }

        Instant now = Instant.now();
        String generated = FileNameUtil.generate(file.getOriginalFilename(), now);
        String datePart  = FileNameUtil.datePartition(now);  // yyyy/MM/dd

        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path dir  = root.resolve(folder).resolve(datePart).normalize();
        if (!dir.startsWith(root)) {
            throw new IllegalArgumentException("Invalid folder path");
        }
        Files.createDirectories(dir);

        Path dst = dir.resolve(generated).normalize();
        if (!dst.startsWith(root)) {
            throw new IllegalArgumentException("Invalid target path");
        }

        file.transferTo(dst.toFile());

        String relPath = folder + "/" + datePart + "/" + generated; // DB 저장용
        String url     = "/uploads/" + relPath;                      // public URL

        return new UploadResponse(url, relPath, generated, file.getSize(), ct);
    }

    private String sanitizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) return "misc";
        folder = folder.replaceAll("[^a-zA-Z0-9_\\-/]", "_");
        folder = folder.replace("..", "");
        while (folder.startsWith("/")) folder = folder.substring(1);
        return folder;
    }
}
