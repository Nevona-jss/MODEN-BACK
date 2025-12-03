package com.moden.modenapi.modules.studio.dto;

/**
 * Studio birthday coupon settings update request (record-based DTO)
 */
public record StudioBirthdayCouponRequest(
        boolean birthdayCouponEnabled,
        String birthdayCouponDescription
) {
}
