package com.moden.modenapi.common.service;

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

    /**
     * events 폴더에 이미지 업로드
     * @return 업로드된 이미지의 public URL (예: /uploads/events/2025/11/24/xxx.jpg)
     */
    public String uploadEventImage(MultipartFile file) throws Exception {
        return uploadImage(file, "events");
    }

    /**
     * 공통 이미지 업로드 로직
     * @param file   업로드 파일
     * @param folder 업로드 하위 폴더명 (예: "events", "services" 등)
     * @return public URL (String)
     */
    private String uploadImage(MultipartFile file, String folder) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        folder = sanitizeFolder(folder);
        if (!StringUtils.hasText(folder)) folder = "misc";

        // only image/*
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

        // 원본 그대로 저장 (압축은 안 함) — 필요하면 Thumbnailator 로 교체 가능
        file.transferTo(dst.toFile());

        String relPath = folder + "/" + datePart + "/" + generated; // DB 저장용
        String url     = "/uploads/" + relPath;                      // public URL

        // 이제는 URL만 반환
        return url;
    }

    private String sanitizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) return "misc";
        // 허용된 문자만
        folder = folder.replaceAll("[^a-zA-Z0-9_\\-/]", "_");
        // 상위 경로 방지
        folder = folder.replace("..", "");
        while (folder.startsWith("/")) folder = folder.substring(1);
        return folder;
    }
}
