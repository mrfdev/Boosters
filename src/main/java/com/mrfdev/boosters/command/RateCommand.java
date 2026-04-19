package com.mrfdev.boosters.command;

import com.mrfdev.boosters.model.BoosterState;
import com.mrfdev.boosters.model.BoosterType;
import com.mrfdev.boosters.service.BoosterService;
import com.mrfdev.boosters.util.DurationUtil;
import com.mrfdev.boosters.util.MessageService;
import com.mrfdev.boosters.util.NumberUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class RateCommand implements TabExecutor {

    private static final List<String> ROOT_ARGUMENTS = List.of("start", "stop");
    private static final List<String> START_TARGETS = List.of("mcmmo", "jobs");
    private static final List<String> STOP_TARGETS = List.of("mcmmo", "jobs", "all");
    private static final List<String> COMMON_DURATIONS = List.of("15m", "30m", "1h", "2h");
    private static final List<String> COMMON_RATES = List.of("2", "2.5", "3");

    private final BoosterService boosterService;
    private final MessageService messageService;

    public RateCommand(BoosterService boosterService, MessageService messageService) {
        this.boosterService = boosterService;
        this.messageService = messageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasViewPermission(sender)) {
            messageService.prefixed(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        if (args.length == 0) {
            sendStatus(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
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
            messageService.prefixed(sender, "<red>You need <yellow>onemb.booster.admin</yellow> to start boosters.</red>");
            return true;
        }

        if (args.length < 4) {
            sendStartSynopsis(sender);
            return true;
        }

        Optional<BoosterType> optionalType = BoosterType.fromInput(args[1]);
        if (optionalType.isEmpty()) {
            sendStartSynopsis(sender);
            return true;
        }

        BoosterType type = optionalType.get();
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

    private boolean handleStop(CommandSender sender, String[] args) {
        if (!hasAdminPermission(sender)) {
            messageService.prefixed(sender, "<red>You need <yellow>onemb.booster.admin</yellow> to stop boosters.</red>");
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
                "<yellow>/rate start <mcmmo|jobs> <time> <rate></yellow><gray> - Start a tracked booster.</gray>");
        messageService.send(sender,
                "<yellow>/rate stop <mcmmo|jobs|all></yellow><gray> - Stop tracked boosters.</gray>");
    }

    private void sendStartSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage: <yellow>/rate start <mcmmo|jobs> <time> <rate></yellow></gray>");
        messageService.send(sender, "<gray>Examples: <yellow>/rate start mcmmo 1h 2</yellow> and <yellow>/rate start jobs 30m 2.5</yellow></gray>");
    }

    private void sendStopSynopsis(CommandSender sender) {
        messageService.prefixed(sender, "<gray>Usage: <yellow>/rate stop <mcmmo|jobs|all></yellow></gray>");
        messageService.send(sender, "<gray>Examples: <yellow>/rate stop mcmmo</yellow> and <yellow>/rate stop all</yellow></gray>");
    }

    private boolean hasViewPermission(CommandSender sender) {
        return sender.isOp()
                || sender.hasPermission("onemb.booster.rate")
                || sender.hasPermission("boosters.rate")
                || sender.hasPermission("onemb.booster.admin")
                || sender.hasPermission("boosters.admin");
    }

    private boolean hasAdminPermission(CommandSender sender) {
        return sender.isOp()
                || sender.hasPermission("onemb.booster.admin")
                || sender.hasPermission("boosters.admin");
    }

    private List<String> partialMatches(String token, List<String> options) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, matches);
        return matches;
    }
}
