package com.mrfloris.boosters;

import com.mrfloris.boosters.commands.RateCommand;
import com.mrfloris.boosters.events.PlayerCommandPreprocess;
import com.mrfloris.boosters.events.ServerCommand;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class pluginEvents extends JavaPlugin {

    private int rate;
    public FileConfiguration config;
    public static String prefix;
    public static String isInactive;
    public static String isActive;
    public static Boolean isDebug;
    Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
    public String color(String msg) {
        if (!Bukkit.getVersion().contains("1.8") && !Bukkit.getVersion().contains("1.9")
                && !Bukkit.getVersion().contains("1.10") && !Bukkit.getVersion().contains("1.11")
                && !Bukkit.getVersion().contains("1.12") && !Bukkit.getVersion().contains("1.13")
                && !Bukkit.getVersion().contains("1.14") && !Bukkit.getVersion().contains("1.15")) {
            Matcher matcher = pattern.matcher(msg);
            while (matcher.find()) {
                String color = msg.substring(matcher.start(), matcher.end());
                msg = msg.replace(color, ChatColor.of(color) + "");
                matcher = pattern.matcher(msg);
            }
        }
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
        isDebug = this.config.getBoolean("debug-mode",false);
        if (isDebug) { this.getLogger().setLevel(Level.FINE); }
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
                    getLogger().warning("I was expecting config.yml mcmmo-rate value to be 1, 2 or 3, etc, not "+rate+". Please fix this.");
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
        getLogger().fine("DEBUG: setRate: " + rate);
    }
    public int getRate() {
        return this.rate;
    }

}