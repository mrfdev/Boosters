package com.mrfdev.boosters.listener;

import com.mrfdev.boosters.service.BoosterService;
import com.mrfdev.boosters.util.DurationUtil;
import com.mrfdev.boosters.util.NumberUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;

public final class ExternalCommandListener implements Listener {

    private final BoosterService boosterService;

    public ExternalCommandListener(BoosterService boosterService) {
        this.boosterService = boosterService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        handleRawCommand(event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        handleRawCommand('/' + event.getCommand());
    }

    private void handleRawCommand(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        String normalized = raw.startsWith("/") ? raw.substring(1) : raw;
        String[] parts = normalized.trim().split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String root = parts[0].toLowerCase(Locale.ROOT);
        if (root.equals("xprate") || root.equals("mcxprate")) {
            trackMcMMO(parts);
            return;
        }

        if (root.equals("jobs")) {
            trackJobs(parts);
        }
    }

    private void trackMcMMO(String[] parts) {
        if (parts.length < 2) {
            return;
        }

        String action = parts[1].toLowerCase(Locale.ROOT);
        if (action.equals("reset")) {
            boosterService.recordExternalMcMMOReset();
            return;
        }

        double rate;
        try {
            rate = NumberUtil.parseRate(parts[1]);
        } catch (NumberFormatException exception) {
            return;
        }

        boolean announce = boosterService.getMcMMODefaultAnnounceOnStart();
        if (parts.length >= 3) {
            String announceToken = parts[2].toLowerCase(Locale.ROOT);
            if (announceToken.equals("true") || announceToken.equals("false")) {
                announce = Boolean.parseBoolean(announceToken);
            }
        }

        boosterService.recordExternalMcMMOBoost(rate, announce);
    }

    private void trackJobs(String[] parts) {
        if (parts.length < 3 || !parts[1].equalsIgnoreCase("boost")) {
            return;
        }

        if (parts.length >= 4 && parts[2].equalsIgnoreCase("all") && parts[3].equalsIgnoreCase("reset")) {
            boosterService.recordExternalJobsReset();
            return;
        }

        if (parts.length < 6) {
            return;
        }

        String target = parts[2];
        String scope = parts[3];
        long durationMillis = DurationUtil.parseDurationMillis(parts[4]);
        if (durationMillis <= 0L) {
            return;
        }

        double rate;
        try {
            rate = NumberUtil.parseRate(parts[5]);
        } catch (NumberFormatException exception) {
            return;
        }

        boosterService.recordExternalJobsBoost(target, scope, durationMillis, rate);
    }
}
