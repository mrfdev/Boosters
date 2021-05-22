package com.mrfloris.boosters;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.mrfloris.boosters.commands.RateCommand;
import com.mrfloris.boosters.events.PlayerCommandPreprocess;
import com.mrfloris.boosters.events.ServerCommand;

public class pluginEvents extends JavaPlugin {

    private int rate;
    public FileConfiguration config;
    public static String prefix;
    public static String isInactive;
    public static String isActive;
    public String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public void onEnable() {
        config = this.getConfig();
        config.options().copyDefaults(true);
        this.saveDefaultConfig();
        prefix = this.config.getString("prefix");
        isActive = this.config.getString("active-msg");
        isInactive = this.config.getString("inactive-msg");
        loadConfig();
        setRate(getConfig().getInt("mcmmo-rate"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (rate == 1) {
                    getLogger().info(isInactive);
                } else if (rate > 1) {
                    getLogger().info(isActive.replaceAll("\\{rate}", String.valueOf(rate)) +" (rate: " + rate + ", starting it up again)");
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "xprate " + rate + " true");
                } else {
                    getLogger().warning("I was expecting config.yml mcmmo-rate value to be 1, 2 or 3, etc. Please fix this.");
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
        rateCommand.setUsage(color(prefix + rateCommand.getUsage()));
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerCommandPreprocess(this), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ServerCommand(this), this);
    }
    public void setRate(int newRate) {
        this.rate = newRate;
        getConfig().set("mcmmo-rate", newRate);
        saveConfig();
        // getLogger().warning("DEBUG: setRate: " + rate);
    }
    public int getRate() {
        return this.rate;
    }

    @Override
    public void onDisable() {
        // Just leaving this here in case we need it.
    }
}