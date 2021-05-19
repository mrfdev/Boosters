package com.mrfloris.mcmmoevent.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import com.mrfloris.mcmmoevent.mcMMOEvent;
import org.bukkit.command.TabCompleter;
import java.util.List;
import static com.mrfloris.mcmmoevent.mcMMOEvent.prefix;
import static com.mrfloris.mcmmoevent.mcMMOEvent.isInactive;
import static com.mrfloris.mcmmoevent.mcMMOEvent.isActive;

public class RateCommand implements CommandExecutor, TabCompleter {

    private final mcMMOEvent plugin;
    public RateCommand(mcMMOEvent plugin) {
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
        return null;
    }
}