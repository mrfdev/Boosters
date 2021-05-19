package com.mrfloris.mcmmoevent.events;

import com.google.common.primitives.Ints;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import com.mrfloris.mcmmoevent.mcMMOEvent;

import java.util.List;

public class ServerCommand implements Listener {

    private final mcMMOEvent plugin;

    public ServerCommand(mcMMOEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(ServerCommandEvent e) {
        /*
            /xprate <rate:int> [broadcast:bool]
         */
        String[] cmd = e.getCommand().split(" ");
        if (!cmd[0].equalsIgnoreCase("xprate")) {
            return;
        }

        switch (cmd.length) {
            case 2:
                break; // break moves on to next section
            case 3:
                if (cmd[2].equalsIgnoreCase("true") || cmd[2].equalsIgnoreCase("false")) {
                    break;
                }
                return; // invalid command

            default: // triggers on no args (length 1), and too many (length 4+)
                return;
        }
        if (cmd[1].equalsIgnoreCase("reset") || cmd[1].equalsIgnoreCase("clear")) {
            plugin.setRate(1);
            return;
        }
        Integer rate = Ints.tryParse(cmd[1]); // this should always exist
        if  (rate == null) {
            return;
        }
        plugin.setRate(rate);
    }
}