package com.moden.modenapi.modules.point.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.point.dto.PointLedgerRes;
import com.moden.modenapi.modules.point.model.PointLedger;
import com.moden.modenapi.modules.point.repository.PointLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages customer reward and promotion points.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PointService extends BaseService<PointLedger, UUID> {

    private final PointLedgerRepository repo;

    @Override
    protected JpaRepository<PointLedger, UUID> getRepository() {
        return repo;
    }

    /**
     * Returns total balance for a customer.
     */
    public int getBalance(UUID customerId) {
        return repo.findAll().stream()
                .filter(p -> p.getCustomerId().equals(customerId))
                .mapToInt(PointLedger::getDelta)
                .sum();
    }

    /**
     * Adds points to a customer's ledger.
     */
    public int addPoints(UUID customerId, int amount, String reason) {
        var ledger = PointLedger.builder()
                .customerId(customerId)
                .delta(amount)
                .reason(reason)
                .build();
        save(ledger);
        return getBalance(customerId);
    }

    /**
     * Deducts points from a customer's ledger.
     */
    public int deductPoints(UUID customerId, int amount, String reason) {
        var ledger = PointLedger.builder()
                .customerId(customerId)
                .delta(-amount)
                .reason(reason)
                .build();
        save(ledger);
        return getBalance(customerId);
    }

    /**
     * Returns the transaction history for a customer.
     */
    @Transactional(readOnly = true)
    public List<PointLedgerRes> getHistory(UUID customerId) {
        return getAll().stream()
                .filter(p -> p.getCustomerId().equals(customerId))
                .map(PointLedgerRes::from)
                .toList();
    }
}
