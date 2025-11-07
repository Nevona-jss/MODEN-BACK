package com.moden.modenapi.common.utils;

import java.security.SecureRandom;

public class IdGenerator {
    private static final SecureRandom RND = new SecureRandom();

    /** Legacy: Studio ID (ST-...) */
    public static String generateId(String name) {
        return generateWithPrefix("ST", name);
    }

    /** Studio ID (ST-...) */
    public static String generateStudioId(String name) {
        return generateWithPrefix("ST", name);
    }

    /** Designer ID (DS-...) */
    public static String generateDesignerId(String seed) {
        return generateWithPrefix("DS", seed);
    }

    /** Generalized generator: <PREFIX>-<SEED>-<6digits> */
    public static String generateWithPrefix(String prefix, String seed) {
        String clean = seed == null ? "" : seed.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (clean.length() > 6) clean = clean.substring(0, 6);
        if (clean.isEmpty()) clean = prefix.equalsIgnoreCase("ST") ? "STUDIO" : "USER";
        int suffix = 100000 + RND.nextInt(900000); // 6 raqam
        return prefix + "-" + clean + "-" + suffix;
    }
}
