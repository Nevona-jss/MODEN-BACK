package com.moden.modenapi.modules.payment.service;


import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.payment.model.Payment;
import com.moden.modenapi.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService extends BaseService<Payment> {

    private final PaymentRepository paymentRepository;

    @Override
    protected PaymentRepository getRepository() {
        return paymentRepository;
    }

}
