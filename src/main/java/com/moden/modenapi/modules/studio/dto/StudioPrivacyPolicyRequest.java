package com.moden.modenapi.modules.studio.dto;

/**
 * Studio privacy policy (HTML) update request (record-based DTO)
 */
public record StudioPrivacyPolicyRequest(
        String privacyPolicyHtml   // CKEditor’dan keladigan raw HTML
) {
}
