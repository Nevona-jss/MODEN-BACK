package com.moden.modenapi.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Weekday {
    MON(0), TUE(1), WED(2), THU(3), FRI(4), SAT(5), SUN(6);

    private final int code;
    Weekday(int code) { this.code = code; }

    @JsonValue
    public int getCode() { return code; }

    public static Weekday fromCode(int code) {
        for (var w : values()) if (w.code == code) return w;
        throw new IllegalArgumentException("Invalid weekday: " + code);
    }

    // Allow JSON like: 0 / 3 / 6
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Weekday fromJson(int code) {
        return fromCode(code);
    }
}
