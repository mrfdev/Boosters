package com.mrfdev.boosters;

import com.mrfdev.boosters.command.RateCommand;
import com.mrfdev.boosters.listener.ExternalCommandListener;
import com.mrfdev.boosters.placeholder.BoostersPlaceholderExpansion;
import com.mrfdev.boosters.service.BoosterService;
import com.mrfdev.boosters.storage.BoosterStateStorage;
import com.mrfdev.boosters.util.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class Boosters extends JavaPlugin {

    private BoosterService boosterService;
    private BoostersPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        MessageService messageService = new MessageService();
        BoosterStateStorage storage = new BoosterStateStorage(this);
        boosterService = new BoosterService(this, storage, messageService);

        RateCommand rateCommand = new RateCommand(boosterService, messageService);
        PluginCommand rate = Objects.requireNonNull(getCommand("rate"), "The /rate command is missing from plugin.yml");
        rate.setExecutor(rateCommand);
        rate.setTabCompleter(rateCommand);

        Bukkit.getPluginManager().registerEvents(new ExternalCommandListener(boosterService), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderExpansion = new BoostersPlaceholderExpansion(this, boosterService);
            placeholderExpansion.register();
            messageService.info(this, "<gray>PlaceholderAPI support has been enabled.</gray>");
        }

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
}
