package com.moden.modenapi.modules.event.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.event.dto.EventCreateReq;
import com.moden.modenapi.modules.event.dto.EventRes;
import com.moden.modenapi.modules.event.dto.EventUpdateReq;
import com.moden.modenapi.modules.event.model.Event;
import com.moden.modenapi.modules.event.repository.EventRepository;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService extends BaseService<Event> {

    private final EventRepository eventRepository;
    private final DesignerDetailRepository designerDetailRepository;
    private final HairStudioDetailRepository studioDetailRepository;
    private final CustomerDetailRepository customerDetailRepository;

    @Override
    protected EventRepository getRepository() {
        return eventRepository;
    }

    // ========= CREATE (faqat DTO qaytaradi) =========
    public EventRes createEvent(UUID studioId, EventCreateReq req) {
        Event event = Event.builder()
                .studioId(studioId)              // studioUserId
                .title(req.title())
                .description(req.description())
                .imageUrl(req.imageUrl())
                .discount(req.discount())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();

        create(event);                           // BaseService<Event> ichki CRUD
        return mapToRes(event);                  // ✅ tashqariga faqat DTO
    }

    // ========= GET ONE (studio 기준) =========
    @Transactional(readOnly = true)
    public EventRes getEventForStudio(UUID studioId, UUID eventId) {
        Event event = getEventAndCheckStudio(studioId, eventId);
        return mapToRes(event);                  // ✅ DTO
    }

    // ========= UPDATE (studio 기준) =========
    public EventRes updateEventForStudio(UUID studioId, UUID eventId, EventUpdateReq req) {
        Event event = getEventAndCheckStudio(studioId, eventId);

        if (req.title() != null)       event.setTitle(req.title());
        if (req.description() != null) event.setDescription(req.description());
        if (req.imageUrl() != null)    event.setImageUrl(req.imageUrl());
        if (req.discount() != null)    event.setDiscount(req.discount());
        if (req.startDate() != null)   event.setStartDate(req.startDate());
        if (req.endDate() != null)     event.setEndDate(req.endDate());

        event.setUpdatedAt(Instant.now());
        update(event);
        return mapToRes(event);              // ✅ DTO
    }

    // ========= DELETE (studio 기준) =========
    public void deleteEventForStudio(UUID studioId, UUID eventId) {
        Event event = getEventAndCheckStudio(studioId, eventId);
        event.setDeletedAt(Instant.now());
        update(event);                       // ✅ void, Event qaytmaydi
    }

    // ========= LIST + FILTER (studio 기준) =========
    @Transactional(readOnly = true)
    public List<EventRes> searchForStudio(
            UUID studioId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        List<Event> baseList = eventRepository.findAllActiveByStudioId(studioId);

        return baseList.stream()
                .filter(e -> {
                    // keyword filter
                    if (keyword != null && !keyword.isBlank()) {
                        String k = keyword.toLowerCase();
                        String title = e.getTitle() != null ? e.getTitle().toLowerCase() : "";
                        String desc  = e.getDescription() != null ? e.getDescription().toLowerCase() : "";
                        if (!title.contains(k) && !desc.contains(k)) return false;
                    }
                    // date range filter
                    if (fromDate != null) {
                        if (e.getEndDate() != null && e.getEndDate().isBefore(fromDate)) return false;
                    }
                    if (toDate != null) {
                        if (e.getStartDate() != null && e.getStartDate().isAfter(toDate)) return false;
                    }
                    return true;
                })
                .map(this::mapToRes)              // ✅ har doim DTO
                .collect(Collectors.toList());
    }

    // ========= GET ALL by studio =========
    @Transactional(readOnly = true)
    public List<EventRes> getAllByStudio(UUID studioId) {
        return eventRepository.findAllActiveByStudioId(studioId)
                .stream()
                .map(this::mapToRes)              // ✅ DTO
                .collect(Collectors.toList());
    }

    // ========= studioId(studioUserId) resolve (studio/designer/customer 공용) =========
    //  - HAIR_STUDIO: 토큰 userId
    //  - DESIGNER   : DesignerDetail.hairStudioId (studioUserId)
    //  - CUSTOMER   : CustomerDetail.studioId (studioUserId)
    public UUID resolveStudioIdForCurrentActor(UUID currentUserId) {
        if (hasRole("HAIR_STUDIO")) {
            studioDetailRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for current user")
                    );
            return currentUserId;   // ✅ studioUserId
        }

        if (hasRole("DESIGNER")) {
            var dd = designerDetailRepository.findByUserIdAndDeletedAtIsNull(currentUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Designer profile not found")
                    );

            UUID studioUserId = dd.getHairStudioId();   // ❗ bu value-ni userId sifatida saqlayapsan

            studioDetailRepository.findByUserIdAndDeletedAtIsNull(studioUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for designer")
                    );

            return studioUserId;
        }

        if (hasRole("CUSTOMER")) {
            var cd = customerDetailRepository.findActiveByUserId(currentUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer profile not found")
                    );

            UUID studioUserId = cd.getStudioId();   // ✅ studioUserId

            studioDetailRepository.findByUserIdAndDeletedAtIsNull(studioUserId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Studio not found for this customer")
                    );

            return studioUserId;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only studio, designer or customer can access events");
    }

    // ========= ichki helperlar =========

    private Event getEventAndCheckStudio(UUID studioId, UUID eventId) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!event.getStudioId().equals(studioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Event does not belong to current studio");
        }
        return event;
    }

    private EventRes mapToRes(Event e) {
        return new EventRes(
                e.getId(),
                e.getStudioId(),
                e.getTitle(),
                e.getDescription(),
                e.getImageUrl(),
                e.getDiscount(),
                e.getStartDate(),
                e.getEndDate(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private boolean hasRole(String role) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        final String target = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(target::equals);
    }
}
