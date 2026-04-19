package com.mrfdev.boosters.model;

import java.util.Locale;
import java.util.Optional;

public enum BoosterType {
    MCMMO("mcmmo", "mcMMO", "mcMMO"),
    JOBS("jobs", "Jobs", "Jobs");

    private final String key;
    private final String displayName;
    private final String dependencyName;

    BoosterType(String key, String displayName, String dependencyName) {
        this.key = key;
        this.displayName = displayName;
        this.dependencyName = dependencyName;
    }

    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    public String dependencyName() {
        return dependencyName;
    }

    public static Optional<BoosterType> fromInput(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String normalized = input.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "mcmmo" -> Optional.of(MCMMO);
            case "jobs" -> Optional.of(JOBS);
            default -> Optional.empty();
        };
    }
}
