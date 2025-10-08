package com.moden.modenapi.modules.studio.service;

import com.moden.modenapi.modules.studio.dto.*;
import com.moden.modenapi.modules.studio.model.HairStudio;
import com.moden.modenapi.modules.studio.repository.HairStudioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class HairStudioService {
    private final HairStudioRepository repo;

    public StudioRes create(StudioCreateReq req){
        var s = HairStudio.builder()
                .name(req.name()).qrCodeUrl(req.qrCodeUrl()).businessNo(req.businessNo())
                .address(req.address()).phone(req.phone()).build();
        s = repo.save(s);
        return new StudioRes(s.getId(), s.getName(), s.getQrCodeUrl(), s.getBusinessNo(), s.getAddress(), s.getPhone());
    }

    @Transactional(readOnly = true)
    public List<StudioRes> list(){
        return repo.findAll().stream()
                .map(s -> new StudioRes(s.getId(), s.getName(), s.getQrCodeUrl(), s.getBusinessNo(), s.getAddress(), s.getPhone()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudioRes get(UUID id){
        var s = repo.findById(id).orElseThrow();
        return new StudioRes(s.getId(), s.getName(), s.getQrCodeUrl(), s.getBusinessNo(), s.getAddress(), s.getPhone());
    }
}
