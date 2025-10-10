package com.moden.modenapi.modules.content.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.content.model.Content;
import com.moden.modenapi.modules.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Manages all app-level content (articles, banners, guides, etc.).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ContentService extends BaseService<Content, UUID> {

    private final ContentRepository repo;

    @Override
    protected JpaRepository<Content, UUID> getRepository() {
        return repo;
    }

}
