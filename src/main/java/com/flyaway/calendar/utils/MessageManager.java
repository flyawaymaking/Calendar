package com.flyaway.calendar.utils;

import com.flyaway.calendar.CalendarPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageManager {
    private final CalendarPlugin plugin;
    private String prefix;

    public MessageManager(CalendarPlugin plugin) {
        this.plugin = plugin;
        loadMessageConfig();
    }

    public void loadMessageConfig() {
        this.prefix = plugin.getConfig().getString("prefix", "&#FF0055&lЕжедневный вход &7» ");
    }

    /**
     * Отправляет сообщение с префиксом плагина
     */
    public void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ColorUtils.formatColor(prefix + message));
    }

    public void sendMessage(Player player, String message) {
        sendMessage((CommandSender) player, message);
    }

    public void sendRawMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ColorUtils.formatColor(message));
    }

    public void sendMessages(CommandSender sender, List<String> messages) {
        if (messages == null || messages.isEmpty()) return;
        for (String message : messages) {
            sendRawMessage(sender, message);
        }
    }
}
