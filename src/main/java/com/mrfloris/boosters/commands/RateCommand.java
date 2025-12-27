package com.mrfloris.boosters.commands;

import com.mrfloris.boosters.pluginEvents;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import static com.mrfloris.boosters.pluginEvents.isActive;
import static com.mrfloris.boosters.pluginEvents.isInactive;
import static com.mrfloris.boosters.pluginEvents.prefix;

public class RateCommand implements CommandExecutor, TabCompleter {

    private final pluginEvents plugin;

    public RateCommand(pluginEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (plugin.getRate() == 1) {
                sender.sendMessage(plugin.color(prefix + isInactive));
            } else if (plugin.getRate() > 1) {
                String activeString = isActive.replaceAll("\\{rate}", String.valueOf(plugin.getRate()));
                sender.sendMessage(plugin.color(prefix + activeString));
            } else {
                sender.sendMessage(plugin.color(prefix + isInactive));
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return Collections.emptyList();
    }
}