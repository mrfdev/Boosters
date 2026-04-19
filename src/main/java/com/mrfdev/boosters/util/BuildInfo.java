package com.mrfdev.boosters.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record BuildInfo(
        String pluginName,
        String pluginVersion,
        String buildNumber,
        String targetMinecraftVersion,
        String targetJavaVersion,
        String artifactFileName
) {

    public static BuildInfo load(JavaPlugin plugin) {
        Properties properties = new Properties();
        try (InputStream inputStream = plugin.getResource("build-info.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not load build-info.properties: " + exception.getMessage());
        }

        String fallbackName = plugin.getName();
        String fallbackVersion = plugin.getDescription().getVersion();
        String pluginName = properties.getProperty("pluginName", fallbackName);
        String pluginVersion = properties.getProperty("pluginVersion", fallbackVersion);
        String buildNumber = properties.getProperty("buildNumber", "unknown");
        String targetMinecraftVersion = properties.getProperty("targetMinecraftVersion", "unknown");
        String targetJavaVersion = properties.getProperty("targetJavaVersion", "unknown");
        String artifactFileName = properties.getProperty("artifactFileName", pluginName + "-v" + pluginVersion + ".jar");

        return new BuildInfo(pluginName, pluginVersion, buildNumber, targetMinecraftVersion, targetJavaVersion, artifactFileName);
    }
}
