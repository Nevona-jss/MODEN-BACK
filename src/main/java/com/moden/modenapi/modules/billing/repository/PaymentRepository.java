package com.moden.modenapi.modules.billing.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.moden.modenapi.modules.billing.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {}
