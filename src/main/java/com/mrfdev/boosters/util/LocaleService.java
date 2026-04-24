package com.mrfdev.boosters.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class LocaleService {

    private static final String DEFAULT_LOCALE_FILE = "Locale_EN.yml";

    private final JavaPlugin plugin;
    private YamlConfiguration localeConfig;
    private String activeLocaleFile = DEFAULT_LOCALE_FILE;

    public LocaleService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String requested = plugin.getConfig().getString("locale.file", DEFAULT_LOCALE_FILE);
        if (requested == null || requested.isBlank()) {
            requested = DEFAULT_LOCALE_FILE;
        }

        if (!resourceExists(requested)) {
            requested = DEFAULT_LOCALE_FILE;
        }

        saveResourceIfMissing(requested);

        File localeFile = new File(plugin.getDataFolder(), requested);
        if (!localeFile.exists()) {
            requested = DEFAULT_LOCALE_FILE;
            saveResourceIfMissing(requested);
            localeFile = new File(plugin.getDataFolder(), requested);
        }

        activeLocaleFile = requested;
        localeConfig = loadYaml(localeFile);
    }

    public String get(String path, String fallback) {
        if (localeConfig == null) {
            return fallback;
        }
        return localeConfig.getString(path, fallback);
    }

    public String activeLocaleFile() {
        return activeLocaleFile;
    }

    private boolean resourceExists(String resourceName) {
        return plugin.getResource(resourceName) != null;
    }

    private void saveResourceIfMissing(String resourceName) {
        File file = new File(plugin.getDataFolder(), resourceName);
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
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
