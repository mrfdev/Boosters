package com.mrfloris.mcmmoevent.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import com.mrfloris.mcmmoevent.mcMMOEvent;

public class ServerCommand implements Listener {

    private final mcMMOEvent plugin;

    public ServerCommand(mcMMOEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(ServerCommandEvent e) {
        String cmd = e.getCommand();
        if (cmd.contains("xprate ") && (cmd.contains(" true") || cmd.contains(" false"))) {
            plugin.setRate(Integer.parseInt(cmd.split(" ")[1]));
        }
        else {
            if (cmd.contains("xprate ") && cmd.equalsIgnoreCase("xprate reset") || cmd.equalsIgnoreCase("xprate clear")) {
                plugin.setRate(1);
            }
        }
    }

}
