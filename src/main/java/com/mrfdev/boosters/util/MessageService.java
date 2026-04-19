package com.mrfdev.boosters.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageService {

    private static final String FALLBACK_PREFIX = "<gold>[Boosters]</gold> ";

    private final LocaleService localeService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(LocaleService localeService) {
        this.localeService = localeService;
    }

    public void prefixed(CommandSender sender, String message, TagResolver... resolvers) {
        send(sender, prefix() + message, resolvers);
    }

    public void send(CommandSender sender, String message, TagResolver... resolvers) {
        sender.sendMessage(miniMessage.deserialize(message, resolvers));
    }

    public void info(JavaPlugin plugin, String message, TagResolver... resolvers) {
        prefixed(plugin.getServer().getConsoleSender(), message, resolvers);
    }

    public static TagResolver value(String key, String value) {
        return Placeholder.unparsed(key, value == null ? "" : value);
    }

    private String prefix() {
        return localeService.get("prefix", FALLBACK_PREFIX);
    }
}
