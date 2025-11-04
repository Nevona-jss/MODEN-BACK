package com.moden.modenapi.common.utils;


import java.lang.reflect.Method;
import java.util.List;

public class DisplayNameUtil {
    private static final List<String> CANDIDATE_METHODS = List.of(
            "getNickName", "getNickname",
            "getDisplayName",
            "getName",
            "getStudioName", "getShopName", "getCompanyName",
            "getFullName", "getTitle"
    );

    public static String extract(Object entity) {
        if (entity == null) return null;
        for (String m : CANDIDATE_METHODS) {
            try {
                Method method = entity.getClass().getMethod(m);
                Object val = method.invoke(entity);
                if (val != null) return String.valueOf(val);
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                // optional: log
            }
        }
        return null;
    }
}
