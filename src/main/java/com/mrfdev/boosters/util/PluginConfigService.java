package com.mrfdev.boosters.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public final class PluginConfigService {

    private final JavaPlugin plugin;
    private final File configFile;
    private YamlConfiguration config;

    public PluginConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void saveDefaultConfig() {
        if (!configFile.isFile()) {
            loadAndUpdate();
            save();
        } else if (config == null) {
            loadAndUpdate();
        }
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            loadAndUpdate();
        }
        return config;
    }

    public void reloadConfig() {
        loadAndUpdate();
    }

    public void saveConfig() {
        save();
    }

    private void loadAndUpdate() {
        ensureDataFolder();
        YamlConfiguration loaded = loadYaml(configFile);
        boolean changed = applyDefaultsAndComments(loaded);
        this.config = loaded;
        if (changed) {
            save();
        }
    }

    private void save() {
        ensureDataFolder();
        if (config == null) {
            config = new YamlConfiguration();
            applyDefaultsAndComments(config);
        }
        try {
            config.save(configFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save config.yml: " + exception.getMessage());
        }
    }

    private boolean applyDefaultsAndComments(YamlConfiguration yaml) {
        boolean changed = false;
        yaml.options().parseComments(true);
        changed |= setHeaderIfMissing(yaml, List.of(
                "1MB Boosters configuration.",
                "This file controls tracked booster integrations, /rate output, debug options, and optional hook commands.",
                "Most changes can be applied with /rate reload or via the plugin's debug config commands.",
                "Runtime booster state is stored separately in booster-state.yml."
        ));

        changed |= ensureValue(yaml, "locale.file", "Locale_EN.yml");
        changed |= ensureComments(yaml, "locale", List.of(
                "Locale file settings.",
                "These control player-facing wording and MiniMessage text loaded from plugins/1MB-Boosters/.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "locale.file", List.of(
                "Selects the locale file used for player-facing messages.",
                "Default: Locale_EN.yml",
                "Expected value: a bundled locale filename such as Locale_EN.yml in plugins/1MB-Boosters/.",
                "Reload behavior: takes effect after /rate reload or a full server restart."
        ));

        changed |= ensureValue(yaml, "restore.delayTicks", 60);
        changed |= ensureComments(yaml, "restore", List.of(
                "Startup restore behavior.",
                "These settings control how long Boosters waits before checking stored booster state after startup.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "restore.delayTicks", List.of(
                "Delay in server ticks before stored boosters are restored after startup.",
                "Default: 60",
                "Expected values: positive whole numbers. 20 ticks = 1 second.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        changed |= ensureValue(yaml, "features.mcmmo.enabled", true);
        changed |= ensureValue(yaml, "features.jobs.enabled", true);
        changed |= ensureValue(yaml, "features.points.enabled", true);
        changed |= ensureValue(yaml, "features.points.visible", true);
        changed |= ensureValue(yaml, "features.points.experimental", true);
        changed |= ensureComments(yaml, "features", List.of(
                "Feature toggles for each supported integration.",
                "Disabling a feature hides its logic from /rate and prevents tracked actions for that integration.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "features.mcmmo", List.of("mcMMO integration toggle settings."));
        changed |= ensureComments(yaml, "features.mcmmo.enabled", List.of(
                "Enables tracked mcMMO booster support and mcMMO sections in /rate output.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "features.jobs", List.of("Jobs integration toggle settings."));
        changed |= ensureComments(yaml, "features.jobs.enabled", List.of(
                "Enables tracked Jobs booster support and Jobs sections in /rate output.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "features.points", List.of("PyroWelcomesPro points integration settings."));
        changed |= ensureComments(yaml, "features.points.enabled", List.of(
                "Enables the PyroWelcomesPro points detection backend.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "features.points.visible", List.of(
                "Controls whether points appear in normal player /rate output and public placeholders.",
                "Detected active Points boosters are shown to players even if this is false, so /rate does not hide live manual boosts.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "features.points.experimental", List.of(
                "Marks points as experimental in admin/debug output.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        changed |= ensureValue(yaml, "display.adminShowExperimentalIntegrations", true);
        changed |= ensureValue(yaml, "display.sections.mcmmo", true);
        changed |= ensureValue(yaml, "display.sections.jobs", true);
        changed |= ensureValue(yaml, "display.sections.points", true);
        changed |= ensureValue(yaml, "display.labels.mcmmo", "<yellow>mcMMO</yellow>");
        changed |= ensureValue(yaml, "display.labels.jobs", "<yellow>Jobs</yellow>");
        changed |= ensureValue(yaml, "display.labels.points", "<yellow>Points</yellow>");
        changed |= ensureComments(yaml, "display", List.of(
                "Display settings for /rate and related output.",
                "MiniMessage is supported in labels where color formatting makes sense.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "display.adminShowExperimentalIntegrations", List.of(
                "Lets admins see hidden experimental integrations in /rate while keeping them hidden from normal players.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "display.sections", List.of(
                "Toggles entire /rate output sections on or off."
        ));
        changed |= ensureComments(yaml, "display.sections.mcmmo", List.of(
                "Shows or hides the mcMMO section in /rate output.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "display.sections.jobs", List.of(
                "Shows or hides the Jobs section in /rate output.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "display.sections.points", List.of(
                "Shows or hides the Points section in /rate output when points are otherwise visible.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "display.labels", List.of(
                "MiniMessage labels used in /rate output and some debug views."
        ));
        changed |= ensureComments(yaml, "display.labels.mcmmo", List.of(
                "Display label used for mcMMO in player/admin output.",
                "Default: <yellow>mcMMO</yellow>",
                "Expected value: any short MiniMessage string.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "display.labels.jobs", List.of(
                "Display label used for Jobs in player/admin output.",
                "Default: <yellow>Jobs</yellow>",
                "Expected value: any short MiniMessage string.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "display.labels.points", List.of(
                "Display label used for Points in player/admin output.",
                "Default: <yellow>Points</yellow>",
                "Expected value: any short MiniMessage string.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        changed |= ensureValue(yaml, "tabCompletion.commonDurations", List.of("15m", "30m", "1h", "2h", "3h"));
        changed |= ensureValue(yaml, "tabCompletion.commonRates", List.of("2", "2.5", "3", "4"));
        changed |= ensureComments(yaml, "tabCompletion", List.of(
                "Suggested values used by /rate tab completion.",
                "These are convenience suggestions only and do not hard-limit what admins can type.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "tabCompletion.commonDurations", List.of(
                "Common duration suggestions for /rate start tab completion.",
                "Default: [15m, 30m, 1h, 2h, 3h]",
                "Expected values: strings using formats like 15m, 1h, 2h30m.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "tabCompletion.commonRates", List.of(
                "Common multiplier suggestions for /rate start tab completion.",
                "Default: [2, 2.5, 3, 4]",
                "Expected values: numeric strings greater than 1.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        changed |= ensureValue(yaml, "logging.auditToConsole", true);
        changed |= ensureValue(yaml, "logging.recentActionLimit", 25);
        changed |= ensureComments(yaml, "logging", List.of(
                "Audit logging and debug history settings.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "logging.auditToConsole", List.of(
                "Writes tracked booster start, stop, and restore actions to the server console.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "logging.recentActionLimit", List.of(
                "Maximum number of in-memory audit entries shown by /rate debug logs.",
                "Default: 25",
                "Expected values: positive whole numbers. Very low values may drop history quickly.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        changed |= ensureValue(yaml, "commandHooks.start.global", List.of(
                "cmi broadcast Booster {booster} {rate}x started for {duration} by {sender}",
                "cmi titlemsg all Booster Started \\n {booster} {rate}x for {duration}"
        ));
        changed |= ensureValue(yaml, "commandHooks.start.mcmmo", List.of());
        changed |= ensureValue(yaml, "commandHooks.start.jobs", List.of());
        changed |= ensureValue(yaml, "commandHooks.start.points", List.of());
        changed |= ensureValue(yaml, "commandHooks.stop.global", List.of(
                "cmi broadcast Booster {booster} stopped by {sender}",
                "cmi titlemsg all Booster Stopped \\n {booster} ended after {duration}"
        ));
        changed |= ensureValue(yaml, "commandHooks.stop.mcmmo", List.of());
        changed |= ensureValue(yaml, "commandHooks.stop.jobs", List.of());
        changed |= ensureValue(yaml, "commandHooks.stop.points", List.of());
        changed |= ensureComments(yaml, "commandHooks", List.of(
                "Console command hooks that run after tracked booster start or stop actions.",
                "Boosters sends these commands raw; target plugins can handle MiniMessage, hex colors, titles, bossbars, sounds, or particles themselves.",
                "Available placeholders: {phase}, {booster}, {booster_key}, {booster_label}, {rate}, {duration}, {duration_compact}, {remaining}, {remaining_compact}, {sender}, {jobs_target}, {jobs_scope}, {ingame}, {discord}.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "commandHooks.start", List.of(
                "Commands that run when a tracked booster starts.",
                "Use empty lists to disable individual hook groups."
        ));
        changed |= ensureComments(yaml, "commandHooks.start.global", List.of(
                "Runs for every booster type when a tracked booster starts.",
                "Default: two example CMI commands.",
                "Expected values: a YAML list of console command strings. Leading slashes are optional.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "commandHooks.start.mcmmo", List.of(
                "Runs only when a tracked mcMMO booster starts.",
                "Default: []",
                "Expected values: a YAML list of console command strings.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "commandHooks.start.jobs", List.of(
                "Runs only when a tracked Jobs booster starts.",
                "Default: []",
                "Expected values: a YAML list of console command strings.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "commandHooks.start.points", List.of(
                "Reserved for future controlled Points booster starts.",
                "Current behavior: Points boosters are detected from PyroWelcomesPro config.yml only, so this does not run yet.",
                "Default: []",
                "Expected values: a YAML list of console command strings.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "commandHooks.stop", List.of(
                "Commands that run when a tracked booster stops or expires."
        ));
        changed |= ensureComments(yaml, "commandHooks.stop.global", List.of(
                "Runs for every booster type when a tracked booster stops.",
                "Default: two example CMI commands.",
                "Expected values: a YAML list of console command strings. Leading slashes are optional.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "commandHooks.stop.mcmmo", List.of(
                "Runs only when a tracked mcMMO booster stops.",
                "Default: []",
                "Expected values: a YAML list of console command strings.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "commandHooks.stop.jobs", List.of(
                "Runs only when a tracked Jobs booster stops.",
                "Default: []",
                "Expected values: a YAML list of console command strings.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "commandHooks.stop.points", List.of(
                "Reserved for future controlled Points booster stops.",
                "Current behavior: Points boosters are detected from PyroWelcomesPro config.yml only, so this does not run yet.",
                "Default: []",
                "Expected values: a YAML list of console command strings.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        changed |= ensureValue(yaml, "broadcasts.start.global", "");
        changed |= ensureValue(yaml, "broadcasts.start.mcmmo", "<gold>[Boosters]</gold> <yellow>mcMMO</yellow> has started at <aqua><rate>x</aqua> for <aqua><duration></aqua><gray> by <white><sender></white>.</gray>");
        changed |= ensureValue(yaml, "broadcasts.start.jobs", "<gold>[Boosters]</gold> <yellow>Jobs</yellow> has started at <aqua><rate>x</aqua> for <aqua><duration></aqua><gray> by <white><sender></white>.</gray>");
        changed |= ensureValue(yaml, "broadcasts.start.points", "<gold>[Boosters]</gold> <yellow>Points</yellow> has started at <aqua><rate>x</aqua> for <aqua><duration></aqua><gray> by <white><sender></white>. Values: <aqua><ingame></aqua> in-game, <aqua><discord></aqua> Discord.</gray>");
        changed |= ensureValue(yaml, "broadcasts.stop.global", "");
        changed |= ensureValue(yaml, "broadcasts.stop.mcmmo", "<gold>[Boosters]</gold> <yellow>mcMMO</yellow> has been stopped<gray> by <white><sender></white>.</gray>");
        changed |= ensureValue(yaml, "broadcasts.stop.jobs", "<gold>[Boosters]</gold> <yellow>Jobs</yellow> has been stopped<gray> by <white><sender></white>.</gray>");
        changed |= ensureValue(yaml, "broadcasts.stop.points", "<gold>[Boosters]</gold> <yellow>Points</yellow> has been stopped<gray> by <white><sender></white>.</gray>");
        changed |= ensureComments(yaml, "broadcasts", List.of(
                "MiniMessage broadcast templates sent by Boosters itself.",
                "Leave a value blank to disable that line. Available placeholders include <booster>, <rate>, <duration>, <sender>, <ingame>, and <discord>.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "broadcasts.start", List.of("Broadcast lines used when tracked boosters start."));
        changed |= ensureComments(yaml, "broadcasts.start.global", List.of(
                "Broadcast sent for every booster type when one starts.",
                "Default: empty string, which disables this broadcast.",
                "Expected values: a single MiniMessage string or blank.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "broadcasts.start.mcmmo", List.of(
                "Broadcast sent when a tracked mcMMO booster starts.",
                "Default: bundled MiniMessage line.",
                "Expected values: a single MiniMessage string or blank.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "broadcasts.start.jobs", List.of(
                "Broadcast sent when a tracked Jobs booster starts.",
                "Default: bundled MiniMessage line.",
                "Expected values: a single MiniMessage string or blank.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "broadcasts.start.points", List.of(
                "Reserved broadcast for future controlled Points booster starts.",
                "Current behavior: manual Points detection does not fire start broadcasts.",
                "Default: bundled MiniMessage line.",
                "Expected values: a single MiniMessage string or blank.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "broadcasts.stop", List.of("Broadcast lines used when tracked boosters stop."));
        changed |= ensureComments(yaml, "broadcasts.stop.global", List.of(
                "Broadcast sent for every booster type when one stops.",
                "Default: empty string, which disables this broadcast.",
                "Expected values: a single MiniMessage string or blank.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "broadcasts.stop.mcmmo", List.of(
                "Broadcast sent when a tracked mcMMO booster stops.",
                "Default: bundled MiniMessage line.",
                "Expected values: a single MiniMessage string or blank.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "broadcasts.stop.jobs", List.of(
                "Broadcast sent when a tracked Jobs booster stops.",
                "Default: bundled MiniMessage line.",
                "Expected values: a single MiniMessage string or blank.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "broadcasts.stop.points", List.of(
                "Reserved broadcast for future controlled Points booster stops.",
                "Current behavior: manual Points detection does not fire stop broadcasts.",
                "Default: bundled MiniMessage line.",
                "Expected values: a single MiniMessage string or blank.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        changed |= ensureValue(yaml, "mcmmo.announceOnRateStart", true);
        changed |= ensureValue(yaml, "mcmmo.announceOnRestore", false);
        changed |= ensureComments(yaml, "mcmmo", List.of(
                "mcMMO-specific behavior toggles.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "mcmmo.announceOnRateStart", List.of(
                "When true, /rate start mcmmo ... uses mcMMO's announce mode when starting the booster.",
                "Default: true",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "mcmmo.announceOnRestore", List.of(
                "When true, restored mcMMO boosters announce again after startup restore.",
                "Default: false",
                "Expected values: true or false.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        changed |= ensureComments(yaml, "jobs", List.of(
                "Jobs integration notes.",
                "Jobs behavior is enabled or disabled through features.jobs.enabled above."
        ));

        changed |= ensureValue(yaml, "points.pluginName", "PyroWelcomesPro");
        changed |= ensureValue(yaml, "points.configFile", "PyroWelcomesPro/config.yml");
        changed |= ensureValue(yaml, "points.reloadCommand", "welcomes reload");
        changed |= ensureValue(yaml, "points.ingamePath", "Settings.EarnablePoints");
        changed |= ensureValue(yaml, "points.discordPath", "Settings.DiscordSRV.EarnablePoints");
        changed |= ensureValue(yaml, "points.baseIngamePoints", 2);
        changed |= ensureValue(yaml, "points.baseDiscordPoints", 1);
        changed |= ensureComments(yaml, "points", List.of(
                "PyroWelcomesPro points integration settings.",
                "Current behavior: read-only/manual detection. Boosters reads PyroWelcomesPro config.yml and infers an active booster when configured points are above the base values.",
                "Boosters does not write, stop, or reload PyroWelcomesPro while that plugin's reload command does not re-read these values reliably.",
                "Changes to this section apply after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "points.pluginName", List.of(
                "Bukkit plugin name used to detect the PyroWelcomesPro dependency.",
                "Default: PyroWelcomesPro",
                "Expected value: the exact plugin name shown by the server plugin manager.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "points.configFile", List.of(
                "Relative path from the plugins folder to PyroWelcomesPro's config.yml file.",
                "Default: PyroWelcomesPro/config.yml",
                "Expected value: a relative file path under plugins/.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "points.reloadCommand", List.of(
                "Reserved reload command for a future controlled Points mode.",
                "Current behavior: this is not run because Points detection is read-only/manual.",
                "Default: welcomes reload",
                "Expected values: a valid console command string or blank to skip reloading the target plugin.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "points.ingamePath", List.of(
                "YAML path inside the target config for in-game welcome points.",
                "Default: Settings.EarnablePoints",
                "Expected values: a valid YAML path string in the target plugin config.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "points.discordPath", List.of(
                "YAML path inside the target config for Discord welcome points.",
                "Default: Settings.DiscordSRV.EarnablePoints",
                "Expected values: a valid YAML path string in the target plugin config.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "points.baseIngamePoints", List.of(
                "Base in-game welcome points used to infer manual Points boosters.",
                "If PyroWelcomesPro's configured value is above this base, /rate reports a Points booster.",
                "Default: 2",
                "Expected values: non-negative whole numbers.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));
        changed |= ensureComments(yaml, "points.baseDiscordPoints", List.of(
                "Base Discord welcome points used to infer manual Points boosters.",
                "If PyroWelcomesPro's configured value is above this base, /rate reports a Points booster.",
                "Default: 1",
                "Expected values: non-negative whole numbers.",
                "Reload behavior: takes effect after /rate reload or a server restart."
        ));

        return changed;
    }

    private boolean ensureValue(YamlConfiguration yaml, String path, Object defaultValue) {
        if (yaml.contains(path)) {
            return false;
        }
        yaml.set(path, defaultValue);
        return true;
    }

    private boolean ensureComments(YamlConfiguration yaml, String path, List<String> comments) {
        List<String> currentComments = yaml.getComments(path);
        if (currentComments != null && !currentComments.isEmpty()) {
            return false;
        }
        yaml.setComments(path, comments);
        return true;
    }

    private boolean setHeaderIfMissing(YamlConfiguration yaml, List<String> header) {
        List<String> currentHeader = yaml.options().getHeader();
        if (currentHeader != null && !currentHeader.isEmpty()) {
            return false;
        }
        yaml.options().setHeader(header);
        return true;
    }

    private YamlConfiguration loadYaml(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        if (!file.isFile()) {
            return yaml;
        }

        try {
            yaml.loadFromString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().warning("Could not read config.yml: " + exception.getMessage());
        }
        return yaml;
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create the plugin data folder.");
        }
    }
}
