package com.mrfdev.boosters.service;

import com.mrfdev.boosters.model.BoosterActionResult;
import com.mrfdev.boosters.model.BoosterState;
import com.mrfdev.boosters.model.BoosterType;
import com.mrfdev.boosters.storage.BoosterStateStorage;
import com.mrfdev.boosters.util.DurationUtil;
import com.mrfdev.boosters.util.LocaleService;
import com.mrfdev.boosters.util.MessageService;
import com.mrfdev.boosters.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BoosterService {

    private final JavaPlugin plugin;
    private final BoosterStateStorage storage;
    private final MessageService messageService;
    private final PyroWelcomesPointsProvider pointsProvider;
    private final LocaleService localeService;
    private final EnumMap<BoosterType, BoosterState> states = new EnumMap<>(BoosterType.class);
    private final EnumMap<BoosterType, BukkitTask> expiryTasks = new EnumMap<>(BoosterType.class);
    private final Deque<String> recentActions = new ArrayDeque<>();

    public BoosterService(JavaPlugin plugin, BoosterStateStorage storage, MessageService messageService,
                          PyroWelcomesPointsProvider pointsProvider, LocaleService localeService) {
        this.plugin = plugin;
        this.storage = storage;
        this.messageService = messageService;
        this.pointsProvider = pointsProvider;
        this.localeService = localeService;

        states.putAll(storage.loadStates());
        for (BoosterType type : BoosterType.values()) {
            states.putIfAbsent(type, BoosterState.inactive(type));
        }
        cleanupExpiredStoredState();
    }

    public BoosterState getState(BoosterType type) {
        expireIfNeeded(type);
        return states.getOrDefault(type, BoosterState.inactive(type));
    }

    public boolean isTrackingEnabled(BoosterType type) {
        return config().getBoolean("features." + type.key() + ".enabled", type != BoosterType.POINTS);
    }

    public boolean isFeatureVisible(BoosterType type) {
        if (type != BoosterType.POINTS) {
            return true;
        }
        return config().getBoolean("features.points.visible", false);
    }

    public boolean isExperimental(BoosterType type) {
        if (type != BoosterType.POINTS) {
            return false;
        }
        return config().getBoolean("features.points.experimental", true);
    }

    public boolean isDependencyAvailable(BoosterType type) {
        return switch (type) {
            case MCMMO, JOBS -> plugin.getServer().getPluginManager().isPluginEnabled(type.dependencyName());
            case POINTS -> pointsProvider.isDependencyEnabled();
        };
    }

    public boolean getMcMMODefaultAnnounceOnStart() {
        return config().getBoolean("mcmmo.announceOnRateStart", true);
    }

    public boolean shouldShowPointsToAdmins() {
        return config().getBoolean("display.adminShowExperimentalIntegrations", true);
    }

    public boolean shouldExposePointsPlaceholders() {
        return isTrackingEnabled(BoosterType.POINTS) && isFeatureVisible(BoosterType.POINTS);
    }

    public List<String> getRecentActions() {
        return List.copyOf(recentActions);
    }

    public void clearRecentActions() {
        recentActions.clear();
        if (config().getBoolean("logging.auditToConsole", true)) {
            plugin.getLogger().info("Cleared recent action log.");
        }
    }

    public File getPointsConfigFile() {
        return pointsProvider.configFile();
    }

    public String getPointsReloadCommand() {
        return pointsProvider.reloadCommand();
    }

    public int getPointsBaseIngamePoints() {
        return pointsProvider.configuredBaseIngamePoints();
    }

    public int getPointsBaseDiscordPoints() {
        return pointsProvider.configuredBaseDiscordPoints();
    }

    public int getPointsCurrentIngamePoints() {
        return pointsProvider.currentIngamePoints();
    }

    public int getPointsCurrentDiscordPoints() {
        return pointsProvider.currentDiscordPoints();
    }

    public boolean isPointsConfigPresent() {
        return pointsProvider.isConfigPresent();
    }

    public BoosterActionResult startTimedBooster(BoosterType type, long durationMillis, double rate, String senderName) {
        return switch (type) {
            case MCMMO -> startTimedMcMMO(durationMillis, rate, senderName);
            case JOBS -> startTimedJobs("all", "all", durationMillis, rate, senderName);
            case POINTS -> startTimedPoints(durationMillis, rate, senderName);
        };
    }

    public BoosterActionResult stopBooster(BoosterType type, String senderName) {
        BoosterState previous = getState(type);
        if (!previous.active()) {
            return BoosterActionResult.failure("There is no tracked " + type.displayName() + " booster to stop.");
        }

        String pointsResetError = null;
        switch (type) {
            case MCMMO -> {
                if (isDependencyAvailable(type)) {
                    dispatchConsole("xprate reset");
                }
            }
            case JOBS -> {
                if (isDependencyAvailable(type)) {
                    dispatchConsole("jobs boost all reset");
                }
            }
            case POINTS -> pointsResetError = pointsProvider.resetToBase();
        }

        if (pointsResetError != null) {
            return BoosterActionResult.failure(pointsResetError);
        }

        clearState(type);
        handleBoosterStopped(type, previous, senderName);
        logAction("Stopped " + type.displayName() + " booster by " + senderName + ".");
        return BoosterActionResult.success(previous, "Stopped the tracked " + type.displayName() + " booster.");
    }

    public void recordExternalMcMMOBoost(double rate, boolean announce, String senderName) {
        if (!isTrackingEnabled(BoosterType.MCMMO)) {
            return;
        }

        BoosterState previous = states.getOrDefault(BoosterType.MCMMO, BoosterState.inactive(BoosterType.MCMMO));
        if (rate <= 1.0D) {
            clearState(BoosterType.MCMMO);
            if (previous.active()) {
                handleBoosterStopped(BoosterType.MCMMO, previous, senderName);
            }
            logAction("Cleared tracked mcMMO state from an external command.");
            return;
        }

        long now = System.currentTimeMillis();
        BoosterState updated;

        if (previous.active() && previous.hasTrackedDuration() && !previous.isExpired(now)) {
            updated = new BoosterState(
                    BoosterType.MCMMO,
                    true,
                    rate,
                    previous.startedAtEpochMillis(),
                    previous.durationMillis(),
                    previous.endsAtEpochMillis(),
                    announce,
                    "",
                    ""
            );
        } else {
            updated = new BoosterState(BoosterType.MCMMO, true, rate, now, 0L, 0L, announce, "", "");
        }

        setState(updated);
        handleBoosterStarted(BoosterType.MCMMO, updated, senderName);
        logAction("Tracked external mcMMO command at " + NumberUtil.formatRate(rate) + "x.");
    }

    public void recordExternalMcMMOReset(String senderName) {
        if (!isTrackingEnabled(BoosterType.MCMMO)) {
            return;
        }
        BoosterState previous = states.getOrDefault(BoosterType.MCMMO, BoosterState.inactive(BoosterType.MCMMO));
        clearState(BoosterType.MCMMO);
        if (previous.active()) {
            handleBoosterStopped(BoosterType.MCMMO, previous, senderName);
        }
        logAction("Tracked external mcMMO reset.");
    }

    public void recordExternalJobsBoost(String target, String scope, long durationMillis, double rate, String senderName) {
        if (!isTrackingEnabled(BoosterType.JOBS)) {
            return;
        }

        BoosterState previous = states.getOrDefault(BoosterType.JOBS, BoosterState.inactive(BoosterType.JOBS));
        if (durationMillis <= 0L || rate <= 1.0D) {
            clearState(BoosterType.JOBS);
            if (previous.active()) {
                handleBoosterStopped(BoosterType.JOBS, previous, senderName);
            }
            logAction("Cleared tracked Jobs state from an external command.");
            return;
        }

        BoosterState updated = createTimedState(BoosterType.JOBS, rate, durationMillis, false, target, scope);
        setState(updated);
        handleBoosterStarted(BoosterType.JOBS, updated, senderName);
        logAction("Tracked external Jobs command at " + NumberUtil.formatRate(rate) + "x for " + DurationUtil.formatFriendlyDuration(durationMillis) + ".");
    }

    public void recordExternalJobsReset(String senderName) {
        if (!isTrackingEnabled(BoosterType.JOBS)) {
            return;
        }
        BoosterState previous = states.getOrDefault(BoosterType.JOBS, BoosterState.inactive(BoosterType.JOBS));
        clearState(BoosterType.JOBS);
        if (previous.active()) {
            handleBoosterStopped(BoosterType.JOBS, previous, senderName);
        }
        logAction("Tracked external Jobs reset.");
    }

    public void restoreTrackedBoosters() {
        cleanupExpiredStoredState();
        warnAboutMissingDependenciesForStoredBoosters();

        restoreMcMMO();
        restoreJobs();
        restorePoints();
    }

    public void logStartupSelfCheck() {
        messageService.info(plugin,
                "<gray>Startup self-check: locale=<yellow><locale></yellow>, mcMMO=<yellow><mcmmo></yellow>, Jobs=<yellow><jobs></yellow>, Points=<yellow><points></yellow>, PAPI=<yellow><papi></yellow>.</gray>",
                MessageService.value("locale", localeService.activeLocaleFile()),
                MessageService.value("mcmmo", integrationStatus(BoosterType.MCMMO)),
                MessageService.value("jobs", integrationStatus(BoosterType.JOBS)),
                MessageService.value("points", pointsStatus()),
                MessageService.value("papi", plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") ? "enabled" : "missing"));
        warnAboutMissingDependenciesForStoredBoosters();
    }

    public void reloadRuntimeSettings() {
        trimRecentActions();
        logAction("Reloaded 1MB Boosters config and locale.");
    }

    public void shutdown() {
        for (BukkitTask task : expiryTasks.values()) {
            task.cancel();
        }
        expiryTasks.clear();
        storage.saveStates(states);
    }

    private BoosterActionResult startTimedMcMMO(long durationMillis, double rate, String senderName) {
        boolean announce = getMcMMODefaultAnnounceOnStart();
        dispatchConsole("xprate " + NumberUtil.formatRate(rate) + " " + announce);
        BoosterState state = createTimedState(BoosterType.MCMMO, rate, durationMillis, announce, "", "");
        setState(state);
        handleBoosterStarted(BoosterType.MCMMO, state, senderName);
        logAction("Started mcMMO booster at " + NumberUtil.formatRate(rate) + "x for " + DurationUtil.formatFriendlyDuration(durationMillis) + " by " + senderName + ".");
        return BoosterActionResult.success(state, "Started the tracked mcMMO booster.");
    }

    private BoosterActionResult startTimedJobs(String target, String scope, long durationMillis, double rate, String senderName) {
        dispatchConsole("jobs boost " + target + " " + scope + " "
                + DurationUtil.formatCompactDuration(durationMillis) + " " + NumberUtil.formatRate(rate));
        BoosterState state = createTimedState(BoosterType.JOBS, rate, durationMillis, false, target, scope);
        setState(state);
        handleBoosterStarted(BoosterType.JOBS, state, senderName);
        logAction("Started Jobs booster at " + NumberUtil.formatRate(rate) + "x for " + DurationUtil.formatFriendlyDuration(durationMillis) + " by " + senderName + ".");
        return BoosterActionResult.success(state, "Started the tracked Jobs booster.");
    }

    private BoosterActionResult startTimedPoints(long durationMillis, double rate, String senderName) {
        if (!NumberUtil.isWholeNumber(rate)) {
            return BoosterActionResult.failure("Points boosters only support whole-number multipliers.");
        }

        int multiplier = (int) rate;
        String error = pointsProvider.applyMultiplier(multiplier);
        if (error != null) {
            return BoosterActionResult.failure(error);
        }

        BoosterState state = createTimedState(BoosterType.POINTS, rate, durationMillis, false, "", "");
        setState(state);
        handleBoosterStarted(BoosterType.POINTS, state, senderName);
        logAction("Started Points booster at " + multiplier + "x for " + DurationUtil.formatFriendlyDuration(durationMillis) + " by " + senderName + ".");
        return BoosterActionResult.success(state, "Started the tracked Points booster.");
    }

    private BoosterState createTimedState(BoosterType type, double rate, long durationMillis, boolean announce,
                                          String jobsTarget, String jobsScope) {
        long now = System.currentTimeMillis();
        return new BoosterState(
                type,
                true,
                rate,
                now,
                durationMillis,
                now + durationMillis,
                announce,
                jobsTarget,
                jobsScope
        );
    }

    private void restoreMcMMO() {
        BoosterState state = states.getOrDefault(BoosterType.MCMMO, BoosterState.inactive(BoosterType.MCMMO));
        if (!state.active() || !isTrackingEnabled(BoosterType.MCMMO) || !isDependencyAvailable(BoosterType.MCMMO)) {
            return;
        }

        boolean announceOnRestore = config().getBoolean("mcmmo.announceOnRestore", false) && state.announceOnStart();
        dispatchConsole("xprate " + NumberUtil.formatRate(state.rate()) + " " + announceOnRestore);
        scheduleExpiry(state);

        if (state.hasTrackedDuration()) {
            messageService.info(plugin,
                    "<gray>Restored the tracked <yellow>mcMMO</yellow> booster at <aqua><rate>x</aqua> with <aqua><remaining></aqua> left.</gray>",
                    MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                    MessageService.value("remaining", DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis()))));
        } else {
            messageService.info(plugin,
                    "<gray>Restored the tracked <yellow>mcMMO</yellow> booster at <aqua><rate>x</aqua> with no tracked end time.</gray>",
                    MessageService.value("rate", NumberUtil.formatRate(state.rate())));
        }
        logAction("Restored tracked mcMMO booster.");
    }

    private void restoreJobs() {
        BoosterState state = states.getOrDefault(BoosterType.JOBS, BoosterState.inactive(BoosterType.JOBS));
        if (!state.active() || !state.hasTrackedDuration()
                || !isTrackingEnabled(BoosterType.JOBS)
                || !isDependencyAvailable(BoosterType.JOBS)) {
            return;
        }

        long remainingMillis = state.remainingMillis(System.currentTimeMillis());
        dispatchConsole("jobs boost " + state.jobsTarget() + " " + state.jobsScope() + " "
                + DurationUtil.formatCompactDuration(remainingMillis) + " " + NumberUtil.formatRate(state.rate()));
        scheduleExpiry(state);

        String scope = state.isJobsGlobal() ? "global" : state.jobsDescriptor();
        messageService.info(plugin,
                "<gray>Restored the tracked <yellow>Jobs</yellow> booster (<aqua><scope></aqua>) at <aqua><rate>x</aqua> with <aqua><remaining></aqua> left.</gray>",
                MessageService.value("scope", scope),
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("remaining", DurationUtil.formatFriendlyDuration(remainingMillis)));
        logAction("Restored tracked Jobs booster.");
    }

    private void restorePoints() {
        BoosterState state = states.getOrDefault(BoosterType.POINTS, BoosterState.inactive(BoosterType.POINTS));
        if (!state.active() || !state.hasTrackedDuration()
                || !isTrackingEnabled(BoosterType.POINTS)
                || !isDependencyAvailable(BoosterType.POINTS)) {
            return;
        }

        String error = pointsProvider.applyMultiplier((int) state.rate());
        if (error != null) {
            plugin.getLogger().warning("Could not restore tracked Points booster: " + error);
            logAction("Failed to restore tracked Points booster: " + error);
            return;
        }

        scheduleExpiry(state);
        messageService.info(plugin,
                "<gray>Restored the tracked <yellow>Points</yellow> booster at <aqua><rate>x</aqua> with <aqua><remaining></aqua> left.</gray>",
                MessageService.value("rate", NumberUtil.formatRate(state.rate())),
                MessageService.value("remaining", DurationUtil.formatFriendlyDuration(state.remainingMillis(System.currentTimeMillis()))));
        logAction("Restored tracked Points booster.");
    }

    private void expireIfNeeded(BoosterType type) {
        BoosterState state = states.getOrDefault(type, BoosterState.inactive(type));
        if (!state.isExpired(System.currentTimeMillis())) {
            return;
        }

        boolean canClear = true;
        switch (type) {
            case MCMMO -> {
                if (isDependencyAvailable(type)) {
                    dispatchConsole("xprate reset");
                }
            }
            case JOBS -> {
            }
            case POINTS -> {
                String error = pointsProvider.resetToBase();
                if (error != null) {
                    plugin.getLogger().warning("Could not reset expired Points booster: " + error);
                    logAction("Failed to reset expired Points booster: " + error);
                    canClear = false;
                }
            }
        }

        if (canClear) {
            clearState(type);
            handleBoosterStopped(type, state, "Timer");
            logAction("Expired " + type.displayName() + " booster and cleared tracked state.");
        }
    }

    private void cleanupExpiredStoredState() {
        boolean changed = false;
        long now = System.currentTimeMillis();
        for (Map.Entry<BoosterType, BoosterState> entry : states.entrySet()) {
            BoosterState state = entry.getValue();
            if (state.isExpired(now)) {
                entry.setValue(BoosterState.inactive(entry.getKey()));
                changed = true;
            }
        }

        if (changed) {
            storage.saveStates(states);
        }
    }

    private void setState(BoosterState state) {
        states.put(state.type(), state);
        storage.saveStates(states);
        scheduleExpiry(state);
    }

    private void clearState(BoosterType type) {
        cancelExpiry(type);
        states.put(type, BoosterState.inactive(type));
        storage.saveStates(states);
    }

    private void scheduleExpiry(BoosterState state) {
        cancelExpiry(state.type());
        if (!state.hasTrackedDuration()) {
            return;
        }

        long remainingMillis = state.remainingMillis(System.currentTimeMillis());
        if (remainingMillis <= 0L) {
            expireIfNeeded(state.type());
            return;
        }

        long expectedEnd = state.endsAtEpochMillis();
        long delayTicks = Math.max(1L, (remainingMillis + 49L) / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> handleScheduledExpiry(state.type(), expectedEnd), delayTicks);
        expiryTasks.put(state.type(), task);
    }

    private void handleScheduledExpiry(BoosterType type, long expectedEnd) {
        BoosterState current = states.getOrDefault(type, BoosterState.inactive(type));
        if (!current.active() || current.endsAtEpochMillis() != expectedEnd) {
            return;
        }
        expireIfNeeded(type);
    }

    private void handleBoosterStarted(BoosterType type, BoosterState state, String senderName) {
        broadcastStart(type, state, senderName);
        executeLifecycleCommands("start", type, state, senderName);
    }

    private void handleBoosterStopped(BoosterType type, BoosterState state, String senderName) {
        broadcastStop(type, state, senderName);
        executeLifecycleCommands("stop", type, state, senderName);
    }

    private void cancelExpiry(BoosterType type) {
        BukkitTask existing = expiryTasks.remove(type);
        if (existing != null) {
            existing.cancel();
        }
    }

    private void broadcastStart(BoosterType type, BoosterState state, String senderName) {
        String duration = DurationUtil.formatFriendlyDuration(state.durationMillis());
        String rate = NumberUtil.formatRate(state.rate());
        broadcastIfConfigured(config().getString("broadcasts.start.global", ""),
                type, rate, duration, senderName, state);
        broadcastIfConfigured(config().getString("broadcasts.start." + type.key(), ""),
                type, rate, duration, senderName, state);
    }

    private void broadcastStop(BoosterType type, BoosterState state, String senderName) {
        String duration = state.hasTrackedDuration()
                ? DurationUtil.formatFriendlyDuration(state.durationMillis())
                : "manual";
        String rate = NumberUtil.formatRate(state.rate());
        broadcastIfConfigured(config().getString("broadcasts.stop.global", ""),
                type, rate, duration, senderName, state);
        broadcastIfConfigured(config().getString("broadcasts.stop." + type.key(), ""),
                type, rate, duration, senderName, state);
    }

    private void broadcastIfConfigured(String template, BoosterType type, String rate, String duration,
                                       String senderName, BoosterState state) {
        if (template == null || template.isBlank()) {
            return;
        }

        net.kyori.adventure.text.Component component = messageServiceComponent(template, type, rate, duration, senderName);

        messageService.send(Bukkit.getConsoleSender(), template,
                MessageService.value("booster", displayLabel(type)),
                MessageService.value("rate", rate),
                MessageService.value("duration", duration),
                MessageService.value("sender", senderName),
                MessageService.value("ingame", String.valueOf(getPointsCurrentIngamePoints())),
                MessageService.value("discord", String.valueOf(getPointsCurrentDiscordPoints())));

        plugin.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(component));
    }

    private void executeLifecycleCommands(String phase, BoosterType type, BoosterState state, String senderName) {
        executeConfiguredCommands(config().getStringList("commandHooks." + phase + ".global"), phase, type, state, senderName);
        executeConfiguredCommands(config().getStringList("commandHooks." + phase + "." + type.key()), phase, type, state, senderName);
    }

    private void executeConfiguredCommands(List<String> commands, String phase, BoosterType type, BoosterState state, String senderName) {
        for (String rawCommand : commands) {
            if (rawCommand == null) {
                continue;
            }

            String trimmed = rawCommand.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String commandNoSlash = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
            String renderedCommand = renderHookCommand(commandNoSlash, phase, type, state, senderName);
            boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), renderedCommand);
            if (!dispatched) {
                plugin.getLogger().warning("Configured " + phase + " command did not run successfully for " + type.displayName() + ": " + renderedCommand);
            }
        }
    }

    private String renderHookCommand(String command, String phase, BoosterType type, BoosterState state, String senderName) {
        long now = System.currentTimeMillis();
        String duration = state.hasTrackedDuration()
                ? DurationUtil.formatFriendlyDuration(state.durationMillis())
                : "Manual";
        String durationCompact = state.hasTrackedDuration()
                ? DurationUtil.formatCompactDuration(state.durationMillis())
                : "manual";
        long remainingMillis = state.hasTrackedDuration() ? state.remainingMillis(now) : 0L;
        String remaining = state.hasTrackedDuration()
                ? DurationUtil.formatFriendlyDuration(remainingMillis)
                : "Manual";
        String remainingCompact = state.hasTrackedDuration()
                ? DurationUtil.formatCompactDuration(remainingMillis)
                : "manual";

        return command
                .replace("{phase}", phase)
                .replace("{booster}", type.displayName())
                .replace("{booster_key}", type.key())
                .replace("{booster_label}", displayLabel(type))
                .replace("{rate}", NumberUtil.formatRate(state.rate()))
                .replace("{duration}", duration)
                .replace("{duration_compact}", durationCompact)
                .replace("{remaining}", remaining)
                .replace("{remaining_compact}", remainingCompact)
                .replace("{sender}", senderName == null || senderName.isBlank() ? "Console" : senderName)
                .replace("{jobs_target}", state.jobsTarget().isBlank() ? "n/a" : state.jobsTarget())
                .replace("{jobs_scope}", state.jobsScope().isBlank() ? "n/a" : state.jobsScope())
                .replace("{ingame}", String.valueOf(getPointsCurrentIngamePoints()))
                .replace("{discord}", String.valueOf(getPointsCurrentDiscordPoints()));
    }

    private net.kyori.adventure.text.Component messageServiceComponent(String template, BoosterType type, String rate,
                                                                       String duration, String senderName) {
        return net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(template,
                MessageService.value("booster", displayLabel(type)),
                MessageService.value("rate", rate),
                MessageService.value("duration", duration),
                MessageService.value("sender", senderName),
                MessageService.value("ingame", String.valueOf(getPointsCurrentIngamePoints())),
                MessageService.value("discord", String.valueOf(getPointsCurrentDiscordPoints())));
    }

    private void warnAboutMissingDependenciesForStoredBoosters() {
        for (BoosterType type : BoosterType.values()) {
            BoosterState state = states.getOrDefault(type, BoosterState.inactive(type));
            if (!state.active()) {
                continue;
            }
            if (!isTrackingEnabled(type)) {
                continue;
            }
            if (isDependencyAvailable(type)) {
                continue;
            }
            plugin.getLogger().warning("Stored " + type.displayName() + " booster state exists, but the dependency plugin is missing or disabled.");
        }
    }

    private String integrationStatus(BoosterType type) {
        if (!isTrackingEnabled(type)) {
            return "disabled";
        }
        if (!isDependencyAvailable(type)) {
            return "missing";
        }
        if (type == BoosterType.POINTS && !pointsProvider.isConfigPresent()) {
            return "config missing";
        }
        if (type == BoosterType.POINTS && isExperimental(type)) {
            return isFeatureVisible(type) ? "experimental visible" : "experimental hidden";
        }
        return "enabled";
    }

    private String pointsStatus() {
        return integrationStatus(BoosterType.POINTS);
    }

    private String displayLabel(BoosterType type) {
        return switch (type) {
            case MCMMO -> config().getString("display.labels.mcmmo", "mcMMO");
            case JOBS -> config().getString("display.labels.jobs", "Jobs");
            case POINTS -> config().getString("display.labels.points", "Points");
        };
    }

    private void logAction(String message) {
        String entry = "[" + java.time.Instant.now() + "] " + message;
        recentActions.addFirst(entry);
        trimRecentActions();
        if (config().getBoolean("logging.auditToConsole", true)) {
            plugin.getLogger().info(message);
        }
    }

    private void trimRecentActions() {
        int maxEntries = Math.max(5, config().getInt("logging.recentActionLimit", 25));
        while (recentActions.size() > maxEntries) {
            recentActions.removeLast();
        }
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }

    private void dispatchConsole(String commandNoSlash) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandNoSlash);
    }
}
