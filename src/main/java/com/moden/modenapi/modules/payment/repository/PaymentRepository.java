package com.moden.modenapi.modules.payment.repository;

import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.payment.model.Payment;
import com.moden.modenapi.common.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends BaseRepository<Payment, UUID> {

    Optional<Payment> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Payment> findByServiceIdAndDeletedAtIsNull(UUID serviceId);

    List<Payment> findAllByPaymentStatusAndDeletedAtIsNull(PaymentStatus paymentStatus);

    Optional<Payment> findTopByServiceIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID serviceId);
}
