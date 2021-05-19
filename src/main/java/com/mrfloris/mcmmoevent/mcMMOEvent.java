package com.mrfloris.mcmmoevent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.mrfloris.mcmmoevent.commands.RateCommand;
import com.mrfloris.mcmmoevent.events.PlayerCommandPreprocess;
import com.mrfloris.mcmmoevent.events.ServerCommand;

public class mcMMOEvent extends JavaPlugin {

    public FileManager fileManager;
    public YamlConfiguration config;
    public static String prefix;
    public static String inactive;

    public String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    private int rate;

    @Override
    public void onEnable() {
        fileManager = new FileManager(this);
        FileManager.Config config = fileManager.getConfig("config.yml");
        config.copyDefaults(true).save();
        this.config = config.get();
        prefix = this.config.getString("prefix");
        inactive = this.config.getString("inactive-msg");
        loadConfig();
        setRate(getConfig().getInt("xprate"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (rate <= 1) {
                    getLogger().info(inactive+" (rate: " + rate + ", no need to start it up again)");
                }
                if (rate > 1) {
                    getLogger().info("A Booster event is ongoing: " + rate + ", (starting it up again)");
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "xprate " + rate + " true");
                }
            }
        }.runTaskLater(this, 180L);

        PluginCommand rateCommand = getCommand("rate");
        assert rateCommand != null;
        rateCommand.setExecutor(new RateCommand(this));
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
        // DEBUG: Bukkit.getLogger().info("DEBUG: setRate: " + rate);
    }
    public int getRate() {
        return this.rate;
    }
    public void onDisable() {
        // Just leaving this here in case we need it.
    }
}