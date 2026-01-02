package com.mrfdev.boosters;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Boosters extends JavaPlugin implements Listener {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(?i)(\\d+)(d|h|m|s)");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        long delay = Math.max(1L, getConfig().getLong("restore.delayTicks", 40));
        Bukkit.getScheduler().runTaskLater(this, this::restoreBoosters, delay);
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    private void restoreBoosters() {
        // mcMMO
        if (getConfig().getBoolean("mcmmo.enabled", true) && Bukkit.getPluginManager().getPlugin("mcMMO") != null) {
            double rate = getConfig().getDouble("mcmmo.activeRate", 1.0);
            if (rate > 1.0) {
                dispatchConsole("xprate " + stripTrailingZeros(rate) + " " + getConfig().getBoolean("mcmmo.activeAnnounce", false));
                getLogger().info("Restored mcMMO xprate to " + rate + "x");
            }
        }

        // Jobs
        if (getConfig().getBoolean("jobs.enabled", true) && Bukkit.getPluginManager().getPlugin("Jobs") != null) {
            long endAt = getConfig().getLong("jobs.stored.endAtEpochMillis", 0L);
            double multiplier = getConfig().getDouble("jobs.stored.multiplier", 1.0);
            if (endAt > 0 && multiplier > 1.0) {
                long remainingMillis = endAt - System.currentTimeMillis();
                long remainingSeconds = remainingMillis / 1000L;
                if (remainingSeconds > 0) {
                    String duration = formatDuration(remainingSeconds);
                    // We store only the base part so we can rewrite the duration safely on restart
                    String base = getConfig().getString("jobs.stored.commandBase", "jobs boost all exp");
                    dispatchConsole(base + " " + duration + " " + stripTrailingZeros(multiplier));
                    getLogger().info("Restored Jobs boost: " + base + " " + duration + " " + multiplier);
                } else {
                    // expired
                    clearJobsStored();
                }
            }
        }
    }

    private void dispatchConsole(String commandNoSlash) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandNoSlash);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        handleCommand(event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        handleCommand("/" + event.getCommand());
    }

    private void handleCommand(String raw) {
        if (raw == null || raw.isEmpty()) return;
        String msg = raw.startsWith("/") ? raw.substring(1) : raw;
        String lower = msg.toLowerCase(Locale.ROOT).trim();

        if (getConfig().getBoolean("mcmmo.enabled", true) && (lower.startsWith("xprate") || lower.startsWith("mcxprate"))) {
            handleMcMMO(lower);
            return;
        }

        if (getConfig().getBoolean("jobs.enabled", true) && (lower.startsWith("jobs ") || lower.equals("jobs"))) {
            handleJobs(lower);
        }
    }

    private void handleMcMMO(String cmd) {
        // Syntax (mcMMO 2.x): /xprate <rate> <true|false>  OR /xprate reset
        // We store both the multiplier and the boolean flag so we can replay the exact command after restart.
        String[] parts = cmd.split("\\s+");
        if (parts.length < 2) return;

        String arg = parts[1].trim();
        if (arg.equalsIgnoreCase("reset")) {
            getConfig().set("mcmmo.activeRate", 1.0);
            getConfig().set("mcmmo.activeAnnounce", false);
            saveConfig();
            return;
        }

        try {
            double rate = Double.parseDouble(arg);
            boolean announce = false;

            if (parts.length >= 3) {
                String boolArg = parts[2].trim().toLowerCase(Locale.ROOT);
                if (boolArg.equals("true") || boolArg.equals("false")) {
                    announce = Boolean.parseBoolean(boolArg);
                }
            } else {
                // keep previous stored value if command omits the boolean
                announce = getConfig().getBoolean("mcmmo.activeAnnounce", false);
            }

            if (rate <= 1.0) {
                getConfig().set("mcmmo.activeRate", 1.0);
            } else {
                getConfig().set("mcmmo.activeRate", rate);
            }
            getConfig().set("mcmmo.activeAnnounce", announce);
            saveConfig();
        } catch (NumberFormatException ignored) {
        }
    }

    private void handleJobs(String cmd) {
        // We care about: jobs boost <job|all> <exp|money|points|all> <duration> <multiplier>
        // Example: jobs boost all exp 1h10m20s 2
        String[] parts = cmd.split("\\s+");
        if (parts.length < 2) return;
        if (!parts[1].equalsIgnoreCase("boost")) return;
        if (parts.length < 6) return;

        String target = parts[2];
        String type = parts[3];
        String durationRaw = parts[4];
        String multRaw = parts[5];

        long durationSeconds = parseDurationSeconds(durationRaw);
        double multiplier;
        try {
            multiplier = Double.parseDouble(multRaw);
        } catch (NumberFormatException e) {
            return;
        }

        // If reset/invalid or multiplier <= 1, treat as clear
        if (durationSeconds <= 0 || multiplier <= 1.0) {
            clearJobsStored();
            return;
        }

        // Store a "base" command without duration/multiplier so we can safely rewrite duration after restart.
        String base = "jobs boost " + target + " " + type;
        getConfig().set("jobs.stored.commandBase", base);
        getConfig().set("jobs.stored.multiplier", multiplier);
        getConfig().set("jobs.stored.endAtEpochMillis", System.currentTimeMillis() + (durationSeconds * 1000L));
        saveConfig();
    }

    private void clearJobsStored() {
        getConfig().set("jobs.stored.commandBase", "jobs boost all exp");
        getConfig().set("jobs.stored.multiplier", 1.0);
        getConfig().set("jobs.stored.endAtEpochMillis", 0L);
        saveConfig();
    }

    private static long parseDurationSeconds(String input) {
        if (input == null) return 0;
        String s = input.trim();
        Matcher m = DURATION_PATTERN.matcher(s);
        long total = 0;
        int matches = 0;
        while (m.find()) {
            matches++;
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2).toLowerCase(Locale.ROOT);
            switch (unit) {
                case "d" -> total += value * 86400L;
                case "h" -> total += value * 3600L;
                case "m" -> total += value * 60L;
                case "s" -> total += value;
            }
        }
        return matches == 0 ? 0 : total;
    }

    private static String formatDuration(long seconds) {
        if (seconds <= 0) return "1s";
        long d = seconds / 86400L; seconds %= 86400L;
        long h = seconds / 3600L; seconds %= 3600L;
        long m = seconds / 60L; seconds %= 60L;
        long s = seconds;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append('d');
        if (h > 0) sb.append(h).append('h');
        if (m > 0) sb.append(m).append('m');
        if (s > 0 || sb.length() == 0) sb.append(s).append('s');
        return sb.toString();
    }

    private static String stripTrailingZeros(double d) {
        String s = Double.toString(d);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("rate")) return false;

        double mcmmoRate = getConfig().getDouble("mcmmo.activeRate", 1.0);
        long endAt = getConfig().getLong("jobs.stored.endAtEpochMillis", 0L);
        double mult = getConfig().getDouble("jobs.stored.multiplier", 1.0);
        String base = getConfig().getString("jobs.stored.commandBase", "jobs boost all exp");

        sender.sendMessage(ChatColor.GOLD + "[Boosters] " + ChatColor.RESET + "Stored booster status:");
        sender.sendMessage(ChatColor.YELLOW + "mcMMO xprate: " + ChatColor.WHITE + stripTrailingZeros(mcmmoRate) + "x");

        if (endAt > 0 && mult > 1.0) {
            long remainingSec = Math.max(0, (endAt - System.currentTimeMillis()) / 1000L);
            if (remainingSec > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Jobs boost: " + ChatColor.WHITE + base + " " + formatDuration(remainingSec) + " " + stripTrailingZeros(mult));
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Jobs boost: " + ChatColor.GRAY + "expired");
            }
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Jobs boost: " + ChatColor.GRAY + "none stored");
        }
        return true;
    }
}
