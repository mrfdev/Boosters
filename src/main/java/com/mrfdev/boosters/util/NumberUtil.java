package com.mrfdev.boosters.util;

import java.math.BigDecimal;

public final class NumberUtil {

    private NumberUtil() {
    }

    public static String formatRate(double rate) {
        return BigDecimal.valueOf(rate).stripTrailingZeros().toPlainString();
    }

    public static double parseRate(String input) {
        return Double.parseDouble(input.trim().replace(',', '.'));
    }

    public static boolean isWholeNumber(double value) {
        return Math.floor(value) == value;
    }
}
