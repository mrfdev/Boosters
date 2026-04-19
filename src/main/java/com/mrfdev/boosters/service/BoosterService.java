package com.mrfdev.boosters.service;

import com.mrfdev.boosters.model.BoosterState;
import com.mrfdev.boosters.model.BoosterType;
import com.mrfdev.boosters.storage.BoosterStateStorage;
import com.mrfdev.boosters.util.DurationUtil;
import com.mrfdev.boosters.util.MessageService;
import com.mrfdev.boosters.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BoosterService {

    private final JavaPlugin plugin;
    private final BoosterStateStorage storage;
    private final MessageService messageService;
    private final EnumMap<BoosterType, BoosterState> states = new EnumMap<>(BoosterType.class);
    private final EnumMap<BoosterType, BukkitTask> expiryTasks = new EnumMap<>(BoosterType.class);

    public BoosterService(JavaPlugin plugin, BoosterStateStorage storage, MessageService messageService) {
        this.plugin = plugin;
        this.storage = storage;
        this.messageService = messageService;

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
        return plugin.getConfig().getBoolean(type.key() + ".enabled", true);
    }

    public boolean isDependencyAvailable(BoosterType type) {
        return plugin.getServer().getPluginManager().isPluginEnabled(type.dependencyName());
    }

    public boolean getMcMMODefaultAnnounceOnStart() {
        return plugin.getConfig().getBoolean("mcmmo.announceOnRateStart", true);
    }

    public BoosterState startTimedBooster(BoosterType type, long durationMillis, double rate) {
        return switch (type) {
            case MCMMO -> startTimedMcMMO(durationMillis, rate);
            case JOBS -> startTimedJobs("all", "all", durationMillis, rate);
        };
    }

    public BoosterState stopBooster(BoosterType type) {
        BoosterState previous = getState(type);
        if (!previous.active()) {
            return previous;
        }

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
        }

        clearState(type);
        return previous;
    }

    public List<BoosterType> stopAllBoosters() {
        List<BoosterType> stopped = new ArrayList<>();
        for (BoosterType type : BoosterType.values()) {
            BoosterState previous = stopBooster(type);
            if (previous.active()) {
                stopped.add(type);
            }
        }
        return stopped;
    }

    public void recordExternalMcMMOBoost(double rate, boolean announce) {
        if (!isTrackingEnabled(BoosterType.MCMMO)) {
            return;
        }

        if (rate <= 1.0D) {
            clearState(BoosterType.MCMMO);
            return;
        }

        BoosterState current = states.getOrDefault(BoosterType.MCMMO, BoosterState.inactive(BoosterType.MCMMO));
        long now = System.currentTimeMillis();
        BoosterState updated;

        if (current.active() && current.hasTrackedDuration() && !current.isExpired(now)) {
            updated = new BoosterState(
                    BoosterType.MCMMO,
                    true,
                    rate,
                    current.startedAtEpochMillis(),
                    current.durationMillis(),
                    current.endsAtEpochMillis(),
                    announce,
                    "",
                    ""
            );
        } else {
            updated = new BoosterState(BoosterType.MCMMO, true, rate, now, 0L, 0L, announce, "", "");
        }

        setState(updated);
    }

    public void recordExternalMcMMOReset() {
        if (!isTrackingEnabled(BoosterType.MCMMO)) {
            return;
        }
        clearState(BoosterType.MCMMO);
    }

    public void recordExternalJobsBoost(String target, String scope, long durationMillis, double rate) {
        if (!isTrackingEnabled(BoosterType.JOBS)) {
            return;
        }

        if (durationMillis <= 0L || rate <= 1.0D) {
            clearState(BoosterType.JOBS);
            return;
        }
        setState(createTimedState(BoosterType.JOBS, rate, durationMillis, false, target, scope));
    }

    public void recordExternalJobsReset() {
        if (!isTrackingEnabled(BoosterType.JOBS)) {
            return;
        }
        clearState(BoosterType.JOBS);
    }

    public void restoreTrackedBoosters() {
        cleanupExpiredStoredState();

        restoreMcMMO();
        restoreJobs();
    }

    public void shutdown() {
        for (BukkitTask task : expiryTasks.values()) {
            task.cancel();
        }
        expiryTasks.clear();
        storage.saveStates(states);
    }

    private BoosterState startTimedMcMMO(long durationMillis, double rate) {
        boolean announce = getMcMMODefaultAnnounceOnStart();
        dispatchConsole("xprate " + NumberUtil.formatRate(rate) + " " + announce);
        BoosterState state = createTimedState(BoosterType.MCMMO, rate, durationMillis, announce, "", "");
        setState(state);
        return state;
    }

    private BoosterState startTimedJobs(String target, String scope, long durationMillis, double rate) {
        dispatchConsole("jobs boost " + target + " " + scope + " "
                + DurationUtil.formatCompactDuration(durationMillis) + " " + NumberUtil.formatRate(rate));
        BoosterState state = createTimedState(BoosterType.JOBS, rate, durationMillis, false, target, scope);
        setState(state);
        return state;
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

        boolean announceOnRestore = plugin.getConfig().getBoolean("mcmmo.announceOnRestore", false) && state.announceOnStart();
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
    }

    private void expireIfNeeded(BoosterType type) {
        BoosterState state = states.getOrDefault(type, BoosterState.inactive(type));
        if (!state.isExpired(System.currentTimeMillis())) {
            return;
        }

        if (type == BoosterType.MCMMO && isDependencyAvailable(type)) {
            dispatchConsole("xprate reset");
        }

        clearState(type);
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

        if (type == BoosterType.MCMMO && isDependencyAvailable(type)) {
            dispatchConsole("xprate reset");
        }

        clearState(type);
    }

    private void cancelExpiry(BoosterType type) {
        BukkitTask existing = expiryTasks.remove(type);
        if (existing != null) {
            existing.cancel();
        }
    }

    private void dispatchConsole(String commandNoSlash) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandNoSlash);
    }
}
