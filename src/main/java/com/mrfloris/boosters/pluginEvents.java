package com.mrfloris.boosters;

import com.mrfloris.boosters.commands.RateCommand;
import com.mrfloris.boosters.events.PlayerCommandPreprocess;
import com.mrfloris.boosters.events.ServerCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jspecify.annotations.NonNull;

public class pluginEvents extends JavaPlugin {

    public static String prefix;
    public static String isInactive;
    public static String isActive;
    @SuppressWarnings("unused")
    public static Boolean isDebug;
    public FileConfiguration config;
    private int rate;

    public Component color(String msg) {
        return color(msg, true);
    }

    public Component color(String msg, Boolean useHex) {
        LegacyComponentSerializer serializer = useHex ? LegacyComponentSerializer.builder().hexColors().character('&').build() : LegacyComponentSerializer.legacyAmpersand();
        return serializer.deserialize(msg);
    }

    @Override
    public void onEnable() {
        config = this.getConfig();
        config.options().copyDefaults(true);
        this.saveDefaultConfig();
        prefix = this.config.getString("prefix");
        isActive = this.config.getString("active-msg");
        isInactive = this.config.getString("inactive-msg");
        setRate(getConfig().getInt("mcmmo-rate"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (rate == 1) {
                    getLogger().info(isInactive);
                } else if (rate > 1) {
                    getLogger().info(isActive.replaceAll("\\{rate}", String.valueOf(rate)) + " (rate: " + rate + ", starting it up again)");
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "xprate " + rate + " true");
                } else {
                    getLogger().warning("I was expecting config.yml mcmmo-rate value to be 1, 2 or 3, etc, not " + rate + ". Please fix this.");
                }
            }
        }.runTaskLater(this, 180L);
        PluginCommand rateCommand = getCommand("rate");
        if (rateCommand == null) {
            getLogger().warning("I was expecting something but rateCommand was null.");
            return;
        }
        RateCommand rateCommandInstance = new RateCommand(this);
        rateCommand.setExecutor(rateCommandInstance);
        rateCommand.setTabCompleter(rateCommandInstance);
        rateCommand.setUsage(colorLegacy(prefix + rateCommand.getUsage()));
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerCommandPreprocess(this), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ServerCommand(this), this);
    }

    public int getRate() {
        return this.rate;
    }

    public void setRate(int newRate) {
        this.rate = newRate;
        getConfig().set("mcmmo-rate", newRate);
        saveConfig();
        // getLogger().info("DEBUG: setRate: " + rate);
    }

    private @NonNull String colorLegacy(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().serialize(color(msg, true));
    }
}
