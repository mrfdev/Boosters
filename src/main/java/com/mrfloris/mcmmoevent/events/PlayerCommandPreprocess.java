package com.mrfloris.mcmmoevent.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.mrfloris.mcmmoevent.mcMMOEvent;

public class PlayerCommandPreprocess implements Listener {

    private mcMMOEvent plugin;

    public PlayerCommandPreprocess(mcMMOEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(PlayerCommandPreprocessEvent e) {
        String cmd = e.getMessage();
        Player p = e.getPlayer();
        if (p.hasPermission("mcmmo.commands.xprate.all") || p.hasPermission("mcmmo.commands.xprate")
                || p.hasPermission("mcmmo.commands.xprate.reset") || p.hasPermission("mcmmo.commands.xprate.set")
                || p.isOp()) {
            if (cmd.contains("/xprate ") && (cmd.contains(" true") || cmd.contains(" false"))) {
                plugin.setRate(Integer.valueOf(cmd.split(" ")[1]));
            } else {
                if (cmd.equalsIgnoreCase("/xprate reset")) {
                    plugin.setRate(1);
                }
            }
        }
    }

}