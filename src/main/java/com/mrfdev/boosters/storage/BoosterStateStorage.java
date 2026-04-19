package com.mrfdev.boosters.storage;

import com.mrfdev.boosters.model.BoosterState;
import com.mrfdev.boosters.model.BoosterType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public final class BoosterStateStorage {

    private final JavaPlugin plugin;
    private final File stateFile;

    public BoosterStateStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "booster-state.yml");
    }

    public EnumMap<BoosterType, BoosterState> loadStates() {
        ensureDataFolder();

        EnumMap<BoosterType, BoosterState> loaded = new EnumMap<>(BoosterType.class);
        if (!stateFile.exists()) {
            for (BoosterType type : BoosterType.values()) {
                loaded.put(type, BoosterState.inactive(type));
            }
            saveStates(loaded);
            return loaded;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        for (BoosterType type : BoosterType.values()) {
            loaded.put(type, readState(yaml.getConfigurationSection(type.key()), type));
        }
        return loaded;
    }

    public void saveStates(Map<BoosterType, BoosterState> states) {
        ensureDataFolder();

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().header("""
                Runtime booster state for 1MB Boosters.
                This file is managed automatically.
                """);

        for (BoosterType type : BoosterType.values()) {
            BoosterState state = states.getOrDefault(type, BoosterState.inactive(type));
            ConfigurationSection section = yaml.createSection(type.key());
            section.set("active", state.active());
            section.set("rate", state.rate());
            section.set("startedAtEpochMillis", state.startedAtEpochMillis());
            section.set("durationMillis", state.durationMillis());
            section.set("endsAtEpochMillis", state.endsAtEpochMillis());
            section.set("announceOnStart", state.announceOnStart());
            section.set("jobsTarget", state.jobsTarget());
            section.set("jobsScope", state.jobsScope());
        }

        try {
            yaml.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save booster-state.yml: " + exception.getMessage());
        }
    }

    private BoosterState readState(ConfigurationSection section, BoosterType type) {
        if (section == null || !section.getBoolean("active", false)) {
            return BoosterState.inactive(type);
        }

        double rate = section.getDouble("rate", 1.0D);
        if (rate <= 1.0D) {
            return BoosterState.inactive(type);
        }

        long startedAt = section.getLong("startedAtEpochMillis", 0L);
        long duration = section.getLong("durationMillis", 0L);
        long endsAt = section.getLong("endsAtEpochMillis", 0L);
        boolean announceOnStart = section.getBoolean("announceOnStart", false);
        String jobsTarget = section.getString("jobsTarget", "");
        String jobsScope = section.getString("jobsScope", "");

        if (type == BoosterType.JOBS && (duration <= 0L || endsAt <= 0L)) {
            return BoosterState.inactive(type);
        }

        return new BoosterState(type, true, rate, startedAt, duration, endsAt, announceOnStart, jobsTarget, jobsScope);
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create the plugin data folder.");
        }
    }
}
