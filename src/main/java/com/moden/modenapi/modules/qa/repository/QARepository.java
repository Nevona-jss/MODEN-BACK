package com.moden.modenapi.modules.qa.repository;

import com.moden.modenapi.common.enums.QAStatus;
import com.moden.modenapi.common.repository.BaseRepository;
import com.moden.modenapi.modules.qa.model.QA;

import java.util.List;
import java.util.UUID;

public interface QARepository extends BaseRepository<QA, UUID> {

    List<QA> findByUserIdOrderByCreatedAtDesc(UUID userId);


}
