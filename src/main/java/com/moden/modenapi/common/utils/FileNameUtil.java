package com.moden.modenapi.common.utils;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

public final class FileNameUtil {

    private FileNameUtil() {
    }

    /**
     * Takrorlanmas file nom generatsiya qiladi.
     *  - time (millis) + random 8char + original extension
     *  - misol: 20251117123045123_ab12cd34.png
     */
    public static String generate(String originalFilename, Instant now) {
        String ext = extractExtension(originalFilename); // ".png" yoki bo‘sh string

        // time-based prefix (yyyyMMddHHmmssSSS)
        ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
        String timePart = String.format(
                "%04d%02d%02d%02d%02d%02d%03d",
                zdt.getYear(),
                zdt.getMonthValue(),
                zdt.getDayOfMonth(),
                zdt.getHour(),
                zdt.getMinute(),
                zdt.getSecond(),
                zdt.getNano() / 1_000_000
        );

        // random 8 char (UUID’dan)
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);

        return timePart + "_" + randomPart + ext;
    }

    /**
     * Sana bo‘yicha folder structure: yyyy/MM/dd
     *  - misol: "2025/11/17"
     */
    public static String datePartition(Instant now) {
        ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
        return String.format(
                "%04d/%02d/%02d",
                zdt.getYear(),
                zdt.getMonthValue(),
                zdt.getDayOfMonth()
        );
    }

    /**
     * Original filename dan extension ajratib olamiz (".png", ".jpg" va hokazo).
     * Agar bo‘lmasa, bo‘sh string qaytaradi.
     */
    private static String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "";
        }
        String name = Paths.get(originalFilename).getFileName().toString();
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx == -1 || dotIdx == name.length() - 1) {
            return "";
        }
        return name.substring(dotIdx).toLowerCase(); // ".png"
    }
}
