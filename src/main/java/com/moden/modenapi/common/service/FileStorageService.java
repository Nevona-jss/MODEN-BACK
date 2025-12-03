package com.moden.modenapi.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 예: http://localhost:7000/uploads
     */
    @Value("${file.base-url:http://localhost:7000/uploads}")
    private String baseUrl;

    /**
     * 파일 저장 후, 전체 URL 반환
     * 예: http://localhost:7000/uploads/9e8c...-...-....jpg
     */
    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            // ensure directory exists
            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            // generate unique file name
            String ext = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + (ext != null ? "." + ext : "");
            Path target = dirPath.resolve(fileName).normalize();

            if (!target.startsWith(dirPath)) {
                throw new IllegalArgumentException("Invalid path");
            }

            // save file to disk
            file.transferTo(target.toFile());

            // return URL  (http://.../uploads/파일명)
            return buildUrl(fileName);

        } catch (IOException e) {
            log.error("❌ Failed to save file", e);
            throw new RuntimeException("Could not save file: " + e.getMessage(), e);
        }
    }

    /**
     * URL 기준으로 파일 삭제
     * - saveFile() 이 반환한 URL 그대로 넣으면 됨.
     * - 예: http://localhost:7000/uploads/xxx.jpg
     */
    public void deleteByUrl(String url) {
        if (!StringUtils.hasText(url)) return;

        try {
            String fileName = extractFileName(url);
            if (fileName == null) {
                log.warn("⚠️ Cannot resolve fileName from url: {}", url);
                return;
            }

            Path dirPath   = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path target    = dirPath.resolve(fileName).normalize();

            if (!target.startsWith(dirPath)) {
                log.warn("⚠️ Skip delete, invalid path: {}", target);
                return;
            }

            if (Files.exists(target)) {
                Files.delete(target);
                log.info("🧹 Deleted file: {}", target);
            } else {
                log.info("ℹ️ File not found (maybe already deleted): {}", target);
            }
        } catch (Exception e) {
            log.error("❌ Failed to delete file by url: {}", url, e);
        }
    }

    /* ==================== private helpers ==================== */

    private String getExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(dot + 1) : null;
    }

    private String buildUrl(String fileName) {
        String base = baseUrl;
        if (base.endsWith("/")) {
            return base + fileName;
        }
        return base + "/" + fileName;
    }

    /**
     * baseUrl / /uploads 패턴에서 파일명만 뽑아냄
     * ex)
     *   baseUrl = http://localhost:7000/uploads
     *   url     = http://localhost:7000/uploads/aaa.jpg → aaa.jpg
     *
     *   url     = /uploads/bbb.png → bbb.png  (혹시 상대경로로 저장한 경우 대비)
     */
    private String extractFileName(String url) {
        String u = url.trim();

        // 1) baseUrl 로 시작하는 경우
        if (StringUtils.hasText(baseUrl) && u.startsWith(baseUrl)) {
            String tail = u.substring(baseUrl.length()); // "/aaa.jpg" 또는 "aaa.jpg"
            while (tail.startsWith("/")) {
                tail = tail.substring(1);
            }
            return tail.isEmpty() ? null : tail;
        }

        // 2) "/uploads/..." 형식으로 들어온 경우
        String uploadsPrefix = "/uploads/";
        int idx = u.indexOf(uploadsPrefix);
        if (idx >= 0) {
            String tail = u.substring(idx + uploadsPrefix.length());
            while (tail.startsWith("/")) {
                tail = tail.substring(1);
            }
            return tail.isEmpty() ? null : tail;
        }

        // 3) 그 외: 그냥 마지막 '/' 이후를 파일명으로 간주 (최후의 수단)
        int slash = u.lastIndexOf('/');
        if (slash >= 0 && slash < u.length() - 1) {
            return u.substring(slash + 1);
        }

        return null;
    }
}
