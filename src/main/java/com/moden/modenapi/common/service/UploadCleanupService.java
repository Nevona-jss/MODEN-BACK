package com.moden.modenapi.common.service;

import com.moden.modenapi.modules.consultation.model.Consultation;
import com.moden.modenapi.modules.consultation.repository.ConsultationRepository;
import com.moden.modenapi.modules.event.model.Event;
import com.moden.modenapi.modules.event.repository.EventRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UploadCleanupService {

    @Value("${file.upload-dir:uploads}")
    private String uploadRoot;

    private final HairStudioDetailRepository studioRepo;
    private final EventRepository eventRepo;
    private final ConsultationRepository consultationRepo;
    // 필요하면 DesignerPortfolioRepository 등도 추가

    /**
     * 매일 새벽 4시에 orphan 파일 정리
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupOrphans() throws IOException {
        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            return;
        }

        // 1) DB 에서 실제로 사용 중인 URL 들 모으기
        Set<String> usedUrls = collectUsedUrls();

        // 2) 파일 시스템을 돌면서, 사용 중이 아닌 파일 삭제
        Files.walk(root)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        // 너무 최신 업로드(예: 1시간 이내)는 안전하게 건드리지 않기
                        Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                        if (lastModified.isAfter(Instant.now().minus(1, ChronoUnit.HOURS))) {
                            return;
                        }

                        String rel = root.relativize(path).toString().replace("\\", "/");
                        String url = "/uploads/" + rel;

                        if (!usedUrls.contains(url)) {
                            Files.delete(path);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private Set<String> collectUsedUrls() {
        Set<String> used = new HashSet<>();

        // 🔹 Studio 로고/배너/프로필 등
        for (HairStudioDetail s : studioRepo.findAll()) {
            if (s.getLogoImageUrl() != null)   used.add(s.getLogoImageUrl());
            if (s.getBannerImageUrl() != null) used.add(s.getBannerImageUrl());
        }

        // 🔹 Event 이미지
        for (Event e : eventRepo.findAll()) {
            if (e.getImageUrl() != null) used.add(e.getImageUrl());
        }

        // 🔹 Consultation 이미지들 (wanted/before/after/drawing)
        for (Consultation c : consultationRepo.findAll()) {
            if (c.getWantedImageUrl() != null)   used.add(c.getWantedImageUrl());
            if (c.getBeforeImageUrl() != null)   used.add(c.getBeforeImageUrl());
            if (c.getAfterImageUrl() != null)    used.add(c.getAfterImageUrl());
            if (c.getDrawingImageUrl() != null)  used.add(c.getDrawingImageUrl());
        }

        // 🔹 TODO: Designer portfolio, 고객 프로필 이미지 등 있으면 여기 추가

        return used;
    }
}
