package com.mrfloris.mcmmoevent.events;

import com.google.common.primitives.Ints;
import com.mrfloris.mcmmoevent.mcMMOEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PlayerCommandPreprocess implements Listener {

    private final mcMMOEvent plugin;

    public PlayerCommandPreprocess(mcMMOEvent plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void on(PlayerCommandPreprocessEvent e) {
        // mcmmo.commands.xprate.all
        // mcmmo.commands.xprate // probably only this one is checked for.
        // mcmmo.commands.xprate.reset
        // mcmmo.commands.xprate.set

        String input = e.getMessage();
        if (input.startsWith("/")){
            input = input.substring(1);
            plugin.getLogger().info("DEBUG:before substring = \""+e.getMessage()+"\"; after = \""+input+"\"");
        }
        String[] cmd = input.split(" ");
        if (!cmd[0].equalsIgnoreCase("xprate")) { return; }
        if (!e.getPlayer().hasPermission("mcmmo.commands.xprate")) { return; } // not the best way, but this whole thing is hacky.
        switch (cmd.length) {
            case 2:
                if (cmd[1].equalsIgnoreCase("reset") || cmd[1].equalsIgnoreCase("clear")) {
                    plugin.setRate(1);
                }
            case 3:
                if (!cmd[2].equalsIgnoreCase("true") && !cmd[2].equalsIgnoreCase("false")) {
                    return;
                }
                Integer rate = Ints.tryParse(cmd[1]); // this should always exist
                if  (rate == null) { return; }
                plugin.setRate(rate);
                return;
            default: // triggers on no args (length 1), and too many (length 4+)
        }
    }
}