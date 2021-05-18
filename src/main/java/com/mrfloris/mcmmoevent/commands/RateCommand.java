package com.mrfloris.mcmmoevent.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.mrfloris.mcmmoevent.mcMMOEvent;

import static com.mrfloris.mcmmoevent.mcMMOEvent.prefix;

public class RateCommand implements CommandExecutor {

    private final mcMMOEvent plugin;


    public RateCommand(mcMMOEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (plugin.getRate() == 1) {
                sender.sendMessage(plugin.color(prefix + "There's no event right now."));
            } else {
                sender.sendMessage(plugin.color(prefix + "There's a &f&l" + plugin.getRate() + "&r&3x&7 event going on right now."));
            }
        } else {
            return false;
        }
        return true;
    }

}