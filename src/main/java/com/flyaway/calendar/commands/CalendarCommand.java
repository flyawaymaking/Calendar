package com.flyaway.calendar.commands;

import com.flyaway.calendar.CalendarPlugin;
import com.flyaway.calendar.holders.MenuHolder;
import com.flyaway.calendar.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CalendarCommand implements CommandExecutor, TabCompleter {
    private final CalendarPlugin plugin;
    private final MessageManager messageManager;

    public CalendarCommand(CalendarPlugin plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("calendar")) {
            if (!(sender instanceof Player player)) {
                messageManager.sendMessageByKey(sender, "error-only-players");
                return true;
            }

            if (args.length == 0) {
                MenuHolder.openCalendarMenu(plugin, player);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    if (sender.hasPermission("calendar.admin")) {
                        try {
                            plugin.reloadPlugin();
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("player", sender.getName());
                            messageManager.sendMessageByKey(sender, "reload-success", placeholders);
                        } catch (Exception e) {
                            messageManager.sendMessageByKey(sender, "reload-failed");
                        }
                    } else {
                        messageManager.sendMessageByKey(sender, "error-no-permission");
                    }
                }
                case "resetall" -> {
                    if (sender.hasPermission("calendar.admin")) {
                        plugin.getPlayerData().forceResetAllPlayers();
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("player", sender.getName());
                        messageManager.sendMessageByKey(sender, "reset-all-success", placeholders);
                    } else {
                        messageManager.sendMessageByKey(sender, "error-no-permission");
                    }
                }
                default -> {
                    showHelp(sender);
                }
            }
            return true;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", sender.getName());

        if (sender.hasPermission("calendar.admin")) {
            messageManager.sendRawMessageByKey(sender, "help-admin", placeholders);
        } else {
            messageManager.sendRawMessageByKey(sender, "help-player", placeholders);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            commands.add("help");

            if (sender.hasPermission("calendar.admin")) {
                commands.add("reload");
                commands.add("resetall");
            }

            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        }

        return completions;
    }
}
