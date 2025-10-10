package com.moden.modenapi.modules.admin;

import com.moden.modenapi.modules.auth.dto.UserResponse;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.booking.dto.ReservationRes;
import com.moden.modenapi.modules.booking.model.Reservation;
import com.moden.modenapi.modules.booking.repository.ReservationRepository;
import com.moden.modenapi.modules.studio.dto.StudioRes;
import com.moden.modenapi.modules.studio.model.HairStudio;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service providing administrative operations.
 * Accessible only to ADMIN or SUPER_ADMIN users.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final HairStudioRepository hairStudioRepository;

    /**
     * 🔹 Returns all registered users across roles.
     */
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream()
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
     * 🔹 Returns all reservations in the system.
     */
    public List<ReservationRes> getAllReservations() {
        List<Reservation> reservations = reservationRepository.findAll();

        return reservations.stream()
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
     * 🔹 Returns all hair studios in the system.
     */
    public List<StudioRes> getAllStudios() {
        List<HairStudio> studios = hairStudioRepository.findAll();

        return studios.stream()
                .map(s -> new StudioRes(
                        s.getId(),
                        s.getName(),
                        s.getQrCodeUrl(),
                        s.getBusinessNo(),
                        s.getAddress(),
                        s.getPhone()
                ))
                .toList();
    }
}
