package com.moden.modenapi.common.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String format(Instant instant) {
        if (instant == null) return null;
        return ISO.withZone(ZoneId.systemDefault()).format(instant);
    }

    public static Instant now() {
        return Instant.now();
    }
}
