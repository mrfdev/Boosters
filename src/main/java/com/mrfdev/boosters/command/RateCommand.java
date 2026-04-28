package com.mrfdev.boosters.command;

import com.mrfdev.boosters.Boosters;
import com.mrfdev.boosters.model.BoosterActionResult;
import com.mrfdev.boosters.model.BoosterState;
import com.mrfdev.boosters.model.BoosterType;
import com.mrfdev.boosters.service.BoosterService;
import com.mrfdev.boosters.util.BuildInfo;
import com.mrfdev.boosters.util.DurationUtil;
import com.mrfdev.boosters.util.LocaleService;
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

    private static final List<String> DEBUG_TOPICS = List.of(
            "summary", "1", "reference", "2", "commands", "permissions",
            "placeholders", "integrations", "state", "raw", "3", "config", "toggle", "all", "logs", "clean", "cleanlogs"
    );

    private final JavaPlugin plugin;
    private final BuildInfo buildInfo;
    private final BoosterService boosterService;
    private final MessageService messageService;
    private final LocaleService localeService;

    public RateCommand(JavaPlugin plugin, BuildInfo buildInfo, BoosterService boosterService,
                       MessageService messageService, LocaleService localeService) {
        this.plugin = plugin;
        this.buildInfo = buildInfo;
        this.boosterService = boosterService;
        this.messageService = messageService;
        this.localeService = localeService;
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
        return switch (subcommand) {
            case "info" -> handleInfo(sender);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender, args);
            case "reload" -> handleReload(sender);
            case "debug" -> handleDebug(sender, args);
            default -> {
                sendCommandSynopsis(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partialMatches(args[0], rootArgumentsFor(sender));
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase(Locale.ROOT);
            if (subcommand.equals("start")) {
                return hasAdminPermission(sender) ? partialMatches(args[1], startTargets()) : List.of();
            }
            if (subcommand.equals("stop")) {
                return hasAdminPermission(sender) ? partialMatches(args[1], stopTargets()) : List.of();
            }
            if (subcommand.equals("debug")) {
                return hasDebugPermission(sender) ? partialMatches(args[1], DEBUG_TOPICS) : List.of();
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return hasAdminPermission(sender) ? partialMatches(args[2], configuredDurations()) : List.of();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("start")) {
            return hasAdminPermission(sender) ? partialMatches(args[3], configuredRates()) : List.of();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("config")) {
            return hasDebugPermission(sender) ? partialMatches(args[2], configDebugPaths()) : List.of();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("toggle")) {
            return hasDebugPermission(sender) ? partialMatches(args[2], toggleableConfigPaths()) : List.of();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("toggle")) {
            return hasDebugPermission(sender) ? partialMatches(args[3], List.of("true", "false")) : List.of();
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("config")
                && toggleableConfigPaths().contains(args[2])) {
            return hasDebugPermission(sender) ? partialMatches(args[3], List.of("true", "false")) : List.of();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("clean")) {
            return hasDebugPermission(sender) ? partialMatches(args[2], List.of("logs")) : List.of();
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
        if (!isBoosterAvailableForCommand(type)) {
            messageService.prefixed(sender,
                    "<red><booster> is disabled for this server.</red>",
                    MessageService.value("booster", type.displayName()));
            return true;
        }

        if (boosterService.getState(type).active()) {
            messageService.prefixed(sender,
                    "<red>There is already a tracked <yellow><booster></yellow> booster active.</red> <gray>Stop it first with <yellow><command></yellow>.</gray>",
                    MessageService.value("booster", type.displayName()),
                    MessageService.value("command", "/rate stop " + type.key()));
            return true;
        }

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

        if (type == BoosterType.POINTS && !NumberUtil.isWholeNumber(rate)) {
            messageService.prefixed(sender, "<red>Points boosters only support whole-number multipliers.</red>");
            return true;
        }

        BoosterActionResult result = boosterService.startTimedBooster(type, durationMillis, rate, senderName(sender));
        if (!result.success()) {
            messageService.prefixed(sender, "<red><message></red>", MessageService.value("message", result.message()));
            return true;
        }

        messageService.prefixed(sender,
                "<gray>Started a tracked <yellow><booster></yellow> booster at <aqua><rate>x</aqua> for <aqua><duration></aqua>.</gray>",
                MessageService.value("booster", type.displayName()),
                MessageService.value("rate", NumberUtil.formatRate(result.state().rate())),
                MessageService.value("duration", DurationUtil.formatFriendlyDuration(result.state().durationMillis())));
        return true;
    }

    private boolean handleStartAll(CommandSender sender, long durationMillis, double rate) {
        List<String> startedBoosters = new ArrayList<>();
        List<String> skippedBoosters = new ArrayList<>();

        for (BoosterType type : BoosterType.values()) {
            if (!shouldIncludeInAll(type)) {
                continue;
            }

            if (boosterService.getState(type).active()) {
                skippedBoosters.add(type.displayName() + " (already active, stop it first)");
                continue;
            }
            if (!boosterService.isTrackingEnabled(type)) {
                skippedBoosters.add(type.displayName() + " (tracking disabled)");
                continue;
            }
            if (!boosterService.isDependencyAvailable(type)) {
                skippedBoosters.add(type.displayName() + " (plugin unavailable)");
                continue;
            }
            if (type == BoosterType.POINTS && !NumberUtil.isWholeNumber(rate)) {
                skippedBoosters.add(type.displayName() + " (whole-number multipliers only)");
                continue;
            }

            BoosterActionResult result = boosterService.startTimedBooster(type, durationMillis, rate, senderName(sender));
            if (result.success()) {
                startedBoosters.add(type.displayName());
            } else {
                skippedBoosters.add(type.displayName() + " (" + result.message() + ")");
            }
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
            return handleStopAll(sender);
        }

        Optional<BoosterType> optionalType = BoosterType.fromInput(target);
        if (optionalType.isEmpty()) {
            sendStopSynopsis(sender);
            return true;
        }

        BoosterType type = optionalType.get();
        BoosterActionResult result = boosterService.stopBooster(type, senderName(sender));
        if (!result.success()) {
            messageService.prefixed(sender,
                    "<gray><message></gray>",
                    MessageService.value("message", result.message()));
            return true;
        }

        messageService.prefixed(sender,
                "<gray>Stopped the tracked <yellow><booster></yellow> booster.</gray>",
                MessageService.value("booster", type.displayName()));
        return true;
    }

    private boolean handleStopAll(CommandSender sender) {
        List<String> stopped = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (BoosterType type : BoosterType.values()) {
            if (!shouldIncludeInAll(type)) {
                continue;
            }
            BoosterActionResult result = boosterService.stopBooster(type, senderName(sender));
            if (result.success()) {
                stopped.add(type.displayName());
            } else if (boosterService.getState(type).active()) {
                skipped.add(type.displayName() + " (" + result.message() + ")");
            }
        }

        if (stopped.isEmpty()) {
            messageService.prefixed(sender, "<gray>No tracked boosters were active.</gray>");
            if (!skipped.isEmpty()) {
                messageService.send(sender,
                        "<gray>Skipped: <yellow><skipped></yellow></gray>",
                        MessageService.value("skipped", String.join(", ", skipped)));
            }
            return true;
        }

        messageService.prefixed(sender,
                "<gray>Stopped <yellow><boosters></yellow>.</gray>",
                MessageService.value("boosters", String.join(" and ", stopped)));
        if (!skipped.isEmpty()) {
            messageService.send(sender,
                    "<gray>Skipped: <yellow><skipped></yellow></gray>",
                    MessageService.value("skipped", String.join(", ", skipped)));
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasAdminPermission(sender)) {
            messageService.prefixed(sender, "<red>You need <yellow>onemb.boosters.admin</yellow> to reload this plugin.</red>");
            return true;
        }

        if (plugin instanceof Boosters boosters) {
            boosters.reloadRuntimeConfiguration();
            messageService.prefixed(sender, "<gray>Reloaded this plugin's config and locale.</gray>");
            return true;
        }

        messageService.prefixed(sender, "<red>Could not reload this plugin safely.</red>");
        return true;
    }

    private void sendStatus(CommandSender sender) {
        messageService.prefixed(sender, locale("rate.header", "<gray>Current booster status:</gray>"));
        sendMcMMOStatus(sender, boosterService.getState(BoosterType.MCMMO));
        sendJobsStatus(sender, boosterService.getState(BoosterType.JOBS));
        if (shouldShowToSender(BoosterType.POINTS, sender)) {
            sendPointsStatus(sender, boosterService.getState(BoosterType.POINTS), sender);
        }
    }

    private void sendMcMMOStatus(CommandSender sender, BoosterState state) {
        if (!configSectionVisible("mcmmo")) {
            return;
        }

        String label = label(BoosterType.MCMMO);
        if (!state.active()) {
            messageService.send(sender, label + "<gray>: </gray>" + locale("rate.inactive", "<red>No booster active.</red>"));
            return;
        }

        if (!state.hasTrackedDuration()) {
            messageService.send(sender, label + "<gray>: </gray>" + locale("rate.active-no-end",
                            "<green>Active</green><gray> at <aqua><rate>x</aqua>. No tracked end time.</gray>"),
                    MessageService.value("rate", NumberUtil.formatRate(state.rate())));
            return;
        }

        messageService.send(sender, label + "<gray>: </gray>" + locale("rate.active-timed",
                        "<green>Active</green><gray> at <aqua><rate>x</aqua> for <aqua><duration></aqua>, <aqua><remaining></aqua> left.</gray>"),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("duration", DurationUtil.formatFriendlyDuration(state.durationMillis())),
                MessageService.value("remaining", DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis()))));
    }

    private void sendJobsStatus(CommandSender sender, BoosterState state) {
        if (!configSectionVisible("jobs")) {
            return;
        }

        String label = label(BoosterType.JOBS);
        if (!state.active()) {
            messageService.send(sender, label + "<gray>: </gray>" + locale("rate.inactive", "<red>No booster active.</red>"));
            return;
        }

        String scope = state.isJobsGlobal() ? "global" : state.jobsDescriptor();
        messageService.send(sender, label + "<gray>: </gray>" + locale("rate.jobs-active-timed",
                        "<green>Active</green><gray> (<aqua><scope></aqua>) at <aqua><rate>x</aqua> for <aqua><duration></aqua>, <aqua><remaining></aqua> left.</gray>"),
                MessageService.value("scope", scope),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("duration", DurationUtil.formatFriendlyDuration(state.durationMillis())),
                MessageService.value("remaining", DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis()))));
    }

    private void sendPointsStatus(CommandSender sender, BoosterState state, CommandSender originalSender) {
        String label = label(BoosterType.POINTS);
        boolean hiddenExperimental = !boosterService.isFeatureVisible(BoosterType.POINTS) && hasAdminPermission(originalSender);
        if (!state.active()) {
            String suffix = hiddenExperimental ? " " + locale("rate.hidden-experimental", "<gray>Hidden experimental integration. Admin-only view.</gray>") : "";
            messageService.send(sender, label + "<gray>: </gray>" + locale("rate.inactive", "<red>No booster active.</red>") + suffix);
            return;
        }

        messageService.send(sender, label + "<gray>: </gray>" + locale("rate.points-active-timed",
                        "<green>Active</green><gray> at <aqua><rate>x</aqua> for <aqua><duration></aqua>, <aqua><remaining></aqua> left. Current values: <aqua><ingame></aqua> in-game, <aqua><discord></aqua> Discord.</gray>"),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("duration", DurationUtil.formatFriendlyDuration(state.durationMillis())),
                MessageService.value("remaining", DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis()))),
                MessageService.value("ingame", String.valueOf(boosterService.getPointsCurrentIngamePoints())),
                MessageService.value("discord", String.valueOf(boosterService.getPointsCurrentDiscordPoints())));
    }

    private void sendCommandSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage:</gray>");
        if (hasViewPermission(sender)) {
            messageService.send(sender, "<yellow>/rate</yellow><gray> - Show the current booster status.</gray>");
            messageService.send(sender, "<yellow>/rate info</yellow><gray> - Learn what this plugin does and view the GitHub URL.</gray>");
        }
        if (hasAdminPermission(sender)) {
            messageService.send(sender,
                    "<yellow>/rate start [mcmmo|jobs|points|all] [time] [rate]</yellow><gray> - Start tracked boosters.</gray>");
            messageService.send(sender,
                    "<yellow>/rate stop [mcmmo|jobs|points|all]</yellow><gray> - Stop tracked boosters.</gray>");
            messageService.send(sender,
                    "<yellow>/rate reload</yellow><gray> - Reload this plugin's config and locale.</gray>");
        }
        if (hasDebugPermission(sender)) {
            messageService.send(sender,
                    "<yellow>/rate debug [summary|reference|raw|placeholders|permissions|config|logs]</yellow><gray> - Show diagnostics.</gray>");
        }
    }

    private boolean handleInfo(CommandSender sender) {
        if (!hasViewPermission(sender)) {
            messageService.prefixed(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        messageService.prefixed(sender, "<gray>About <yellow>1MB-Boosters</yellow>:</gray>");
        messageService.send(sender,
                "<gray>This plugin tracks and restores <yellow>mcMMO</yellow>, <yellow>Jobs</yellow>, and optional <yellow>Points</yellow> boosters for 1MoreBlock.</gray>");
        messageService.send(sender,
                "<gray>Use <yellow>/rate</yellow> to view booster status. Staff can use <yellow>/rate start</yellow>, <yellow>/rate stop</yellow>, <yellow>/rate reload</yellow>, and <yellow>/rate debug</yellow>.</gray>");
        messageService.send(sender,
                "<gray>Build: <yellow><version></yellow> build <yellow><build></yellow><gray>, compiled for Paper API <yellow><paper></yellow> with compatibility floor <yellow><floor></yellow>.</gray>",
                MessageService.value("version", buildInfo.pluginVersion()),
                MessageService.value("build", buildInfo.buildNumber()),
                MessageService.value("paper", buildInfo.compilePaperApiVersion()),
                MessageService.value("floor", buildInfo.declaredApiCompatibilityVersion()));
        messageService.send(sender,
                "<click:open_url:'https://github.com/mrfdev/Boosters'><hover:show_text:'<gray>Open the GitHub repository</gray>'><aqua>https://github.com/mrfdev/Boosters</aqua></hover></click>");
        return true;
    }

    private void sendStartSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage: <yellow>/rate start [mcmmo|jobs|points|all] [time] [rate]</yellow></gray>");
        messageService.send(sender, "<gray>Examples: <yellow>/rate start mcmmo 1h 2</yellow>, <yellow>/rate start jobs 30m 2.5</yellow>, <yellow>/rate start points 1h 2</yellow>, and <yellow>/rate start all 1h 2</yellow></gray>");
    }

    private void sendStopSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage: <yellow>/rate stop [mcmmo|jobs|points|all]</yellow></gray>");
        messageService.send(sender, "<gray>Examples: <yellow>/rate stop mcmmo</yellow>, <yellow>/rate stop points</yellow>, and <yellow>/rate stop all</yellow></gray>");
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!hasDebugPermission(sender)) {
            messageService.prefixed(sender, "<red>You need <yellow>onemb.boosters.debug</yellow> to use <yellow>/rate debug</yellow>.</red>");
            return true;
        }

        if (args.length == 1) {
            sendDebugSummary(sender);
            return true;
        }

        String topic = args[1].toLowerCase(Locale.ROOT);
        if (topic.equals("clean") && args.length >= 3 && args[2].equalsIgnoreCase("logs")) {
            boosterService.clearRecentActions();
            messageService.prefixed(sender, "<gray>Cleared recent action logs.</gray>");
            return true;
        }

        return switch (topic) {
            case "1", "summary" -> {
                sendDebugSummary(sender);
                yield true;
            }
            case "2", "reference" -> {
                sendDebugReference(sender);
                yield true;
            }
            case "commands" -> {
                sendDebugCommands(sender);
                yield true;
            }
            case "permissions" -> {
                sendDebugPermissions(sender);
                yield true;
            }
            case "placeholders" -> {
                sendDebugPlaceholders(sender);
                yield true;
            }
            case "integrations" -> {
                sendDebugIntegrations(sender);
                yield true;
            }
            case "state" -> {
                sendDebugState(sender);
                yield true;
            }
            case "3", "raw" -> {
                sendDebugRaw(sender);
                yield true;
            }
            case "config" -> handleDebugConfig(sender, args);
            case "toggle" -> handleDebugToggle(sender, args);
            case "all" -> {
                if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                    messageService.prefixed(sender, "<red>Console only command.</red> <gray>Use <yellow>/rate debug summary</yellow>, <yellow>reference</yellow>, <yellow>integrations</yellow>, <yellow>state</yellow>, or <yellow>config</yellow> in-game.</gray>");
                    yield true;
                }
                sendDebugAll(sender);
                yield true;
            }
            case "logs" -> {
                sendDebugLogs(sender);
                yield true;
            }
            case "cleanlogs" -> {
                boosterService.clearRecentActions();
                messageService.prefixed(sender, "<gray>Cleared recent action logs.</gray>");
                yield true;
            }
            default -> {
                sendDebugSynopsis(sender);
                yield true;
            }
        };
    }

    private boolean handleDebugConfig(CommandSender sender, String[] args) {
        if (args.length == 2) {
            sendDebugConfigSummary(sender);
            return true;
        }

        String path = args[2];
        Object currentValue = plugin.getConfig().get(path);
        if (args.length == 3) {
            messageService.prefixed(sender,
                    "<gray><path></gray><gray> = <yellow><value></yellow></gray>",
                    MessageService.value("path", path),
                    MessageService.value("value", currentValue == null ? "null" : String.valueOf(currentValue)));
            return true;
        }

        String rawValue = args[3];
        plugin.getConfig().set(path, parseConfigValue(rawValue));
        plugin.saveConfig();
        if (plugin instanceof Boosters boosters) {
            boosters.reloadRuntimeConfiguration();
        }
        messageService.prefixed(sender,
                "<gray>Updated <yellow><path></yellow> to <yellow><value></yellow> and reloaded this plugin.</gray>",
                MessageService.value("path", path),
                MessageService.value("value", rawValue));
        return true;
    }

    private boolean handleDebugToggle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messageService.prefixed(sender, "<gray>Usage: <yellow>/rate debug toggle <path> [true|false]</yellow></gray>");
            messageService.send(sender, "<gray>Examples: <yellow>/rate debug toggle features.points.visible</yellow> or <yellow>/rate debug toggle features.points.visible true</yellow></gray>");
            return true;
        }

        String path = args[2];
        if (!plugin.getConfig().contains(path)) {
            messageService.prefixed(sender,
                    "<red>Config path <yellow><path></yellow> was not found.</red>",
                    MessageService.value("path", path));
            return true;
        }

        Object currentValue = plugin.getConfig().get(path);
        if (!(currentValue instanceof Boolean currentBoolean)) {
            messageService.prefixed(sender,
                    "<red><path></yellow> is not a true/false setting, so it cannot be toggled.</red>",
                    MessageService.value("path", path));
            return true;
        }

        boolean newValue = args.length >= 4 ? Boolean.parseBoolean(args[3]) : !currentBoolean;
        plugin.getConfig().set(path, newValue);
        plugin.saveConfig();
        if (plugin instanceof Boosters boosters) {
            boosters.reloadRuntimeConfiguration();
        }

        messageService.prefixed(sender,
                "<gray>Toggled <yellow><path></yellow> to <yellow><value></yellow> and reloaded this plugin.</gray>",
                MessageService.value("path", path),
                MessageService.value("value", String.valueOf(newValue)));
        return true;
    }

    private void sendDebugSummary(CommandSender sender) {
        File stateFile = new File(plugin.getDataFolder(), "booster-state.yml");
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        Plugin mcMMO = Bukkit.getPluginManager().getPlugin("mcMMO");
        Plugin jobs = Bukkit.getPluginManager().getPlugin("Jobs");
        Plugin points = Bukkit.getPluginManager().getPlugin(plugin.getConfig().getString("points.pluginName", "PyroWelcomesPro"));

        messageService.prefixed(sender, "<gray>Debug summary:</gray>");
        messageService.send(sender,
                "<yellow>Plugin</yellow><gray>: <white><name></white> v<white><version></white> build <white><build></white></gray>",
                MessageService.value("name", buildInfo.pluginName()),
                MessageService.value("version", buildInfo.pluginVersion()),
                MessageService.value("build", buildInfo.buildNumber()));
        messageService.send(sender,
                "<yellow>Build</yellow><gray>: Paper API <white><paper_api></white>, plugin.yml api-version <white><plugin_api></white>, compatibility floor <white><floor></white>, Java <white><java></white></gray>",
                MessageService.value("paper_api", buildInfo.compilePaperApiVersion()),
                MessageService.value("plugin_api", buildInfo.pluginYamlApiVersion()),
                MessageService.value("floor", buildInfo.declaredApiCompatibilityVersion()),
                MessageService.value("java", buildInfo.targetJavaVersion()));
        messageService.send(sender,
                "<yellow>Runtime</yellow><gray>: Server <white><server></white>, Bukkit API <white><api></white>, Java <white><java></white></gray>",
                MessageService.value("server", Bukkit.getVersion()),
                MessageService.value("api", Bukkit.getBukkitVersion()),
                MessageService.value("java", System.getProperty("java.version", "unknown")));
        messageService.send(sender,
                "<yellow>Locale</yellow><gray>: <white><file></white></gray>",
                MessageService.value("file", localeService.activeLocaleFile()));
        messageService.send(sender,
                "<yellow>Data folder</yellow><gray>: <white><path></white></gray>",
                MessageService.value("path", plugin.getDataFolder().getAbsolutePath()));
        messageService.send(sender,
                "<yellow>State file</yellow><gray>: <white><path></white> (<white><exists></white>)</gray>",
                MessageService.value("path", stateFile.getAbsolutePath()),
                MessageService.value("exists", stateFile.exists() ? "exists" : "missing"));
        messageService.send(sender, "<yellow>PlaceholderAPI</yellow><gray>: <white><status></white></gray>", MessageService.value("status", pluginStatus(placeholderApi)));
        messageService.send(sender, "<yellow>mcMMO plugin</yellow><gray>: <white><status></white></gray>", MessageService.value("status", pluginStatus(mcMMO)));
        messageService.send(sender, "<yellow>Jobs plugin</yellow><gray>: <white><status></white></gray>", MessageService.value("status", pluginStatus(jobs)));
        messageService.send(sender, "<yellow>Points plugin</yellow><gray>: <white><status></white></gray>", MessageService.value("status", pluginStatus(points)));
        messageService.send(sender, "<gray>Use <yellow>/rate debug reference</yellow>, <yellow>/rate debug integrations</yellow>, <yellow>/rate debug raw</yellow>, or <yellow>/rate debug config</yellow> for more detail.</gray>");
    }

    private void sendDebugReference(CommandSender sender) {
        sendDebugCommands(sender);
        sendDebugPermissions(sender);
        sendDebugPlaceholders(sender);
    }

    private void sendDebugAll(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Full debug dump:</gray>");
        sendDebugSummary(sender);
        sendDebugReference(sender);
        sendDebugIntegrations(sender);
        sendDebugState(sender);
        sendDebugRaw(sender);
        sendDebugConfigSummary(sender);
        sendDebugLogs(sender);
    }

    private void sendDebugCommands(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Debug commands reference:</gray>");
        messageService.send(sender, "<white>/rate</white><gray> - Show the current booster status.</gray>");
        messageService.send(sender, "<white>/rate info</white><gray> - Show a player-friendly plugin introduction and GitHub URL.</gray>");
        messageService.send(sender, "<white>/rate start [mcmmo|jobs|points|all] [time] [rate]</white><gray> - Start tracked boosters.</gray>");
        messageService.send(sender, "<white>/rate stop [mcmmo|jobs|points|all]</white><gray> - Stop tracked boosters.</gray>");
        messageService.send(sender, "<white>/rate reload</white><gray> - Reload this plugin's config and locale.</gray>");
        messageService.send(sender, "<white>/rate debug summary</white><gray> - Show build/runtime summary.</gray>");
        messageService.send(sender, "<white>/rate debug reference</white><gray> - Show commands, permissions, and placeholders.</gray>");
        messageService.send(sender, "<white>/rate debug integrations</white><gray> - Show integration status details.</gray>");
        messageService.send(sender, "<white>/rate debug state</white><gray> - Show tracked state details.</gray>");
        messageService.send(sender, "<white>/rate debug raw</white><gray> - Show raw state values and important file paths.</gray>");
        messageService.send(sender, "<white>/rate debug all</white><gray> - Console-only full debug dump.</gray>");
        messageService.send(sender, "<white>/rate debug config [path] [value]</white><gray> - Inspect or set config values.</gray>");
        messageService.send(sender, "<white>/rate debug toggle <path> [true|false]</white><gray> - Flip or explicitly set a true/false config setting.</gray>");
        messageService.send(sender, "<white>/rate debug logs</white><gray> - Show recent audit actions.</gray>");
        messageService.send(sender, "<white>/rate debug clean logs</white><gray> - Clear recent audit actions.</gray>");
        messageService.send(sender, "<white>/rate debug cleanlogs</white><gray> - Alias for clearing recent audit actions.</gray>");
    }

    private void sendDebugConfigSummary(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Config summary:</gray>");
        sendConfigSectionHeader(sender, "Core");
        sendConfigEntry(sender, "locale.file");
        sendConfigEntry(sender, "restore.delayTicks");

        sendConfigSectionHeader(sender, "Features");
        sendConfigEntry(sender, "features.mcmmo.enabled");
        sendConfigEntry(sender, "features.jobs.enabled");
        sendConfigEntry(sender, "features.points.enabled");
        sendConfigEntry(sender, "features.points.visible");
        sendConfigEntry(sender, "features.points.experimental");

        sendConfigSectionHeader(sender, "Display");
        sendConfigEntry(sender, "display.sections.mcmmo");
        sendConfigEntry(sender, "display.sections.jobs");
        sendConfigEntry(sender, "display.sections.points");
        sendConfigEntry(sender, "display.labels.mcmmo");
        sendConfigEntry(sender, "display.labels.jobs");
        sendConfigEntry(sender, "display.labels.points");
        sendConfigEntry(sender, "display.adminShowExperimentalIntegrations");

        sendConfigSectionHeader(sender, "Tab Completion");
        sendConfigEntry(sender, "tabCompletion.commonDurations");
        sendConfigEntry(sender, "tabCompletion.commonRates");

        sendConfigSectionHeader(sender, "Logging");
        sendConfigEntry(sender, "logging.auditToConsole");
        sendConfigEntry(sender, "logging.recentActionLimit");

        sendConfigSectionHeader(sender, "Broadcasts");
        sendConfigEntry(sender, "broadcasts.start.global");
        sendConfigEntry(sender, "broadcasts.start.mcmmo");
        sendConfigEntry(sender, "broadcasts.start.jobs");
        sendConfigEntry(sender, "broadcasts.start.points");
        sendConfigEntry(sender, "broadcasts.stop.global");
        sendConfigEntry(sender, "broadcasts.stop.mcmmo");
        sendConfigEntry(sender, "broadcasts.stop.jobs");
        sendConfigEntry(sender, "broadcasts.stop.points");

        sendConfigSectionHeader(sender, "mcMMO");
        sendConfigEntry(sender, "mcmmo.announceOnRateStart");
        sendConfigEntry(sender, "mcmmo.announceOnRestore");

        sendConfigSectionHeader(sender, "Points");
        sendConfigEntry(sender, "points.pluginName");
        sendConfigEntry(sender, "points.configFile");
        sendConfigEntry(sender, "points.reloadCommand");
        sendConfigEntry(sender, "points.ingamePath");
        sendConfigEntry(sender, "points.discordPath");
        sendConfigEntry(sender, "points.baseIngamePoints");
        sendConfigEntry(sender, "points.baseDiscordPoints");

        sendConfigSectionHeader(sender, "Command Hooks");
        sendConfigEntry(sender, "commandHooks.start.global");
        sendConfigEntry(sender, "commandHooks.start.mcmmo");
        sendConfigEntry(sender, "commandHooks.start.jobs");
        sendConfigEntry(sender, "commandHooks.start.points");
        sendConfigEntry(sender, "commandHooks.stop.global");
        sendConfigEntry(sender, "commandHooks.stop.mcmmo");
        sendConfigEntry(sender, "commandHooks.stop.jobs");
        sendConfigEntry(sender, "commandHooks.stop.points");
    }

    private void sendDebugPermissions(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Permission nodes:</gray>");
        sendClickableDebugEntry(sender, "onemb.boosters.rate", "Allows players to use /rate.", "Click to copy this permission node");
        sendClickableDebugEntry(sender, "onemb.boosters.admin", "Allows staff to use /rate start, /rate stop, and /rate reload.", "Click to copy this permission node");
        sendClickableDebugEntry(sender, "onemb.boosters.debug", "Allows staff to use /rate debug.", "Click to copy this permission node");
    }

    private void sendDebugPlaceholders(CommandSender sender) {
        messageService.prefixed(sender, "<gray>PlaceholderAPI placeholders:</gray>");
        sendClickableDebugEntry(sender, "%onembboosters_mcmmo_active%", "Returns Yes or No.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_mcmmo_rate%", "Returns the tracked mcMMO rate.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_mcmmo_time%", "Returns the original tracked mcMMO duration, or Manual for native /xprate.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_mcmmo_timeleft%", "Returns the remaining tracked mcMMO duration, or Manual for native /xprate.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_jobs_active%", "Returns Yes or No.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_jobs_rate%", "Returns the tracked Jobs rate.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_jobs_time%", "Returns the original tracked Jobs duration.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_jobs_timeleft%", "Returns the remaining tracked Jobs duration.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_points_active%", "Returns Yes or No.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_points_rate%", "Returns the tracked Points multiplier, or 1 while points are hidden.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_points_time%", "Returns the original tracked Points duration, or None while points are hidden.", "Click to copy this placeholder");
        sendClickableDebugEntry(sender, "%onembboosters_points_timeleft%", "Returns the remaining tracked Points duration, or None while points are hidden.", "Click to copy this placeholder");
    }

    private void sendDebugIntegrations(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Integration details:</gray>");
        sendMcMMODebug(sender, boosterService.getState(BoosterType.MCMMO));
        sendJobsDebug(sender, boosterService.getState(BoosterType.JOBS));
        sendPointsDebug(sender, boosterService.getState(BoosterType.POINTS));
    }

    private void sendDebugState(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Tracked state details:</gray>");
        sendMcMMODebug(sender, boosterService.getState(BoosterType.MCMMO));
        sendJobsDebug(sender, boosterService.getState(BoosterType.JOBS));
        sendPointsDebug(sender, boosterService.getState(BoosterType.POINTS));
    }

    private void sendDebugRaw(CommandSender sender) {
        File stateFile = new File(plugin.getDataFolder(), "booster-state.yml");
        File localeFile = new File(plugin.getDataFolder(), localeService.activeLocaleFile());

        messageService.prefixed(sender, "<gray>Raw state and file paths:</gray>");
        messageService.send(sender,
                "<yellow>Artifact</yellow><gray>: <white><artifact></white></gray>",
                MessageService.value("artifact", buildInfo.artifactFileName()));
        messageService.send(sender,
                "<yellow>Data folder</yellow><gray>: <white><path></white></gray>",
                MessageService.value("path", plugin.getDataFolder().getAbsolutePath()));
        messageService.send(sender,
                "<yellow>Locale file</yellow><gray>: <white><path></white></gray>",
                MessageService.value("path", localeFile.getAbsolutePath()));
        messageService.send(sender,
                "<yellow>State file</yellow><gray>: <white><path></white></gray>",
                MessageService.value("path", stateFile.getAbsolutePath()));
        messageService.send(sender,
                "<yellow>Points config</yellow><gray>: <white><path></white></gray>",
                MessageService.value("path", boosterService.getPointsConfigFile().getAbsolutePath()));
        sendRawBoosterState(sender, boosterService.getState(BoosterType.MCMMO));
        sendRawBoosterState(sender, boosterService.getState(BoosterType.JOBS));
        sendRawBoosterState(sender, boosterService.getState(BoosterType.POINTS));
    }

    private void sendDebugLogs(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Recent audit actions:</gray>");
        List<String> entries = boosterService.getRecentActions();
        if (entries.isEmpty()) {
            messageService.send(sender, "<gray>No recent actions have been logged yet.</gray>");
            return;
        }
        for (String entry : entries) {
            messageService.send(sender, "<gray>" + escapeMiniMessage(entry) + "</gray>");
        }
    }

    private void sendMcMMODebug(CommandSender sender, BoosterState state) {
        messageService.send(sender,
                "<yellow>mcMMO</yellow><gray>: enabled=<white><tracking></white>, dependency=<white><dependency></white>, active=<white><active></white>, mode=<white><mode></white>, rate=<white><rate>x</white>, announce=<white><announce></white></gray>",
                MessageService.value("tracking", String.valueOf(boosterService.isTrackingEnabled(BoosterType.MCMMO))),
                MessageService.value("dependency", String.valueOf(boosterService.isDependencyAvailable(BoosterType.MCMMO))),
                MessageService.value("active", String.valueOf(state.active())),
                MessageService.value("mode", state.active() ? (state.hasTrackedDuration() ? "timed" : "manual") : "inactive"),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("announce", String.valueOf(state.announceOnStart())));
        messageService.send(sender,
                "<gray>  started=<white><started></white>, duration=<white><duration></white>, ends=<white><ends></white>, remaining=<white><remaining></white></gray>",
                MessageService.value("started", formatTimestamp(state.startedAtEpochMillis())),
                MessageService.value("duration", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.durationMillis()) : "n/a"),
                MessageService.value("ends", formatTimestamp(state.endsAtEpochMillis())),
                MessageService.value("remaining", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis())) : "n/a"));
    }

    private void sendJobsDebug(CommandSender sender, BoosterState state) {
        messageService.send(sender,
                "<yellow>Jobs</yellow><gray>: enabled=<white><tracking></white>, dependency=<white><dependency></white>, active=<white><active></white>, target=<white><target></white>, scope=<white><scope></white>, rate=<white><rate>x</white></gray>",
                MessageService.value("tracking", String.valueOf(boosterService.isTrackingEnabled(BoosterType.JOBS))),
                MessageService.value("dependency", String.valueOf(boosterService.isDependencyAvailable(BoosterType.JOBS))),
                MessageService.value("active", String.valueOf(state.active())),
                MessageService.value("target", state.jobsTarget().isBlank() ? "n/a" : state.jobsTarget()),
                MessageService.value("scope", state.jobsScope().isBlank() ? "n/a" : state.jobsScope()),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())));
        messageService.send(sender,
                "<gray>  started=<white><started></white>, duration=<white><duration></white>, ends=<white><ends></white>, remaining=<white><remaining></white></gray>",
                MessageService.value("started", formatTimestamp(state.startedAtEpochMillis())),
                MessageService.value("duration", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.durationMillis()) : "n/a"),
                MessageService.value("ends", formatTimestamp(state.endsAtEpochMillis())),
                MessageService.value("remaining", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis())) : "n/a"));
    }

    private void sendPointsDebug(CommandSender sender, BoosterState state) {
        messageService.send(sender,
                "<yellow>Points</yellow><gray>: enabled=<white><enabled></white>, visible=<white><visible></white>, experimental=<white><experimental></white>, dependency=<white><dependency></white>, config=<white><config></white>, active=<white><active></white>, rate=<white><rate>x</white></gray>",
                MessageService.value("enabled", String.valueOf(boosterService.isTrackingEnabled(BoosterType.POINTS))),
                MessageService.value("visible", String.valueOf(boosterService.isFeatureVisible(BoosterType.POINTS))),
                MessageService.value("experimental", String.valueOf(boosterService.isExperimental(BoosterType.POINTS))),
                MessageService.value("dependency", String.valueOf(boosterService.isDependencyAvailable(BoosterType.POINTS))),
                MessageService.value("config", String.valueOf(boosterService.isPointsConfigPresent())),
                MessageService.value("active", String.valueOf(state.active())),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())));
        messageService.send(sender,
                "<gray>  config_file=<white><file></white>, reload_command=<white><reload></white>, base=<white><ingame>/<discord></white>, current=<white><current_ingame>/<current_discord></white></gray>",
                MessageService.value("file", boosterService.getPointsConfigFile().getAbsolutePath()),
                MessageService.value("reload", boosterService.getPointsReloadCommand()),
                MessageService.value("ingame", String.valueOf(boosterService.getPointsBaseIngamePoints())),
                MessageService.value("discord", String.valueOf(boosterService.getPointsBaseDiscordPoints())),
                MessageService.value("current_ingame", String.valueOf(boosterService.getPointsCurrentIngamePoints())),
                MessageService.value("current_discord", String.valueOf(boosterService.getPointsCurrentDiscordPoints())));
        messageService.send(sender,
                "<gray>  started=<white><started></white>, duration=<white><duration></white>, ends=<white><ends></white>, remaining=<white><remaining></white></gray>",
                MessageService.value("started", formatTimestamp(state.startedAtEpochMillis())),
                MessageService.value("duration", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.durationMillis()) : "n/a"),
                MessageService.value("ends", formatTimestamp(state.endsAtEpochMillis())),
                MessageService.value("remaining", state.hasTrackedDuration() ? DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis())) : "n/a"));
    }

    private void sendRawBoosterState(CommandSender sender, BoosterState state) {
        messageService.send(sender,
                "<yellow><type></yellow><gray>: active=<white><active></white>, rate=<white><rate></white>, started_at=<white><started></white>, duration_millis=<white><duration></white>, ends_at=<white><ends></white>, announce=<white><announce></white>, jobs_target=<white><target></white>, jobs_scope=<white><scope></white></gray>",
                MessageService.value("type", state.type().displayName()),
                MessageService.value("active", String.valueOf(state.active())),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("started", String.valueOf(state.startedAtEpochMillis())),
                MessageService.value("duration", String.valueOf(state.durationMillis())),
                MessageService.value("ends", String.valueOf(state.endsAtEpochMillis())),
                MessageService.value("announce", String.valueOf(state.announceOnStart())),
                MessageService.value("target", state.jobsTarget().isBlank() ? "-" : state.jobsTarget()),
                MessageService.value("scope", state.jobsScope().isBlank() ? "-" : state.jobsScope()));
    }

    private void sendDebugSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage: <yellow>/rate debug [summary|reference|commands|permissions|placeholders|integrations|state|raw|config|toggle|all|logs|clean logs|cleanlogs]</yellow></gray>");
    }

    private boolean hasViewPermission(CommandSender sender) {
        return sender.isOp()
                || sender.hasPermission("onemb.boosters.rate")
                || sender.hasPermission("onemb.boosters.admin")
                || sender.hasPermission("onemb.boosters.debug");
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("onemb.boosters.admin");
    }

    private boolean hasDebugPermission(CommandSender sender) {
        return sender.isOp()
                || sender.hasPermission("onemb.boosters.debug")
                || sender.hasPermission("onemb.boosters.admin");
    }

    private boolean shouldShowToSender(BoosterType type, CommandSender sender) {
        if (type != BoosterType.POINTS) {
            return configSectionVisible(type.key());
        }
        if (!boosterService.isTrackingEnabled(type)) {
            return false;
        }
        return boosterService.isFeatureVisible(type) || (hasAdminPermission(sender) && boosterService.shouldShowPointsToAdmins());
    }

    private boolean isBoosterAvailableForCommand(BoosterType type) {
        return type != BoosterType.POINTS || boosterService.isTrackingEnabled(BoosterType.POINTS);
    }

    private List<String> rootArgumentsFor(CommandSender sender) {
        List<String> arguments = new ArrayList<>();
        if (hasViewPermission(sender)) {
            arguments.add("info");
        }
        if (hasAdminPermission(sender)) {
            arguments.add("start");
            arguments.add("stop");
            arguments.add("reload");
        }
        if (hasDebugPermission(sender)) {
            arguments.add("debug");
        }
        return arguments;
    }

    private List<String> startTargets() {
        List<String> targets = new ArrayList<>();
        for (BoosterType type : BoosterType.values()) {
            if (isBoosterAvailableForCommand(type)) {
                targets.add(type.key());
            }
        }
        targets.add("all");
        return targets;
    }

    private List<String> stopTargets() {
        return startTargets();
    }

    private boolean shouldIncludeInAll(BoosterType type) {
        if (!isBoosterAvailableForCommand(type)) {
            return false;
        }
        return type != BoosterType.POINTS || boosterService.isFeatureVisible(BoosterType.POINTS);
    }

    private boolean configSectionVisible(String key) {
        return plugin.getConfig().getBoolean("display.sections." + key, true);
    }

    private String label(BoosterType type) {
        return switch (type) {
            case MCMMO -> plugin.getConfig().getString("display.labels.mcmmo", "<yellow>mcMMO</yellow>");
            case JOBS -> plugin.getConfig().getString("display.labels.jobs", "<yellow>Jobs</yellow>");
            case POINTS -> plugin.getConfig().getString("display.labels.points", "<yellow>Points</yellow>");
        };
    }

    private String locale(String path, String fallback) {
        return localeService.get(path, fallback);
    }

    private List<String> configDebugPaths() {
        return List.of(
                "locale.file",
                "restore.delayTicks",
                "features.mcmmo.enabled",
                "features.jobs.enabled",
                "features.points.enabled",
                "features.points.visible",
                "features.points.experimental",
                "display.sections.mcmmo",
                "display.sections.jobs",
                "display.sections.points",
                "display.labels.mcmmo",
                "display.labels.jobs",
                "display.labels.points",
                "display.adminShowExperimentalIntegrations",
                "tabCompletion.commonDurations",
                "tabCompletion.commonRates",
                "logging.auditToConsole",
                "logging.recentActionLimit",
                "broadcasts.start.global",
                "broadcasts.start.mcmmo",
                "broadcasts.start.jobs",
                "broadcasts.start.points",
                "broadcasts.stop.global",
                "broadcasts.stop.mcmmo",
                "broadcasts.stop.jobs",
                "broadcasts.stop.points",
                "mcmmo.announceOnRateStart",
                "mcmmo.announceOnRestore",
                "points.pluginName",
                "points.configFile",
                "points.reloadCommand",
                "points.ingamePath",
                "points.discordPath",
                "points.baseIngamePoints",
                "points.baseDiscordPoints",
                "commandHooks.start.global",
                "commandHooks.start.mcmmo",
                "commandHooks.start.jobs",
                "commandHooks.start.points",
                "commandHooks.stop.global",
                "commandHooks.stop.mcmmo",
                "commandHooks.stop.jobs",
                "commandHooks.stop.points"
        );
    }

    private List<String> toggleableConfigPaths() {
        return List.of(
                "features.mcmmo.enabled",
                "features.jobs.enabled",
                "features.points.enabled",
                "features.points.visible",
                "features.points.experimental",
                "display.sections.mcmmo",
                "display.sections.jobs",
                "display.sections.points",
                "display.adminShowExperimentalIntegrations",
                "logging.auditToConsole",
                "mcmmo.announceOnRateStart",
                "mcmmo.announceOnRestore"
        );
    }

    private void sendConfigSectionHeader(CommandSender sender, String title) {
        messageService.send(sender, "<yellow>" + title + "</yellow><gray>:</gray>");
    }

    private void sendConfigEntry(CommandSender sender, String path) {
        Object value = plugin.getConfig().get(path);
        sendClickableDebugEntry(sender, path, formatConfigValue(value), "Click to copy the config key");
    }

    private String formatConfigValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof List<?> list) {
            return list.isEmpty() ? "[]" : list.toString();
        }
        return String.valueOf(value);
    }

    private List<String> configuredDurations() {
        List<String> durations = plugin.getConfig().getStringList("tabCompletion.commonDurations");
        return durations.isEmpty() ? List.of("15m", "30m", "1h", "2h") : durations;
    }

    private List<String> configuredRates() {
        List<String> rates = plugin.getConfig().getStringList("tabCompletion.commonRates");
        return rates.isEmpty() ? List.of("2", "2.5", "3") : rates;
    }

    private void sendClickableDebugEntry(CommandSender sender, String value, String description, String hoverText) {
        String escapedValue = value.replace("\\", "\\\\").replace("'", "\\'");
        String escapedDescription = description.replace("\\", "\\\\").replace("'", "\\'");
        String escapedHover = hoverText.replace("\\", "\\\\").replace("'", "\\'");
        messageService.send(sender,
                "<click:suggest_command:'" + escapedValue + "'><hover:show_text:'<gray>" + escapedHover + "</gray>'><white>"
                        + escapedValue + "</white></hover></click><gray> - " + escapedDescription + "</gray>");
    }

    private List<String> partialMatches(String token, List<String> options) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, matches);
        return matches;
    }

    private String pluginStatus(Plugin pluginDependency) {
        if (pluginDependency == null) {
            return "not installed";
        }
        return pluginDependency.isEnabled()
                ? "enabled (" + pluginDependency.getPluginMeta().getVersion() + ")"
                : "installed but disabled";
    }

    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0L) {
            return "n/a";
        }
        return Instant.ofEpochMilli(epochMillis).toString();
    }

    private String senderName(CommandSender sender) {
        return sender.getName() == null || sender.getName().isBlank() ? "Console" : sender.getName();
    }

    private Object parseConfigValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }
        return value;
    }

    private String escapeMiniMessage(String input) {
        return input.replace("<", "&lt;").replace(">", "&gt;");
    }
}
