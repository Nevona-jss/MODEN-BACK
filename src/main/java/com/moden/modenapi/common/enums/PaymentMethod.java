package com.moden.modenapi.common.enums;

public enum PaymentMethod {

    CASH(1),
    CARD(2),
    NAVER_PAY(3),
    KAKAO_PAY(4),
    POINT(5),
    COUPON(6);

    private final int code;

    PaymentMethod(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    // Raqamdan enum qaytaruvchi metod
    public static PaymentMethod fromCode(int code) {
        for (PaymentMethod pm : values()) {
            if (pm.code == code) {
                return pm;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
