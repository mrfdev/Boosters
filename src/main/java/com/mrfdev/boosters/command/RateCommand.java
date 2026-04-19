package com.mrfdev.boosters.command;

import com.mrfdev.boosters.model.BoosterState;
import com.mrfdev.boosters.model.BoosterType;
import com.mrfdev.boosters.service.BoosterService;
import com.mrfdev.boosters.util.BuildInfo;
import com.mrfdev.boosters.util.DurationUtil;
import com.mrfdev.boosters.util.MessageService;
import com.mrfdev.boosters.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class RateCommand implements TabExecutor {

    private static final List<String> ROOT_ARGUMENTS = List.of("start", "stop", "debug");
    private static final List<String> START_TARGETS = List.of("mcmmo", "jobs", "all");
    private static final List<String> STOP_TARGETS = List.of("mcmmo", "jobs", "all");
    private static final List<String> DEBUG_PAGES = List.of("1", "2");
    private static final List<String> COMMON_DURATIONS = List.of("15m", "30m", "1h", "2h");
    private static final List<String> COMMON_RATES = List.of("2", "2.5", "3");

    private final JavaPlugin plugin;
    private final BuildInfo buildInfo;
    private final BoosterService boosterService;
    private final MessageService messageService;

    public RateCommand(JavaPlugin plugin, BuildInfo buildInfo, BoosterService boosterService, MessageService messageService) {
        this.plugin = plugin;
        this.buildInfo = buildInfo;
        this.boosterService = boosterService;
        this.messageService = messageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!hasViewPermission(sender)) {
                messageService.prefixed(sender, "<red>You do not have permission to use this command.</red>");
                return true;
            }
            sendStatus(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("debug")) {
            return handleDebug(sender, args);
        }

        if (!hasViewPermission(sender)) {
            messageService.prefixed(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        return switch (subcommand) {
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender, args);
            default -> {
                sendCommandSynopsis(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partialMatches(args[0], ROOT_ARGUMENTS);
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase(Locale.ROOT);
            if (subcommand.equals("start")) {
                return partialMatches(args[1], START_TARGETS);
            }
            if (subcommand.equals("stop")) {
                return partialMatches(args[1], STOP_TARGETS);
            }
            if (subcommand.equals("debug")) {
                return partialMatches(args[1], DEBUG_PAGES);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return partialMatches(args[2], COMMON_DURATIONS);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("start")) {
            return partialMatches(args[3], COMMON_RATES);
        }

        return List.of();
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            messageService.prefixed(sender, "<red>You need <yellow>onemb.boosters.admin</yellow> to start boosters.</red>");
            return true;
        }

        if (args.length < 4) {
            sendStartSynopsis(sender);
            return true;
        }

        long durationMillis = DurationUtil.parseDurationMillis(args[2]);
        if (durationMillis <= 0L) {
            messageService.prefixed(sender,
                    "<red>Invalid duration.</red> <gray>Use values like <yellow>15m</yellow>, <yellow>1h</yellow>, or <yellow>1h30m</yellow>.</gray>");
            sendStartSynopsis(sender);
            return true;
        }

        double rate;
        try {
            rate = NumberUtil.parseRate(args[3]);
        } catch (NumberFormatException exception) {
            messageService.prefixed(sender,
                    "<red>Invalid rate.</red> <gray>Use numbers like <yellow>2</yellow> or <yellow>2.5</yellow>.</gray>");
            sendStartSynopsis(sender);
            return true;
        }

        if (rate <= 1.0D) {
            messageService.prefixed(sender, "<red>The rate must be greater than <yellow>1</yellow>.</red>");
            return true;
        }

        String target = args[1].toLowerCase(Locale.ROOT);
        if (target.equals("all")) {
            return handleStartAll(sender, durationMillis, rate);
        }

        Optional<BoosterType> optionalType = BoosterType.fromInput(target);
        if (optionalType.isEmpty()) {
            sendStartSynopsis(sender);
            return true;
        }

        BoosterType type = optionalType.get();
        if (!boosterService.isTrackingEnabled(type)) {
            messageService.prefixed(sender,
                    "<red><booster> tracking is disabled in the config.</red>",
                    MessageService.value("booster", type.displayName()));
            return true;
        }

        if (!boosterService.isDependencyAvailable(type)) {
            messageService.prefixed(sender,
                    "<red><booster> is not installed or not enabled, so that booster cannot be started.</red>",
                    MessageService.value("booster", type.displayName()));
            return true;
        }

        BoosterState started = boosterService.startTimedBooster(type, durationMillis, rate);
        if (type == BoosterType.MCMMO) {
            messageService.prefixed(sender,
                    "<gray>Started a tracked <yellow>mcMMO</yellow> booster at <aqua><rate>x</aqua> for <aqua><duration></aqua>.</gray>",
                    MessageService.value("rate", NumberUtil.formatRate(started.rate())),
                    MessageService.value("duration", DurationUtil.formatFriendlyDuration(started.durationMillis())));
        } else {
            messageService.prefixed(sender,
                    "<gray>Started a tracked <yellow>Jobs</yellow> booster at <aqua><rate>x</aqua> for <aqua><duration></aqua>.</gray>",
                    MessageService.value("rate", NumberUtil.formatRate(started.rate())),
                    MessageService.value("duration", DurationUtil.formatFriendlyDuration(started.durationMillis())));
        }
        return true;
    }

    private boolean handleStartAll(CommandSender sender, long durationMillis, double rate) {
        List<String> startedBoosters = new ArrayList<>();
        List<String> skippedBoosters = new ArrayList<>();

        for (BoosterType type : BoosterType.values()) {
            if (!boosterService.isTrackingEnabled(type)) {
                skippedBoosters.add(type.displayName() + " (tracking disabled)");
                continue;
            }
            if (!boosterService.isDependencyAvailable(type)) {
                skippedBoosters.add(type.displayName() + " (plugin unavailable)");
                continue;
            }

            boosterService.startTimedBooster(type, durationMillis, rate);
            startedBoosters.add(type.displayName());
        }

        if (startedBoosters.isEmpty()) {
            messageService.prefixed(sender,
                    "<red>Could not start any boosters.</red> <gray><details></gray>",
                    MessageService.value("details", skippedBoosters.isEmpty()
                            ? "No supported boosters are available."
                            : "Skipped: " + String.join(", ", skippedBoosters)));
            return true;
        }

        messageService.prefixed(sender,
                "<gray>Started <yellow><boosters></yellow> at <aqua><rate>x</aqua> for <aqua><duration></aqua>.</gray>",
                MessageService.value("boosters", String.join(" and ", startedBoosters)),
                MessageService.value("rate", NumberUtil.formatRate(rate)),
                MessageService.value("duration", DurationUtil.formatFriendlyDuration(durationMillis)));

        if (!skippedBoosters.isEmpty()) {
            messageService.send(sender,
                    "<gray>Skipped: <yellow><skipped></yellow></gray>",
                    MessageService.value("skipped", String.join(", ", skippedBoosters)));
        }
        return true;
    }

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            messageService.prefixed(sender, "<red>You need <yellow>onemb.boosters.admin</yellow> to stop boosters.</red>");
            return true;
        }

        if (args.length < 2) {
            sendStopSynopsis(sender);
            return true;
        }

        String target = args[1].toLowerCase(Locale.ROOT);
        if (target.equals("all")) {
            List<BoosterType> stopped = boosterService.stopAllBoosters();
            if (stopped.isEmpty()) {
                messageService.prefixed(sender, "<gray>No tracked boosters were active.</gray>");
                return true;
            }

            String stoppedNames = stopped.stream()
                    .map(BoosterType::displayName)
                    .reduce((left, right) -> left + " and " + right)
                    .orElse("boosters");
            messageService.prefixed(sender,
                    "<gray>Stopped <yellow><boosters></yellow>.</gray>",
                    MessageService.value("boosters", stoppedNames));
            return true;
        }

        Optional<BoosterType> optionalType = BoosterType.fromInput(target);
        if (optionalType.isEmpty()) {
            sendStopSynopsis(sender);
            return true;
        }

        BoosterType type = optionalType.get();
        BoosterState stopped = boosterService.stopBooster(type);
        if (!stopped.active()) {
            messageService.prefixed(sender,
                    "<gray>There is no tracked <yellow><booster></yellow> booster to stop.</gray>",
                    MessageService.value("booster", type.displayName()));
            return true;
        }

        messageService.prefixed(sender,
                "<gray>Stopped the tracked <yellow><booster></yellow> booster.</gray>",
                MessageService.value("booster", type.displayName()));
        return true;
    }

    private void sendStatus(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Current booster status:</gray>");
        sendMcMMOStatus(sender, boosterService.getState(BoosterType.MCMMO));
        sendJobsStatus(sender, boosterService.getState(BoosterType.JOBS));
    }

    private void sendMcMMOStatus(CommandSender sender, BoosterState state) {
        if (!state.active()) {
            messageService.send(sender, "<yellow>mcMMO</yellow><gray>: </gray><red>No booster active.</red>");
            return;
        }

        if (!state.hasTrackedDuration()) {
            messageService.send(sender,
                    "<yellow>mcMMO</yellow><gray>: </gray><green>Active</green><gray> at <aqua><rate>x</aqua>. No tracked end time.</gray>",
                    MessageService.value("rate", NumberUtil.formatRate(state.rate())));
            return;
        }

        messageService.send(sender,
                "<yellow>mcMMO</yellow><gray>: </gray><green>Active</green><gray> at <aqua><rate>x</aqua> for <aqua><duration></aqua>, <aqua><remaining></aqua> left.</gray>",
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("duration", DurationUtil.formatFriendlyDuration(state.durationMillis())),
                MessageService.value("remaining", DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis()))));
    }

    private void sendJobsStatus(CommandSender sender, BoosterState state) {
        if (!state.active()) {
            messageService.send(sender, "<yellow>Jobs</yellow><gray>: </gray><red>No booster active.</red>");
            return;
        }

        String scope = state.isJobsGlobal() ? "global" : state.jobsDescriptor();
        messageService.send(sender,
                "<yellow>Jobs</yellow><gray>: </gray><green>Active</green><gray> (<aqua><scope></aqua>) at <aqua><rate>x</aqua> for <aqua><duration></aqua>, <aqua><remaining></aqua> left.</gray>",
                MessageService.value("scope", scope),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("duration", DurationUtil.formatFriendlyDuration(state.durationMillis())),
                MessageService.value("remaining", DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis()))));
    }

    private void sendCommandSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage:</gray>");
        messageService.send(sender, "<yellow>/rate</yellow><gray> - Show the current booster status.</gray>");
        messageService.send(sender,
                "<yellow>/rate start [mcmmo|jobs|all] [time] [rate]</yellow><gray> - Start tracked boosters.</gray>");
        messageService.send(sender,
                "<yellow>/rate stop [mcmmo|jobs|all]</yellow><gray> - Stop tracked boosters.</gray>");
        messageService.send(sender,
                "<yellow>/rate debug [1|2]</yellow><gray> - Show plugin diagnostics in two pages.</gray>");
    }

    private void sendStartSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage: <yellow>/rate start [mcmmo|jobs|all] [time] [rate]</yellow></gray>");
        messageService.send(sender, "<gray>Examples: <yellow>/rate start mcmmo 1h 2</yellow>, <yellow>/rate start jobs 30m 2.5</yellow>, and <yellow>/rate start all 1h 2</yellow></gray>");
    }

    private void sendStopSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage: <yellow>/rate stop [mcmmo|jobs|all]</yellow></gray>");
        messageService.send(sender, "<gray>Examples: <yellow>/rate stop mcmmo</yellow> and <yellow>/rate stop all</yellow></gray>");
    }

    private boolean hasViewPermission(CommandSender sender) {
        return sender.isOp()
                || sender.hasPermission("onemb.boosters.rate")
                || sender.hasPermission("onemb.boosters.admin")
                || sender.hasPermission("onemb.boosters.debug");
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.isOp()
                || sender.hasPermission("onemb.boosters.admin");
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!hasDebugPermission(sender)) {
            messageService.prefixed(sender, "<red>You need <yellow>onemb.boosters.debug</yellow> to use <yellow>/rate debug</yellow>.</red>");
            return true;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                sendDebugSynopsis(sender);
                return true;
            }
        }

        if (page == 1) {
            sendDebugPageOne(sender);
            return true;
        }
        if (page == 2) {
            sendDebugPageTwo(sender);
            return true;
        }

        sendDebugSynopsis(sender);
        return true;
    }

    private void sendDebugPageOne(CommandSender sender) {
        File stateFile = new File(plugin.getDataFolder(), "booster-state.yml");
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        Plugin mcMMO = Bukkit.getPluginManager().getPlugin("mcMMO");
        Plugin jobs = Bukkit.getPluginManager().getPlugin("Jobs");

        messageService.prefixed(sender, "<gray>Debug page <yellow>1/2</yellow>: build, runtime, dependencies, and tracked state.</gray>");
        messageService.send(sender,
                "<yellow>Plugin</yellow><gray>: <white><name></white> v<white><version></white> build <white><build></white></gray>",
                MessageService.value("name", buildInfo.pluginName()),
                MessageService.value("version", buildInfo.pluginVersion()),
                MessageService.value("build", buildInfo.buildNumber()));
        messageService.send(sender,
                "<yellow>Targets</yellow><gray>: Minecraft <white><mc></white>, Java <white><java></white></gray>",
                MessageService.value("mc", buildInfo.targetMinecraftVersion()),
                MessageService.value("java", buildInfo.targetJavaVersion()));
        messageService.send(sender,
                "<yellow>Artifact</yellow><gray>: <white><artifact></white></gray>",
                MessageService.value("artifact", buildInfo.artifactFileName()));
        messageService.send(sender,
                "<yellow>Runtime</yellow><gray>: Server <white><server></white>, Bukkit API <white><api></white>, Java <white><java></white></gray>",
                MessageService.value("server", Bukkit.getVersion()),
                MessageService.value("api", Bukkit.getBukkitVersion()),
                MessageService.value("java", System.getProperty("java.version", "unknown")));
        messageService.send(sender,
                "<yellow>Data folder</yellow><gray>: <white><path></white> (<white><exists></white>)</gray>",
                MessageService.value("path", plugin.getDataFolder().getAbsolutePath()),
                MessageService.value("exists", plugin.getDataFolder().exists() ? "exists" : "missing"));
        messageService.send(sender,
                "<yellow>State file</yellow><gray>: <white><path></white> (<white><exists></white>)</gray>",
                MessageService.value("path", stateFile.getAbsolutePath()),
                MessageService.value("exists", stateFile.exists() ? "exists" : "missing"));
        messageService.send(sender,
                "<yellow>Restore delay</yellow><gray>: <white><delay></white> ticks</gray>",
                MessageService.value("delay", String.valueOf(Math.max(1L, plugin.getConfig().getLong("restore.delayTicks", 60L)))));
        messageService.send(sender,
                "<yellow>PlaceholderAPI</yellow><gray>: <white><status></white></gray>",
                MessageService.value("status", pluginStatus(placeholderApi)));
        messageService.send(sender,
                "<yellow>mcMMO plugin</yellow><gray>: <white><status></white></gray>",
                MessageService.value("status", pluginStatus(mcMMO)));
        messageService.send(sender,
                "<yellow>Jobs plugin</yellow><gray>: <white><status></white></gray>",
                MessageService.value("status", pluginStatus(jobs)));

        sendMcMMODebug(sender, boosterService.getState(BoosterType.MCMMO));
        sendJobsDebug(sender, boosterService.getState(BoosterType.JOBS));
        messageService.send(sender, "<gray>Use <yellow>/rate debug 2</yellow> for commands, permissions, and placeholders.</gray>");
    }

    private void sendDebugPageTwo(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Debug page <yellow>2/2</yellow>: commands, permissions, and placeholders.</gray>");
        messageService.send(sender, "<yellow>Commands</yellow><gray>:</gray>");
        messageService.send(sender, "<white>/rate</white><gray> - Show the current booster status.</gray>");
        messageService.send(sender, "<white>/rate start [mcmmo|jobs|all] [time] [rate]</white><gray> - Start tracked boosters.</gray>");
        messageService.send(sender, "<white>/rate stop [mcmmo|jobs|all]</white><gray> - Stop tracked boosters.</gray>");
        messageService.send(sender, "<white>/rate debug [1|2]</white><gray> - Show plugin diagnostics.</gray>");

        messageService.send(sender, "<yellow>Permission nodes</yellow><gray>:</gray>");
        messageService.send(sender, "<white>onemb.boosters.rate</white><gray> - Allows players to use <yellow>/rate</yellow>.</gray>");
        messageService.send(sender, "<white>onemb.boosters.admin</white><gray> - Allows staff to use <yellow>/rate start</yellow> and <yellow>/rate stop</yellow>.</gray>");
        messageService.send(sender, "<white>onemb.boosters.debug</white><gray> - Allows staff to use <yellow>/rate debug</yellow>.</gray>");

        messageService.send(sender, "<yellow>PlaceholderAPI placeholders</yellow><gray>:</gray>");
        messageService.send(sender, "<white>%onemb_boosters_mcmmo_active%</white><gray> - Returns <yellow>Yes</yellow> or <yellow>No</yellow>.</gray>");
        messageService.send(sender, "<white>%onemb_boosters_mcmmo_rate%</white><gray> - Returns the tracked mcMMO rate, such as <yellow>2</yellow> or <yellow>2.5</yellow>.</gray>");
        messageService.send(sender, "<white>%onemb_boosters_mcmmo_time%</white><gray> - Returns the original tracked mcMMO duration, or <yellow>Manual</yellow> for a direct native <yellow>/xprate</yellow>.</gray>");
        messageService.send(sender, "<white>%onemb_boosters_mcmmo_timeleft%</white><gray> - Returns the remaining tracked mcMMO time, or <yellow>Manual</yellow> for a direct native <yellow>/xprate</yellow>.</gray>");
        messageService.send(sender, "<white>%onemb_boosters_jobs_active%</white><gray> - Returns <yellow>Yes</yellow> or <yellow>No</yellow>.</gray>");
        messageService.send(sender, "<white>%onemb_boosters_jobs_rate%</white><gray> - Returns the tracked Jobs rate, such as <yellow>2</yellow> or <yellow>2.5</yellow>.</gray>");
        messageService.send(sender, "<white>%onemb_boosters_jobs_time%</white><gray> - Returns the original tracked Jobs duration.</gray>");
        messageService.send(sender, "<white>%onemb_boosters_jobs_timeleft%</white><gray> - Returns the remaining tracked Jobs time.</gray>");
        messageService.send(sender, "<gray>Inactive boosters return <yellow>1</yellow> for rate and <yellow>None</yellow> for time values.</gray>");
        messageService.send(sender, "<gray>Use <yellow>/rate debug 1</yellow> for build, runtime, dependencies, and tracked state.</gray>");
    }

    private void sendDebugSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage: <yellow>/rate debug [1|2]</yellow></gray>");
        messageService.send(sender, "<gray><yellow>/rate debug</yellow> opens page 1. <yellow>/rate debug 2</yellow> opens the commands, permissions, and placeholders page.</gray>");
    }

    private void sendMcMMODebug(CommandSender sender, BoosterState state) {
        messageService.send(sender,
                "<yellow>mcMMO tracking</yellow><gray>: enabled=<white><tracking></white>, dependency=<white><dependency></white>, active=<white><active></white>, mode=<white><mode></white>, rate=<white><rate>x</white>, announce=<white><announce></white></gray>",
                MessageService.value("tracking", String.valueOf(boosterService.isTrackingEnabled(BoosterType.MCMMO))),
                MessageService.value("dependency", String.valueOf(boosterService.isDependencyAvailable(BoosterType.MCMMO))),
                MessageService.value("active", String.valueOf(state.active())),
                MessageService.value("mode", state.active() ? (state.hasTrackedDuration() ? "timed" : "manual") : "inactive"),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("announce", String.valueOf(state.announceOnStart())));
        messageService.send(sender,
                "<yellow>mcMMO times</yellow><gray>: started=<white><started></white>, duration=<white><duration></white>, ends=<white><ends></white>, remaining=<white><remaining></white></gray>",
                MessageService.value("started", formatTimestamp(state.startedAtEpochMillis())),
                MessageService.value("duration", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.durationMillis()) : "n/a"),
                MessageService.value("ends", formatTimestamp(state.endsAtEpochMillis())),
                MessageService.value("remaining", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis())) : "n/a"));
    }

    private void sendJobsDebug(CommandSender sender, BoosterState state) {
        messageService.send(sender,
                "<yellow>Jobs tracking</yellow><gray>: enabled=<white><tracking></white>, dependency=<white><dependency></white>, active=<white><active></white>, target=<white><target></white>, scope=<white><scope></white>, rate=<white><rate>x</white></gray>",
                MessageService.value("tracking", String.valueOf(boosterService.isTrackingEnabled(BoosterType.JOBS))),
                MessageService.value("dependency", String.valueOf(boosterService.isDependencyAvailable(BoosterType.JOBS))),
                MessageService.value("active", String.valueOf(state.active())),
                MessageService.value("target", state.jobsTarget().isBlank() ? "n/a" : state.jobsTarget()),
                MessageService.value("scope", state.jobsScope().isBlank() ? "n/a" : state.jobsScope()),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())));
        messageService.send(sender,
                "<yellow>Jobs times</yellow><gray>: started=<white><started></white>, duration=<white><duration></white>, ends=<white><ends></white>, remaining=<white><remaining></white></gray>",
                MessageService.value("started", formatTimestamp(state.startedAtEpochMillis())),
                MessageService.value("duration", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.durationMillis()) : "n/a"),
                MessageService.value("ends", formatTimestamp(state.endsAtEpochMillis())),
                MessageService.value("remaining", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis())) : "n/a"));
    }

    private String pluginStatus(Plugin pluginDependency) {
        if (pluginDependency == null) {
            return "not installed";
        }
        return pluginDependency.isEnabled()
                ? "enabled (" + pluginDependency.getDescription().getVersion() + ")"
                : "installed but disabled";
    }

    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0L) {
            return "n/a";
        }
        return Instant.ofEpochMilli(epochMillis).toString();
    }

    private boolean hasDebugPermission(CommandSender sender) {
        return sender.isOp()
                || sender.hasPermission("onemb.boosters.debug")
                || sender.hasPermission("onemb.boosters.admin");
    }

    private List<String> partialMatches(String token, List<String> options) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, matches);
        return matches;
    }
}
