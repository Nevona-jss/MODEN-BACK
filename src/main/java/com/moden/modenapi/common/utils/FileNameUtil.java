package com.moden.modenapi.common.utils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class FileNameUtil {
    private FileNameUtil() {}

    // originalName + epochSecond + rand
    public static String generate(String originalFilename, Instant now) {
        String ext = "";
        String base = "file";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalFilename.substring(dot).toLowerCase(Locale.ROOT); // incl '.'
                base = sanitize(originalFilename.substring(0, dot));
            } else {
                base = sanitize(originalFilename);
            }
        }
        String stamp = String.valueOf(now.getEpochSecond());
        String rnd = Integer.toHexString((int)(Math.random() * 0xFFFF));
        return base + "_" + stamp + "_" + rnd + ext;
    }

    // Folder partition (yyyy/MM/dd)
    public static String datePartition(Instant now) {
        var z = now.atZone(java.time.ZoneOffset.UTC);
        return DateTimeFormatter.ofPattern("yyyy/MM/dd").format(z);
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) return "file";
        // faqat xavfsiz belgilar
        return s.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
