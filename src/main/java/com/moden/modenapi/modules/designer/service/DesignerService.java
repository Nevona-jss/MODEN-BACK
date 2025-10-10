package com.moden.modenapi.modules.designer.service;


import com.moden.modenapi.modules.designer.dto.DesignerDto;
import com.moden.modenapi.modules.designer.dto.DesignerProfileResponse;
import com.moden.modenapi.modules.designer.dto.ReservationSummaryRes;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.model.HairStudio;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import com.moden.modenapi.modules.booking.repository.ReservationRepository; // ✅ correct path
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for handling Designer related operations.
 * <p>
 * Handles:
 * <ul>
 *     <li>Designer profile CRUD</li>
 *     <li>Listing all designers under a Hair Studio</li>
 *     <li>Fetching designer reservations and statistics</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DesignerService {

    private final DesignerDetailRepository designerRepository;
    private final HairStudioRepository hairStudioRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Registers a new designer under a given salon.
     *
     * @param dto DTO containing designer info
     * @return saved designer detail
     */
    public DesignerDto create(DesignerDto dto) {
        HairStudio salon = hairStudioRepository.findById(dto.salonId())
                .orElseThrow(() -> new RuntimeException("Hair studio not found"));

        var designer = DesignerDetail.builder()
                .userId(dto.userId())
                .hairStudio(salon)
                .portfolioUrl(dto.portfolioUrl())
                .bio(dto.bio())
                .createdAt(Instant.now())
                .build();

        designerRepository.save(designer);
        return mapToDto(designer);
    }

    /**
     * Updates an existing designer profile.
     *
     * @param designerId designer user UUID
     * @param dto        updated data
     * @return updated profile
     */
    public DesignerDto update(UUID designerId, DesignerDto dto) {
        DesignerDetail designer = designerRepository.findById(designerId)
                .orElseThrow(() -> new RuntimeException("Designer not found"));

        designer.setBio(dto.bio());
        designer.setPortfolioUrl(dto.portfolioUrl());
        designer.setUpdatedAt(Instant.now());

        designerRepository.save(designer);
        return mapToDto(designer);
    }

    /**
     * Fetches designer profile by userId.
     *
     * @param designerId designer UUID
     * @return profile info
     */
    @Transactional(readOnly = true)
    public DesignerDto get(UUID designerId) {
        DesignerDetail designer = designerRepository.findById(designerId)
                .orElseThrow(() -> new RuntimeException("Designer not found"));
        return mapToDto(designer);
    }

    /**
     * Lists all designers belonging to a given hair studio.
     *
     * @param salonId hair studio UUID
     * @return list of designer DTOs
     */
    @Transactional(readOnly = true)
    public List<DesignerDto> listBySalon(UUID salonId) {
        return designerRepository.findAllByHairStudio_Id(salonId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Deletes a designer (soft delete can be added later).
     *
     * @param designerId UUID
     */
    public void delete(UUID designerId) {
        if (!designerRepository.existsById(designerId)) {
            throw new RuntimeException("Designer not found");
        }
        designerRepository.deleteById(designerId);
    }

    /**
     * Maps entity to DTO.
     */
    private DesignerDto mapToDto(DesignerDetail d) {
        return new DesignerDto(
                d.getUserId(),
                d.getBio(),
                d.getPortfolioUrl(),
                d.getHairStudio().getId(),
                d.getUser().getId()
        );
    }

    @Transactional(readOnly = true)
    public DesignerProfileResponse getProfile(UUID designerId) {
        var designer = designerRepository.findById(designerId)
                .orElseThrow(() -> new RuntimeException("Designer not found"));
        var studio = designer.getHairStudio();
        return new DesignerProfileResponse(
                designer.getUserId(),
                designer.getBio(),
                designer.getPortfolioUrl(),
                designer.getPhonePublic(),
                studio != null ? studio.getId() : null,
                studio != null ? studio.getName() : null
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationSummaryRes> getReservations(UUID designerId) {
        var reservations = reservationRepository.findAllByDesigner_Id(designerId);
        return reservations.stream()
                .map(r -> new ReservationSummaryRes(
                        r.getId(),
                        r.getCustomer().getId(),
                        r.getCustomer().getName(),
                        r.getStatus(),
                        r.getReservedAt()
                ))
                .toList();
    }


}
