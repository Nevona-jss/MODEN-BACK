package com.moden.modenapi.modules.customer.dto;

import java.util.List;

public record CustomerListPageRes(
        int totalCount,
        int limit,
        int page,
        List<CustomerResponseForList> data
) {}
