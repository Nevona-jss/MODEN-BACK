package com.moden.modenapi.modules.designer.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.model.DesignerPortfolioItem;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerPortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DesignerPortfolioService extends BaseService<DesignerPortfolioItem> {

    private final DesignerPortfolioItemRepository itemRepo;
    private final DesignerDetailRepository designerRepo;

    @Override
    protected DesignerPortfolioItemRepository getRepository() {
        return itemRepo;
    }

    // ==========================
    // Helpers
    // ==========================
    private DesignerDetail getDesigner(UUID designerId) {
        return designerRepo.findActiveById(designerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Designer not found or deleted"));
    }

    /** Remove nulls, preserve insertion order, remove duplicates */
    private List<UUID> normalizeUniqueOrder(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        LinkedHashSet<UUID> set = new LinkedHashSet<>();
        for (UUID id : ids) {
            if (id != null) set.add(id);
        }
        return new ArrayList<>(set);
    }

    private List<DesignerPortfolioItem> orderByIdList(List<DesignerPortfolioItem> items, List<UUID> idOrder) {
        Map<UUID, DesignerPortfolioItem> map = items.stream()
                .collect(Collectors.toMap(DesignerPortfolioItem::getId, it -> it));
        List<DesignerPortfolioItem> ordered = new ArrayList<>(idOrder.size());
        for (UUID id : idOrder) {
            DesignerPortfolioItem it = map.get(id);
            if (it != null) ordered.add(it);
        }
        return ordered;
    }

    private void assertAllItemsExist(List<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return;
        List<DesignerPortfolioItem> existing = itemRepo.findAllByIdIn(itemIds);
        Set<UUID> ok = existing.stream().map(DesignerPortfolioItem::getId).collect(Collectors.toSet());
        for (UUID id : itemIds) {
            if (!ok.contains(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Portfolio item not found: " + id);
            }
        }
    }

    // ==========================
    // Commands
    // ==========================

    /** Create one item and attach to designer’s list tail */
    public DesignerPortfolioItem createAndAttach(UUID designerId, DesignerPortfolioItem toCreate) {
        DesignerDetail d = getDesigner(designerId);

        DesignerPortfolioItem saved = create(toCreate); // BaseService#create

        List<UUID> ids = d.getPortfolioItemIds();
        if (ids == null) ids = new ArrayList<>();
        ids.add(saved.getId());
        d.setPortfolioItemIds(normalizeUniqueOrder(ids));
        designerRepo.save(d);

        return saved;
    }

    /** Bulk create multiple items and append to designer’s portfolio */
    public List<DesignerPortfolioItem> bulkCreateAndAttach(UUID designerId, List<DesignerPortfolioItem> items) {
        DesignerDetail d = getDesigner(designerId);

        List<DesignerPortfolioItem> saved = new ArrayList<>();
        if (items != null) {
            for (DesignerPortfolioItem it : items) {
                saved.add(create(it));
            }
        }

        List<UUID> ids = d.getPortfolioItemIds();
        if (ids == null) ids = new ArrayList<>();
        for (DesignerPortfolioItem it : saved) ids.add(it.getId());
        d.setPortfolioItemIds(normalizeUniqueOrder(ids));
        designerRepo.save(d);

        return saved;
    }

    /** Replace whole portfolio with provided item IDs (order respected) */
    public void setPortfolio(UUID designerId, List<UUID> itemIds) {
        DesignerDetail d = getDesigner(designerId);

        List<UUID> normalized = normalizeUniqueOrder(itemIds);
        assertAllItemsExist(normalized);

        d.setPortfolioItemIds(normalized);
        designerRepo.save(d);
    }

    /** Append existing item IDs to tail (no duplicates) */
    public void addItems(UUID designerId, List<UUID> itemIds) {
        DesignerDetail d = getDesigner(designerId);

        List<UUID> base = d.getPortfolioItemIds();
        if (base == null) base = new ArrayList<>();
        if (itemIds != null) base.addAll(itemIds);

        d.setPortfolioItemIds(normalizeUniqueOrder(base));
        designerRepo.save(d);
    }

    /** Remove single item reference from designer; optionally soft-delete the item */
    public void removeItem(UUID designerId, UUID itemId, boolean alsoDeleteItem) {
        DesignerDetail d = getDesigner(designerId);

        List<UUID> base = d.getPortfolioItemIds();
        if (base == null || base.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No portfolio items attached");
        }

        boolean changed = base.removeIf(id -> id.equals(itemId));
        if (!changed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not attached to this designer");
        }
        d.setPortfolioItemIds(base);
        designerRepo.save(d);

        if (alsoDeleteItem) {
            DesignerPortfolioItem item = itemRepo.findById(itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
            item.setDeletedAt(java.time.Instant.now());
            update(item); // BaseService#update
        }
    }

    public void replaceWithUrls(UUID designerId, List<String> urls) {
        DesignerDetail d = getDesigner(designerId);

        // 1) 기존 포트폴리오 soft-delete (선택)
        List<UUID> oldIds = d.getPortfolioItemIds();
        if (oldIds != null && !oldIds.isEmpty()) {
            List<DesignerPortfolioItem> oldItems = itemRepo.findAllByIdIn(oldIds);
            for (DesignerPortfolioItem it : oldItems) {
                it.setDeletedAt(java.time.Instant.now());
                update(it); // BaseService#update
            }
        }

        // 2) 새 아이템 생성
        List<DesignerPortfolioItem> newItems = new ArrayList<>();
        if (urls != null) {
            for (String url : urls) {
                if (url == null || url.isBlank()) continue;

                DesignerPortfolioItem item = DesignerPortfolioItem.builder()
                        .designerId(designerId)   // ✅ 꼭 넣어줘야 함 (NOT NULL)
                        .imageUrl(url)
                        .caption(null)            // 필요하면 캡션 추가
                        .build();

                newItems.add(create(item)); // BaseService#create
            }
        }

        // 3) DesignerDetail.portfolioItemIds 갱신
        List<UUID> newIds = newItems.stream()
                .map(DesignerPortfolioItem::getId)
                .collect(Collectors.toList());

        d.setPortfolioItemIds(normalizeUniqueOrder(newIds));
        designerRepo.save(d);
    }



    /** Reorder with new ID order (must contain same IDs as current) */
    public void reorder(UUID designerId, List<UUID> newOrder) {
        DesignerDetail d = getDesigner(designerId);

        List<UUID> current = d.getPortfolioItemIds();
        if (current == null) current = Collections.emptyList();
        List<UUID> normalized = normalizeUniqueOrder(newOrder);

        if (current.size() != normalized.size()
                || !new HashSet<>(current).equals(new HashSet<>(normalized))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reorder must contain the same item IDs as the current portfolio");
        }

        d.setPortfolioItemIds(new ArrayList<>(normalized));
        designerRepo.save(d);
    }

    // ==========================
    // Queries
    // ==========================

    /** Get ordered portfolio for designer (soft-delete aware) */
    @Transactional(readOnly = true)
    public List<DesignerPortfolioItem> getPortfolio(UUID designerId) {
        DesignerDetail d = getDesigner(designerId);
        List<UUID> ids = d.getPortfolioItemIds();
        if (ids == null || ids.isEmpty()) return Collections.emptyList();

        List<DesignerPortfolioItem> items = itemRepo.findAllByIdIn(ids).stream()
                .filter(i -> i.getDeletedAt() == null)
                .collect(Collectors.toList());

        return orderByIdList(items, ids);
    }
}
