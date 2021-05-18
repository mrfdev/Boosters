package com.mrfloris.mcmmoevent;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.mrfloris.mcmmoevent.commands.RateCommand;
import com.mrfloris.mcmmoevent.events.PlayerCommandPreprocess;
import com.mrfloris.mcmmoevent.events.ServerCommand;

public class mcMMOEvent extends JavaPlugin {

    public FileManager fileManager;
    public YamlConfiguration config;
    public String color(String msg) {
        return msg.replace("&", "§");
        // TODO: update to return ChatColor.translateAlternateColorCodes('&', msg);
    }
    private int rate;

    // TODO: move prefix to public static so we can use it where appropriate
    // String prefixFromThere = mcMMOEvent.getPrefix();

    //    static String prefix = null;
    //    public static String getPrefix() {
    //        return prefix;
    //    }

    @Override
    public void onEnable() {
        fileManager = new FileManager(this);
        FileManager.Config config = fileManager.getConfig("config.yml");
        config.copyDefaults(true).save();
        this.config = config.get();

        // we don't use this here (also, see above to do item) String prefix = this.config.getString("prefix");
        loadConfig();
        setRate(getConfig().getInt("xprate"));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (rate <= 1) {
                    Bukkit.getLogger().info("[mcMMO] Event is off, rate: " + rate + ", (no need to start it up again)");
                }
                if (rate > 1 ) {
                    Bukkit.getLogger().info("[mcMMO] Event is ongoing: " + rate + ", (starting it up again)");
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "xprate " + rate + " true");
                }
//                if (rate != 1) {
//                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "xprate " + rate + " true");
//                }
            }
        }.runTaskLater(this, 180L);

        getCommand("rate").setExecutor(new RateCommand(this));
        // TODO: nitpicky fix this warning
        // original: getCommand("rate").setExecutor(new RateCommand(this));
        // can be null warning fix 1: Objects.requireNonNull(getCommand("rate")).setExecutor(new RateCommand(this));
        // can be null warning fix 2: if (this.getCommand("rate")!= null)
        // what's the right way here to deal with command can be null on setExecutor?

        Bukkit.getServer().getPluginManager().registerEvents(new PlayerCommandPreprocess(this), this);
        Bukkit.getServer().getPluginManager().registerEvents(new ServerCommand(this), this);
    }

    private void loadConfig() {
        // note: $rate is still 0 at this point
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    public void setRate(int newRate) {
        this.rate = newRate;
        getConfig().set("xprate", newRate);
        saveConfig();
//        Bukkit.getLogger().info("DEBUG: setRate: " + rate);
    }

    public int getRate() {
        return this.rate;
    }

    public void onDisable() {
        // Just leaving this here in case we need it.
    }

}
