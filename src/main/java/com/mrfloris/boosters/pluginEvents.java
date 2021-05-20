package com.mrfloris.boosters;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.mrfloris.boosters.commands.RateCommand;
import com.mrfloris.boosters.events.PlayerCommandPreprocess;
import com.mrfloris.boosters.events.ServerCommand;

public class pluginEvents extends JavaPlugin {

    private int rate;
    public FileManager fileManager;
    public YamlConfiguration config;
    public static String prefix;
    public static String isInactive;
    public static String isActive;
    public String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public void onEnable() {
        fileManager = new FileManager(this);
        FileManager.Config config = fileManager.getConfig("config.yml");
        config.copyDefaults(true).save();
        this.config = config.get();
        prefix = this.config.getString("prefix");
        isActive = this.config.getString("active-msg");
        isInactive = this.config.getString("inactive-msg");
        loadConfig();
        setRate(getConfig().getInt("xprate"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (rate == 1) {
                    getLogger().info(isInactive);
                } else if (rate > 1) {
                    getLogger().info(isActive.replaceAll("\\{rate}", String.valueOf(rate)) +" (rate: " + rate + ", starting it up again)");
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "xprate " + rate + " true");
                } else {
                    getLogger().warning("I was expecting config.yml xprate value to be 1, 2 or 3, etc. Please fix this.");
                }
            }
        }.runTaskLater(this, 180L);

        PluginCommand rateCommand = getCommand("rate");
        assert rateCommand != null;
        RateCommand rateCommandInstance = new RateCommand(this);
        rateCommand.setExecutor(rateCommandInstance);
        rateCommand.setTabCompleter(rateCommandInstance);
        rateCommand.setUsage(color(prefix + rateCommand.getUsage()));
        Bukkit.getServer().getPluginManager().registerEvents(new PlayerCommandPreprocess(this), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ServerCommand(this), this);
    }
    private void loadConfig() {
        // Reminder: $rate is still 0 at this point
        getConfig().options().copyDefaults(true);
        saveConfig();
    }
    public void setRate(int newRate) {
        this.rate = newRate;
        getConfig().set("xprate", newRate);
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