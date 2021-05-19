package com.mrfloris.mcmmoevent.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.mrfloris.mcmmoevent.mcMMOEvent;

public class PlayerCommandPreprocess implements Listener {

    private final mcMMOEvent plugin;

    int tryparse(String tryset) {
        try {
            return Integer.parseInt(tryset);
        }
        catch (NumberFormatException e) {
            // yes
        }
        return Integer.parseInt(tryset);
    }
    public PlayerCommandPreprocess(mcMMOEvent plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void on(PlayerCommandPreprocessEvent e) {
        String cmd = e.getMessage();
        Player p = e.getPlayer();
        // TODO: check if command is xprate, then check for permission, no need to check permission on every command <zrips>
//        @EventHandler
//        public void on(PlayerCommandPreprocessEvent e) {
//            String cmd = e.getMessage();
//
//            if (!cmd.toLowerCase().startsWith("/xprate "))
//                return;
//
//            Player p = e.getPlayer();
//            if (!p.hasPermission("mcmmo.commands.xprate.all") &&
//                    !p.hasPermission("mcmmo.commands.xprate") &&
//                    !p.hasPermission("mcmmo.commands.xprate.reset") &&
//                    !p.hasPermission("mcmmo.commands.xprate.set") &&
//                    !p.isOp())
//                return;
//
//            if (cmd.equalsIgnoreCase("/xprate true") || cmd.equalsIgnoreCase("/xprate false")) {
//                plugin.setRate(Integer.parseInt(cmd.split(" ")[1]));
//            } else if (cmd.contains("/xprate ") && cmd.equalsIgnoreCase("/xprate reset") || cmd.equalsIgnoreCase("/xprate clear")) {
//                plugin.setRate(1);
//            }
//        }
        if (p.hasPermission("mcmmo.commands.xprate.all") || p.hasPermission("mcmmo.commands.xprate")
                || p.hasPermission("mcmmo.commands.xprate.reset") || p.hasPermission("mcmmo.commands.xprate.set")) {
            if (cmd.contains("/xprate ") && (cmd.contains(" true") || cmd.contains(" false"))) {
                plugin.setRate(tryparse(cmd.split(" ")[1]));
            } else {
                if (cmd.contains("/xprate ") && cmd.equalsIgnoreCase("/xprate reset") || cmd.equalsIgnoreCase("/xprate clear")) {
                    plugin.setRate(1);
                }
            }
        }
    }
}