package com.mrfdev.boosters.model;

public record BoosterActionResult(
        boolean success,
        BoosterState state,
        String message
) {

    public static BoosterActionResult success(BoosterState state, String message) {
        return new BoosterActionResult(true, state, message);
    }

    public static BoosterActionResult failure(String message) {
        return new BoosterActionResult(false, null, message);
    }
}
