package com.moden.modenapi.modules.consult.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.consult.model.Consultation;
import com.moden.modenapi.modules.consult.repository.ConsultationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

/**
 * Handles consultations between customers and designers.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ConsultationService extends BaseService<Consultation, UUID> {

    private final ConsultationRepository repo;

    @Override
    protected JpaRepository<Consultation, UUID> getRepository() {
        return repo;
    }

    @Transactional(readOnly = true)
    public List<Consultation> listByCustomer(UUID customerId) {
        return repo.findAll().stream()
                .filter(c -> c.getReservation() != null &&
                        c.getReservation().getCustomer() != null &&
                        c.getReservation().getCustomer().getId().equals(customerId))
                .toList();
    }
}
