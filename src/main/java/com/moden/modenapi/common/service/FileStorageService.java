package com.moden.modenapi.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    @Value("${file.base-url:http://localhost:7000/uploads}")
    private String baseUrl;

    public String saveFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            // ensure directory exists
            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            // generate unique file fullName
            String ext = getExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + (ext != null ? "." + ext : "");
            Path target = dirPath.resolve(fileName);

            // save file to disk
            file.transferTo(target.toFile());

            // return URL
            return baseUrl.endsWith("/") ? baseUrl + fileName : baseUrl + "/" + fileName;
        } catch (IOException e) {
            log.error("❌ Failed to save file", e);
            throw new RuntimeException("Could not save file: " + e.getMessage(), e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(dot + 1) : null;
    }
}
