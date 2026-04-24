package com.mrfdev.boosters.service;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class PyroWelcomesPointsProvider {

    private final JavaPlugin plugin;

    public PyroWelcomesPointsProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String pluginName() {
        return plugin.getConfig().getString("points.pluginName", "PyroWelcomesPro");
    }

    public boolean isDependencyEnabled() {
        return plugin.getServer().getPluginManager().isPluginEnabled(pluginName());
    }

    public File configFile() {
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        String relativePath = plugin.getConfig().getString("points.configFile", "PyroWelcomesPro/config.yml");
        return new File(pluginsFolder, relativePath);
    }

    public boolean isConfigPresent() {
        return configFile().isFile();
    }

    public String reloadCommand() {
        return plugin.getConfig().getString("points.reloadCommand", "welcomes reload");
    }

    public String ingamePath() {
        return plugin.getConfig().getString("points.ingamePath", "Settings.EarnablePoints");
    }

    public String discordPath() {
        return plugin.getConfig().getString("points.discordPath", "Settings.DiscordSRV.EarnablePoints");
    }

    public int configuredBaseIngamePoints() {
        return Math.max(0, plugin.getConfig().getInt("points.baseIngamePoints", 2));
    }

    public int configuredBaseDiscordPoints() {
        return Math.max(0, plugin.getConfig().getInt("points.baseDiscordPoints", 1));
    }

    public int currentIngamePoints() {
        return readCurrentValue(ingamePath(), configuredBaseIngamePoints());
    }

    public int currentDiscordPoints() {
        return readCurrentValue(discordPath(), configuredBaseDiscordPoints());
    }

    public String applyMultiplier(int multiplier) {
        return writeValues(configuredBaseIngamePoints() * multiplier, configuredBaseDiscordPoints() * multiplier);
    }

    public String resetToBase() {
        return writeValues(configuredBaseIngamePoints(), configuredBaseDiscordPoints());
    }

    private int readCurrentValue(String path, int fallback) {
        if (!isConfigPresent()) {
            return fallback;
        }

        YamlConfiguration yaml = loadYaml(configFile());
        return Math.max(0, yaml.getInt(path, fallback));
    }

    private String writeValues(int ingamePoints, int discordPoints) {
        File file = configFile();
        if (!file.isFile()) {
            return "PyroWelcomesPro config.yml could not be found.";
        }

        YamlConfiguration yaml = loadYaml(file);
        yaml.set(ingamePath(), ingamePoints);
        yaml.set(discordPath(), discordPoints);

        try {
            yaml.save(file);
        } catch (IOException exception) {
            return "Could not save PyroWelcomesPro config.yml: " + exception.getMessage();
        }

        String reloadCommand = reloadCommand();
        if (reloadCommand == null || reloadCommand.isBlank()) {
            return null;
        }

        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reloadCommand);
        if (!dispatched) {
            return "The configured welcomes reload command did not run successfully.";
        }

        return null;
    }

    private YamlConfiguration loadYaml(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (!file.isFile()) {
            return yaml;
        }

        try {
            yaml.loadFromString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().warning("Could not read " + file.getName() + ": " + exception.getMessage());
        }
        return yaml;
    }
}
