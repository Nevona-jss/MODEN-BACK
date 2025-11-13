package com.moden.modenapi.modules.studioservice.service;

import com.moden.modenapi.common.service.BaseService;
import com.moden.modenapi.modules.product.model.StudioProduct;
import com.moden.modenapi.modules.product.repository.StudioProductRepository;
import com.moden.modenapi.modules.studioservice.dto.*;
import com.moden.modenapi.modules.studioservice.model.ServiceUsedProduct;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import com.moden.modenapi.modules.studioservice.repository.ServiceUsedProductRepository;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StudioServiceService extends BaseService<StudioService> {

    private final StudioServiceRepository studioServiceRepository;
    private final ServiceUsedProductRepository serviceUsedProductRepository;
    private final StudioProductRepository studioProductRepository;

    // CREATE (bitta)
    public StudioServiceRes create(UUID studioId, StudioServiceCreateRequest req) {
        StudioService entity = StudioService.builder()
                .studioId(studioId)
                .serviceType(req.serviceType())
                .afterService(req.afterService())
                .durationMin(req.durationMin())
                .servicePrice(req.servicePrice())
                .designerTipPercent(req.designerTipPercent())
                .build();

        StudioService saved = studioServiceRepository.save(entity);

        // Service'da ishlatilgan productlarni saqlash
        syncServiceProducts(saved.getId(), req.products());

        List<ServiceUsedProduct> products = serviceUsedProductRepository.findByServiceId(saved.getId());
        return toRes(saved, products);
    }

    public StudioServiceRes update(UUID studioId, UUID serviceId, StudioServiceUpdateReq req) {
        StudioService entity = studioServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("StudioService not found: " + serviceId));

        if (!entity.getStudioId().equals(studioId)) {
            throw new IllegalArgumentException("You do not have permission to update this service.");
        }

        entity.setServiceType(req.serviceType());
        entity.setAfterService(req.afterService());
        entity.setDurationMin(req.durationMin());
        entity.setServicePrice(req.servicePrice());
        entity.setDesignerTipPercent(req.designerTipPercent());

        // eski productlarni tozalab, request'dan kelgan productlar bilan yangilayapti
        syncServiceProducts(entity.getId(), req.products());

        List<ServiceUsedProduct> products = serviceUsedProductRepository.findByServiceId(entity.getId());
        return toRes(entity, products);
    }


    // DELETE
    public void delete(UUID studioId, UUID serviceId) {
        StudioService entity = studioServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("StudioService not found: " + serviceId));

        if (!entity.getStudioId().equals(studioId)) {
            throw new IllegalArgumentException("You do not have permission to delete this service.");
        }

        serviceUsedProductRepository.deleteByServiceId(serviceId);
        studioServiceRepository.delete(entity);
    }

    // DETAIL
    @Transactional(readOnly = true)
    public StudioServiceRes getOne(UUID studioId, UUID serviceId) {
        StudioService entity = studioServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("StudioService not found: " + serviceId));

        if (!entity.getStudioId().equals(studioId)) {
            throw new IllegalArgumentException("You do not have permission to view this service.");
        }

        List<ServiceUsedProduct> products = serviceUsedProductRepository.findByServiceId(serviceId);
        return toRes(entity, products);
    }

    // LIST BY STUDIO
    @Transactional(readOnly = true)
    public List<StudioServiceRes> listByStudio(UUID studioId) {
        List<StudioService> services = studioServiceRepository.findByStudioId(studioId);

        return services.stream()
                .map(s -> {
                    List<ServiceUsedProduct> products = serviceUsedProductRepository.findByServiceId(s.getId());
                    return toRes(s, products);
                })
                .toList();
    }

    // ---- private helpers ----

    private void syncServiceProducts(UUID serviceId, List<ServiceUsedProductReq> productReqs) {
        // old productlarni tozalab tashlaymiz (simple strategy)
        serviceUsedProductRepository.deleteByServiceId(serviceId);

        if (productReqs == null || productReqs.isEmpty()) {
            return;
        }

        for (ServiceUsedProductReq p : productReqs) {
            BigDecimal totalPrice = p.price().multiply(BigDecimal.valueOf(p.quantity()));

            ServiceUsedProduct entity = ServiceUsedProduct.builder()
                    .serviceId(serviceId)
                    .productId(p.productId())
                    .quantity(p.quantity())
                    .price(p.price())
                    .totalPrice(totalPrice)
                    .build();

            serviceUsedProductRepository.save(entity);
        }
    }

    private StudioServiceRes toRes(StudioService service, List<ServiceUsedProduct> products) {

        List<ServiceUsedProductRes> productResList =
                (products == null ? Collections.<ServiceUsedProduct>emptyList() : products).stream()
                        .map(p -> {
                            // product nomini olish (topilmasa null yoki "UNKNOWN")
                            String productName = studioProductRepository.findById(p.getProductId())
                                    .map(StudioProduct::getProductName)
                                    .orElse(null);

                            return new ServiceUsedProductRes(
                                    p.getProductId(),
                                    productName,          // ⬅️ FE tanlash/ko‘rsatish uchun nom
                                    p.getQuantity(),
                                    p.getPrice(),
                                    p.getTotalPrice()
                            );
                        })
                        .toList();

        return new StudioServiceRes(
                service.getId(),
                service.getStudioId(),
                service.getServiceType(),
                service.getAfterService(),
                service.getDurationMin(),
                service.getServicePrice(),
                service.getDesignerTipPercent(),
                productResList,
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
    @Override
    protected JpaRepository<StudioService, UUID> getRepository() {
        return null;
    }
}
