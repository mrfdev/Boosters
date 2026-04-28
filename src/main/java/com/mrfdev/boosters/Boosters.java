package com.mrfdev.boosters;

import com.mrfdev.boosters.command.RateCommand;
import com.mrfdev.boosters.listener.ExternalCommandListener;
import com.mrfdev.boosters.placeholder.BoostersPlaceholderExpansion;
import com.mrfdev.boosters.service.BoosterService;
import com.mrfdev.boosters.service.PyroWelcomesPointsProvider;
import com.mrfdev.boosters.storage.BoosterStateStorage;
import com.mrfdev.boosters.util.BuildInfo;
import com.mrfdev.boosters.util.LocaleService;
import com.mrfdev.boosters.util.MessageService;
import com.mrfdev.boosters.util.PluginConfigService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.stream.Stream;

public final class Boosters extends JavaPlugin {

    private BoosterService boosterService;
    private BoostersPlaceholderExpansion placeholderExpansion;
    private BuildInfo buildInfo;
    private LocaleService localeService;
    private MessageService messageService;
    private PluginConfigService pluginConfigService;

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        saveDefaultConfig();

        buildInfo = BuildInfo.load(this);
        localeService = new LocaleService(this);
        messageService = new MessageService(localeService);
        BoosterStateStorage storage = new BoosterStateStorage(this);
        PyroWelcomesPointsProvider pointsProvider = new PyroWelcomesPointsProvider(this);
        boosterService = new BoosterService(this, storage, messageService, pointsProvider, localeService);

        RateCommand rateCommand = new RateCommand(this, buildInfo, boosterService, messageService, localeService);
        PluginCommand rate = Objects.requireNonNull(getCommand("rate"), "The /rate command is missing from plugin.yml");
        rate.setExecutor(rateCommand);
        rate.setTabCompleter(rateCommand);

        Bukkit.getPluginManager().registerEvents(new ExternalCommandListener(boosterService), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new BoostersPlaceholderExpansion(this, boosterService);
            placeholderExpansion.register();
            messageService.info(this, "<gray>PlaceholderAPI support has been enabled.</gray>");
        }

        boosterService.logStartupSelfCheck();
        long delay = Math.max(1L, getConfig().getLong("restore.delayTicks", 60L));
        Bukkit.getScheduler().runTaskLater(this, boosterService::restoreTrackedBoosters, delay);
        messageService.info(this, "<gray>Booster tracking is ready. Restore check runs in <yellow><delay></yellow>.</gray>",
                MessageService.value("delay", delay + " ticks"));
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }

        if (boosterService != null) {
            boosterService.shutdown();
            boosterService = null;
        }
    }

    public void reloadRuntimeConfiguration() {
        reloadConfig();
        if (localeService != null) {
            localeService.reload();
        }
        if (boosterService != null) {
            boosterService.reloadRuntimeSettings();
        }
    }

    @Override
    public void saveDefaultConfig() {
        if (pluginConfigService == null) {
            pluginConfigService = new PluginConfigService(this);
        }
        pluginConfigService.saveDefaultConfig();
    }

    @Override
    public FileConfiguration getConfig() {
        if (pluginConfigService == null) {
            pluginConfigService = new PluginConfigService(this);
        }
        return pluginConfigService.getConfig();
    }

    @Override
    public void reloadConfig() {
        if (pluginConfigService == null) {
            pluginConfigService = new PluginConfigService(this);
        }
        pluginConfigService.reloadConfig();
    }

    @Override
    public void saveConfig() {
        if (pluginConfigService == null) {
            pluginConfigService = new PluginConfigService(this);
        }
        pluginConfigService.saveConfig();
    }

    private void migrateLegacyDataFolder() {
        Path newFolder = getDataFolder().toPath();
        if (Files.exists(newFolder)) {
            return;
        }

        Path pluginsFolder = newFolder.getParent();
        if (pluginsFolder == null) {
            return;
        }

        for (String legacyName : new String[]{"Boosters", "boosters"}) {
            Path legacyFolder = pluginsFolder.resolve(legacyName);
            if (!Files.exists(legacyFolder) || legacyFolder.equals(newFolder)) {
                continue;
            }

            try {
                Files.createDirectories(newFolder.getParent());
                try {
                    Files.move(legacyFolder, newFolder, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException moveFailure) {
                    copyDirectoryContents(legacyFolder, newFolder);
                }
                getLogger().info("Migrated legacy data folder from " + legacyFolder + " to " + newFolder);
                return;
            } catch (IOException exception) {
                getLogger().warning("Could not migrate legacy data folder from " + legacyFolder + " to " + newFolder + ": " + exception.getMessage());
            }
        }
    }

    private void copyDirectoryContents(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path sourcePath : paths.toList()) {
                Path relative = source.relativize(sourcePath);
                Path targetPath = target.resolve(relative);
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                    continue;
                }
                Files.createDirectories(targetPath.getParent());
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
