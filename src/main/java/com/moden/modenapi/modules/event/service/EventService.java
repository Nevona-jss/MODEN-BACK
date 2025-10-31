package com.moden.modenapi.modules.event.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.event.dto.*;
import com.moden.modenapi.modules.event.model.Event;
import com.moden.modenapi.modules.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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
                .type(req.type())
                .imageUrl(req.imageUrl())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .build();

        create(event);
        return mapToRes(event);
    }

    // 🔹 UPDATE
    public EventRes updateEvent(UUID eventId, EventUpdateReq req) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (req.title() != null) event.setTitle(req.title());
        if (req.description() != null) event.setDescription(req.description());
        if (req.type() != null) event.setType(req.type());
        if (req.imageUrl() != null) event.setImageUrl(req.imageUrl());
        if (req.startDate() != null) event.setStartDate(req.startDate());
        if (req.endDate() != null) event.setEndDate(req.endDate());

        event.setUpdatedAt(Instant.now());
        update(event);
        return mapToRes(event);
    }

    // 🔹 DELETE
    public void deleteEvent(UUID eventId) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        event.setDeletedAt(Instant.now());
        update(event);
    }

    // 🔹 GET ONE
    @Transactional(readOnly = true)
    public EventRes getEvent(UUID eventId) {
        Event event = eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return mapToRes(event);
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
                e.getType(),
                e.getImageUrl(),
                e.getStartDate(),
                e.getEndDate(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
