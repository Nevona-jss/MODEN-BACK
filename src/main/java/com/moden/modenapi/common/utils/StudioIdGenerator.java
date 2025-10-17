package com.moden.modenapi.common.utils;

public class StudioIdGenerator {
    public static String generateId(String name) {
        String clean = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (clean.length() > 6) clean = clean.substring(0, 6);
        long suffix = System.currentTimeMillis() % 100000;
        return "ST-" + clean + "-" + suffix;
    }
}
