package com.moden.modenapi.common.controller;

import com.moden.modenapi.common.response.ResponseMessage;
import com.moden.modenapi.common.utils.FileNameUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Value("${file.upload-dir:uploads}")
    private String uploadRoot; // e.g. /home/.../uploads

    @Operation(
            summary = "Image upload (single or multiple)",
            description = """
            Universal image upload endpoint.
            - Single: form field name = file
            - Multiple: form field name = files (array)
            Response: List<String> (image URLs).
            All files are stored directly under /uploads.
            """
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseMessage<List<String>>> uploadUniversal(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        try {
            List<MultipartFile> targetFiles = new ArrayList<>();

            if (file != null && !file.isEmpty()) {
                targetFiles.add(file);
            }
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

            List<String> urls = new ArrayList<>();
            for (MultipartFile f : targetFiles) {
                String url = processAndSaveImage(f);
                urls.add(url);
            }

            return ResponseEntity.status(201)
                    .body(ResponseMessage.success("Uploaded", urls));

        } catch (IllegalArgumentException e) {
            // 이미지가 아니거나 잘못된 파일 등
            return ResponseEntity.badRequest()
                    .body(ResponseMessage.failure(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // TODO: logger 로 교체
            return ResponseEntity.internalServerError()
                    .body(ResponseMessage.failure("Internal server error during upload"));
        }
    }

    /**
     * 이미지 압축 + 저장 후, public URL(String) 만 반환
     */
    private String processAndSaveImage(MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        // 1) 원본 파일명/확장자
        String originalName = Objects.toString(file.getOriginalFilename(), "image");
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot != -1) {
            ext = originalName.substring(dot + 1).toLowerCase();
        }

        // 2) 최종 저장 포맷 결정
        //    - heic/heif 포함 “애매한” 확장자는 전부 jpg 로 통일
        String targetFormat;
        switch (ext) {
            case "png":
                targetFormat = "png";
                break;
            case "webp":
                targetFormat = "webp";
                break;
            default:
                // jpg, jpeg, heic, heif, heif, bmp, gif 등등 → 전부 jpg 로 저장
                targetFormat = "jpg";
                break;
        }

        // 3) unique file name 생성
        Instant now = Instant.now();
        String generated = FileNameUtil.generate(originalName, now);

        // FileNameUtil 이 heic 같은 확장자 그대로 붙였을 수 있으니,
        // 우리가 정한 targetFormat 으로 확장자 강제 변경
        int genDot = generated.lastIndexOf('.');
        if (genDot != -1) {
            generated = generated.substring(0, genDot + 1) + targetFormat;
        } else {
            generated = generated + "." + targetFormat;
        }

        // 4) 저장 경로 준비
        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        Path dst = root.resolve(generated).normalize();
        if (!dst.startsWith(root)) {
            throw new IllegalArgumentException("Invalid target path");
        }

        // 5) 이미지로 읽어서 리사이즈 + 재인코딩
        try (InputStream in = file.getInputStream()) {
            Thumbnails.of(in)
                    .size(1920, 1920)       // 최대 1920x1920
                    .outputFormat(targetFormat)
                    .outputQuality(0.7f)
                    .toFile(dst.toFile());
        } catch (Exception e) {
            // 이미지로 디코딩이 안 되면 여기서 에러
            throw new IllegalArgumentException("Only image uploads are allowed");
        }

        return "/uploads/" + generated;
    }

}
