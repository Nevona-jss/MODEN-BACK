package com.moden.modenapi.modules.admin;

import com.moden.modenapi.modules.auth.dto.UserResponse;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.booking.dto.ReservationRes;
import com.moden.modenapi.modules.booking.repository.ReservationRepository;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ✅ AdminService
 * Provides system-wide access for ADMIN / SUPER_ADMIN.
 * Fetches users, reservations, and hair studios.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final HairStudioRepository hairStudioRepository;

    /**
     * 🔹 Get all registered users.
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getName(),
                        u.getPhone(),
                        u.getEmail(),
                        u.getUserType(),
                        u.getGender(),
                        u.getBirthdate(),
                        u.isConsentMarketing()
                ))
                .toList();
    }

    /**
     * 🔹 Get all reservations.
     */
    public List<ReservationRes> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(r -> new ReservationRes(
                        r.getId(),
                        r.getCustomer() != null ? r.getCustomer().getId() : null,
                        r.getDesigner() != null ? r.getDesigner().getId() : null,
                        r.getService() != null ? r.getService().getId() : null,
                        r.getStudio() != null ? r.getStudio().getId() : null,
                        r.getStatus(),
                        r.getReservedAt(),
                        r.getExternalRef()
                ))
                .toList();
    }

    /**
     * 🔹 Get all hair studios.
     */
    public List<StudioRes> getAllStudios() {
        return hairStudioRepository.findAll().stream()
                .map(s -> new StudioRes(
                        s.getId(),
                        s.getIdForLogin(),
                        s.getName(),
                        s.getBusinessNo(),
                        s.getOwner(),
                        s.getOwnerPhone(),
                        s.getStudioPhone(),
                        s.getAddress(),
                        s.getLogo(),
                        s.getInstagram(),
                        s.getNaver()
                ))
                .toList();
    }
}
