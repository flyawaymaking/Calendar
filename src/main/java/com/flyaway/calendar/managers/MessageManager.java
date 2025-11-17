package com.flyaway.calendar.managers;

import com.flyaway.calendar.CalendarPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class MessageManager {
    private final CalendarPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String prefix;

    public MessageManager(CalendarPlugin plugin) {
        this.plugin = plugin;
        loadMessageConfig();
    }

    public void loadMessageConfig() {
        this.prefix = plugin.getConfig().getString("prefix", "<gray>[<aqua>Calendar</aqua>]</gray>");
    }

    public @NotNull String getMessage(String key, Map<String, String> placeholders) {
        String message = plugin.getConfig().getString("messages." + key, "<red>message." + key + " not-found");
        return formatMessage(message, placeholders);
    }

    public @NotNull String getMessage(String key) {
        return getMessage(key, null);
    }

    public void sendMessageByKey(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        if (message.isEmpty()) return;
        sendRawMessage(sender, prefix + " » " + message);
    }

    public void sendMessageByKey(CommandSender sender, String key) {
        sendMessageByKey(sender, key, null);
    }

    public void sendRawMessageByKey(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = getMessage(key, placeholders);
        if (message.isEmpty()) return;
        sendRawMessage(sender, message);
    }

    public void sendRawMessage(CommandSender sender, String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return;
        sendRawMessage(sender, formatMessage(message, placeholders));
    }

    public void sendRawMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(miniMessage.deserialize(message));
    }

    public @NotNull String formatMessage(String message, Map<String, String> placeholders) {
        if (message == null || message.isEmpty()) return "";
        message = message.replaceAll("\\s+$", "");

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public @NotNull Component convertToComponent(String message) {
        return convertToComponent(formatMessage(message, null), null);
    }

    public @NotNull Component convertToComponent(String message, Map<String, String> placeholders) {
        message = formatMessage(message, placeholders);
        if (message.isEmpty()) return Component.empty();
        return miniMessage.deserialize(message);
    }
}
