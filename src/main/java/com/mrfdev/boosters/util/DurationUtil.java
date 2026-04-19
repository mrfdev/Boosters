package com.mrfdev.boosters.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationUtil {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(\\d+)(d|h|m|s)");

    private DurationUtil() {
    }

    public static long parseDurationMillis(String input) {
        if (input == null || input.isBlank()) {
            return 0L;
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        long totalSeconds = 0L;
        int cursor = 0;
        int matches = 0;

        while (matcher.find()) {
            if (matcher.start() != cursor) {
                return 0L;
            }

            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase(Locale.ROOT);
            switch (unit) {
                case "d" -> totalSeconds += value * 86400L;
                case "h" -> totalSeconds += value * 3600L;
                case "m" -> totalSeconds += value * 60L;
                case "s" -> totalSeconds += value;
                default -> {
                    return 0L;
                }
            }

            cursor = matcher.end();
            matches++;
        }

        if (matches == 0 || cursor != normalized.length()) {
            return 0L;
        }

        return totalSeconds * 1000L;
    }

    public static String formatFriendlyDuration(long millis) {
        long seconds = toDisplaySeconds(millis);
        if (seconds <= 0L) {
            return "0s";
        }

        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        List<String> parts = new ArrayList<>();
        if (days > 0L) {
            parts.add(days + "d");
        }
        if (hours > 0L) {
            parts.add(hours + "h");
        }
        if (minutes > 0L) {
            parts.add(minutes + "m");
        }
        if (seconds > 0L) {
            parts.add(seconds + "s");
        }

        if (parts.isEmpty()) {
            return "0s";
        }

        if (parts.size() > 2) {
            return parts.get(0) + " " + parts.get(1);
        }

        return String.join(" ", parts);
    }

    public static String formatCompactDuration(long millis) {
        long seconds = toDisplaySeconds(millis);
        if (seconds <= 0L) {
            return "1s";
        }

        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;

        StringBuilder builder = new StringBuilder();
        if (days > 0L) {
            builder.append(days).append('d');
        }
        if (hours > 0L) {
            builder.append(hours).append('h');
        }
        if (minutes > 0L) {
            builder.append(minutes).append('m');
        }
        if (seconds > 0L || builder.length() == 0) {
            builder.append(seconds).append('s');
        }
        return builder.toString();
    }

    private static long toDisplaySeconds(long millis) {
        if (millis <= 0L) {
            return 0L;
        }
        return Math.max(1L, (millis + 999L) / 1000L);
    }
}
