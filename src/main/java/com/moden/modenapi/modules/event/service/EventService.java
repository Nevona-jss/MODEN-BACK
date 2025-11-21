package com.moden.modenapi.modules.event.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.event.dto.EventCreateReq;
import com.moden.modenapi.modules.event.dto.EventRes;
import com.moden.modenapi.modules.event.dto.EventUpdateReq;
import com.moden.modenapi.modules.event.model.Event;
import com.moden.modenapi.modules.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @Override
    protected EventRepository getRepository() {
        return eventRepository;
    }

    // 🔹 CREATE
    public EventRes createEvent(UUID studioId, EventCreateReq req) {
        Event event = Event.builder()
                .studioId(studioId)
                .title(req.title())
                .description(req.description())
                .imageUrl(req.imageUrl())
                .discount(req.discount())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();

        create(event);
        return mapToRes(event);
    }

    // 🔹 내부 helper: 해당 studio 의 event 맞는지 체크
    private Event getEventAndCheckStudio(UUID studioId, UUID eventId) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!event.getStudioId().equals(studioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Event does not belong to current studio");
        }
        return event;
    }

    // 🔹 GET ONE (studio 기준)
    @Transactional(readOnly = true)
    public EventRes getEventForStudio(UUID studioId, UUID eventId) {
        Event event = getEventAndCheckStudio(studioId, eventId);
        return mapToRes(event);
    }

    // 🔹 UPDATE (studio 기준)
    public EventRes updateEventForStudio(UUID studioId, UUID eventId, EventUpdateReq req) {
        Event event = getEventAndCheckStudio(studioId, eventId);

        if (req.title() != null)      event.setTitle(req.title());
        if (req.description() != null)event.setDescription(req.description());
        if (req.imageUrl() != null)   event.setImageUrl(req.imageUrl());
        if (req.discount() != null)   event.setDiscount(req.discount());
        if (req.startDate() != null)  event.setStartDate(req.startDate());
        if (req.endDate() != null)    event.setEndDate(req.endDate());

        event.setUpdatedAt(Instant.now());
        update(event);
        return mapToRes(event);
    }

    // 🔹 DELETE (studio 기준)
    public void deleteEventForStudio(UUID studioId, UUID eventId) {
        Event event = getEventAndCheckStudio(studioId, eventId);
        event.setDeletedAt(Instant.now());
        update(event);
    }
    // ============================================================
    // LIST + FILTER for current studio
    //  - keyword: title / description 에 포함 여부
    //  - fromDate / toDate: 이벤트 기간이 이 날짜 구간과 겹치는지 여부
    //  - DB 쿼리 단순화를 위해 우선 studioId 기준 active 이벤트 가져온 뒤, 메모리에서 필터
    //    (나중에 필요하면 QueryDSL/Specification 등으로 최적화)
    // ============================================================
    @Transactional(readOnly = true)
    public List<EventRes> searchForStudio(
            UUID studioId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        // 기존 getAllByStudio(studioId)를 대체
        List<Event> baseList = eventRepository.findAllActiveByStudioId(studioId);

        return baseList.stream()
                .filter(e -> {
                    // 1) keyword filter (title + description)
                    if (keyword != null && !keyword.isBlank()) {
                        String k = keyword.toLowerCase();
                        String title = e.getTitle() != null ? e.getTitle().toLowerCase() : "";
                        String desc = e.getDescription() != null ? e.getDescription().toLowerCase() : "";
                        if (!title.contains(k) && !desc.contains(k)) {
                            return false;
                        }
                    }

                    // 2) date filter (기간이 [fromDate, toDate] 와 겹치는지)
                    if (fromDate != null) {
                        // event의 endDate가 fromDate보다 이전이면 제외
                        if (e.getEndDate() != null && e.getEndDate().isBefore(fromDate)) {
                            return false;
                        }
                    }
                    if (toDate != null) {
                        // event의 startDate가 toDate보다 이후면 제외
                        if (e.getStartDate() != null && e.getStartDate().isAfter(toDate)) {
                            return false;
                        }
                    }

                    return true;
                })
                .map(this::mapToRes)
                .collect(Collectors.toList());
    }

    // 🔹 GET ALL by Studio
    @Transactional(readOnly = true)
    public List<EventRes> getAllByStudio(UUID studioId) {
        return eventRepository.findAllActiveByStudioId(studioId)
                .stream()
                .map(this::mapToRes)
                .collect(Collectors.toList());
    }

    // 🔹 Mapper
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
}
