package com.moden.modenapi.common.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * HTML sanitizatsiya (XSS himoya) uchun util.
 */
public final class HtmlSanitizerUtil {

    private HtmlSanitizerUtil() {
        // util class, instance kerak emas
    }

    /**
     * 개인정보/보안 안내 kabi rich HTML matnlarni xavfsiz formatga tozalash.
     * script, event handler (onclick, onload ...) va xavfli teglar uchirib tashlanadi.
     */
    public static String sanitizePrivacyHtml(String rawHtml) {
        if (rawHtml == null) return null;

        Safelist safelist = Safelist.relaxed()
                .addTags("p", "br", "ul", "ol", "li", "strong", "b", "em", "i", "u", "span", "div")
                .addAttributes("a", "href", "title")
                .addProtocols("a", "href", "http", "https", "mailto");

        return Jsoup.clean(rawHtml, safelist);
    }
}
