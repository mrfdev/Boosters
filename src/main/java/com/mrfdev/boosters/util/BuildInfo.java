package com.mrfdev.boosters.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record BuildInfo(
        String pluginName,
        String pluginVersion,
        String buildNumber,
        String compilePaperApiVersion,
        String declaredApiCompatibilityVersion,
        String pluginYamlApiVersion,
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
        String fallbackVersion = plugin.getPluginMeta().getVersion();
        String pluginName = properties.getProperty("pluginName", fallbackName);
        String pluginVersion = properties.getProperty("pluginVersion", fallbackVersion);
        String buildNumber = properties.getProperty("buildNumber", "unknown");
        String compilePaperApiVersion = properties.getProperty("compilePaperApiVersion", "unknown");
        String declaredApiCompatibilityVersion = properties.getProperty("declaredApiCompatibilityVersion", "unknown");
        String pluginYamlApiVersion = properties.getProperty("pluginYamlApiVersion", declaredApiCompatibilityVersion);
        String targetJavaVersion = properties.getProperty("targetJavaVersion", "unknown");
        String artifactFileName = properties.getProperty("artifactFileName", pluginName + "-v" + pluginVersion + ".jar");

        return new BuildInfo(pluginName, pluginVersion, buildNumber, compilePaperApiVersion, declaredApiCompatibilityVersion, pluginYamlApiVersion, targetJavaVersion, artifactFileName);
    }
}
