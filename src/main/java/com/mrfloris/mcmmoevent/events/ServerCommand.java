package com.mrfloris.mcmmoevent.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import com.mrfloris.mcmmoevent.mcMMOEvent;

public class ServerCommand implements Listener {

    private final mcMMOEvent plugin;

    int tryparse(String tryset) {
        try {
            return Integer.parseInt(tryset);
        }
        catch (NumberFormatException e) {
            // yes
        }
        return 1;
    }
    public ServerCommand(mcMMOEvent plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void on(ServerCommandEvent e) {
        String cmd = e.getCommand();
        String[] args = cmd.split(" ");
        if (args.length == 3 && args[0].equalsIgnoreCase("xprate")
                && (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false"))) {
            plugin.setRate(tryparse(args[1]));
        } else {
            if (args[0].equalsIgnoreCase("xprate") && cmd.equalsIgnoreCase("xprate reset") || cmd.equalsIgnoreCase("xprate clear")) {
                plugin.setRate(1);
            }
        }
    }
}