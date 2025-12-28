package com.mrfloris.boosters.events;

import com.google.common.primitives.Ints;
import com.mrfloris.boosters.pluginEvents;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.jspecify.annotations.NonNull;

public class ServerCommand implements Listener {

    private final pluginEvents plugin;

    public ServerCommand(pluginEvents plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on(@NonNull ServerCommandEvent e) {
        /*
            cmd length 1 - invalid
            cmd length 2
            /xprate reset - valid
            /xprate clear - valid
            /xprate anything else - invalid
            cmd length 3
            /xprate valid-int valid-bool - valid

         */
        String[] cmd = e.getCommand().split(" ");
        if (!cmd[0].equalsIgnoreCase("xprate")) {
            return;
        }
        switch (cmd.length) {
            case 2:
                if (cmd[1].equalsIgnoreCase("reset") || cmd[1].equalsIgnoreCase("clear")) {
                    plugin.setRate(1);
                }
                return;
            case 3:
                if (!cmd[2].equalsIgnoreCase("true") && !cmd[2].equalsIgnoreCase("false")) {
                    return;
                }
                Integer rate = Ints.tryParse(cmd[1]); // this should always exist
                if (rate == null) {
                    return;
                }
                plugin.setRate(rate);
                return;
            default: // triggers on no args (length 1), and too many (length 4+)
        }
    }
}