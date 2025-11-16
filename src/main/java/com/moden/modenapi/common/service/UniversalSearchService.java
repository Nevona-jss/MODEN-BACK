package com.moden.modenapi.common.service;

import com.moden.modenapi.common.dto.UniversalSearchItemRes;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.product.model.StudioProduct;
import com.moden.modenapi.modules.product.repository.StudioProductRepository;
import com.moden.modenapi.modules.studioservice.model.StudioService;
import com.moden.modenapi.modules.studioservice.repository.StudioServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversalSearchService {

    private final StudioServiceRepository studioServiceRepository;
    private final StudioProductRepository studioProductRepository;
    private final UserRepository userRepository;                 // ✅ qo‘shildi
    private final DesignerDetailRepository designerDetailRepository; // ✅ qo‘shildi

    public List<UniversalSearchItemRes> searchForStudio(UUID studioId, String keyword) {

        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String kw = keyword.trim();

        List<UniversalSearchItemRes> result = new ArrayList<>();

        // 1) SERVICE – studioId bo‘yicha filtr
        var services = studioServiceRepository.searchByStudioIdAndAfterService(studioId, kw);
        for (StudioService s : services) {
            result.add(new UniversalSearchItemRes(
                    "SERVICE",
                    s.getId(),
                    s.getServiceType() != null ? s.getServiceType().name() : null,
                    s.getAfterService(),
                    null
            ));
        }

        // 2) PRODUCT – studioId bo‘yicha filtr
        var byName  = studioProductRepository.findByStudioIdAndProductNameContainingIgnoreCase(studioId, kw);
        var byNotes = studioProductRepository.findByStudioIdAndNotesContainingIgnoreCase(studioId, kw);

        Map<UUID, StudioProduct> productMap = new LinkedHashMap<>();
        byName.forEach(p -> productMap.put(p.getId(), p));
        byNotes.forEach(p -> productMap.putIfAbsent(p.getId(), p));

        for (StudioProduct p : productMap.values()) {
            result.add(new UniversalSearchItemRes(
                    "PRODUCT",
                    p.getId(),
                    p.getProductName(),
                    p.getNotes(),
                    null
            ));
        }

        // 3) CUSTOMER – hozircha umumiy (studio filter yo‘q),
        //    faqat ism/telefon bo‘yicha qidiramiz
        var customers = userRepository.searchCustomers(kw);   // ✅ studioId param yo‘q
        for (User c : customers) {
            result.add(new UniversalSearchItemRes(
                    "CUSTOMER",
                    c.getId(),
                    c.getFullName(),     // title
                    c.getPhone(),        // subtitle
                    null
            ));
        }

        // 4) DESIGNER – DesignerDetailRepository ichidagi @Query-ni ishlatamiz
        var designers = designerDetailRepository.searchDesigners(kw); // ✅ studioId param yo‘q
        for (User d : designers) {
            result.add(new UniversalSearchItemRes(
                    "DESIGNER",
                    d.getId(),
                    d.getFullName(),     // title
                    null,                // hozircha subtitle yo‘q
                    null
            ));
        }

        return result;
    }
}
