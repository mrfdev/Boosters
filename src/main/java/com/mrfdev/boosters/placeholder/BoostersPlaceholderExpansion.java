package com.mrfdev.boosters.placeholder;

import com.mrfdev.boosters.service.BoosterService;
import com.mrfdev.boosters.model.BoosterState;
import com.mrfdev.boosters.model.BoosterType;
import com.mrfdev.boosters.util.DurationUtil;
import com.mrfdev.boosters.util.NumberUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class BoostersPlaceholderExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final BoosterService boosterService;

    public BoostersPlaceholderExpansion(JavaPlugin plugin, BoosterService boosterService) {
        this.plugin = plugin;
        this.boosterService = boosterService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "onembboosters";
    }

    @Override
    public @NotNull String getAuthor() {
        List<String> authors = plugin.getDescription().getAuthors();
        return authors.isEmpty() ? "mrfloris" : String.join(", ", authors);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.toLowerCase(Locale.ROOT).split("_", 2);
        if (parts.length != 2) {
            return null;
        }

        Optional<BoosterType> optionalType = BoosterType.fromInput(parts[0]);
        if (optionalType.isEmpty()) {
            return null;
        }

        BoosterState state = boosterService.getState(optionalType.get());
        return switch (parts[1]) {
            case "active" -> state.active() ? "Yes" : "No";
            case "rate" -> state.active() ? NumberUtil.formatRate(state.rate()) : "1";
            case "time" -> formatDurationValue(state, false);
            case "timeleft" -> formatDurationValue(state, true);
            default -> null;
        };
    }

    private String formatDurationValue(BoosterState state, boolean remaining) {
        if (!state.active()) {
            return "None";
        }

        if (!state.hasTrackedDuration()) {
            return "Manual";
        }

        long millis = remaining ? state.remainingMillis(System.currentTimeMillis()) : state.durationMillis();
        return DurationUtil.formatFriendlyDuration(millis);
    }
}
