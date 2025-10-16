package com.flyaway.calendar.commands;

import com.flyaway.calendar.utils.ColorUtils;
import com.flyaway.calendar.CalendarPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CalendarCommand implements CommandExecutor {
    private final CalendarPlugin plugin;

    public CalendarCommand(CalendarPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("calendar")) {
            if (!(sender instanceof Player)) {
                plugin.getMessageManager().sendMessage(sender, "Эта команда только для игроков!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                // Данные всегда загружаются свежими из файла при открытии меню
                plugin.getMenuManager().openCalendarMenu(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("calendar.admin")) {
                try {
                    plugin.reloadPlugin();
                    plugin.getMessageManager().sendMessage(sender, plugin.getConfig().getString("reload.success", "&#33FF33Плагин успешно перезагружен!"));
                } catch (Exception e) {
                    plugin.getMessageManager().sendMessage(sender, plugin.getConfig().getString("reload.failed", "&#FF3333Не удалось перезагрузить плагин!"));
                    e.printStackTrace();
                }
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("resetall") && sender.hasPermission("calendar.admin")) {
                plugin.getPlayerData().forceResetAllPlayers();
                plugin.getMessageManager().sendMessage(sender, "&aНаграды всех игроков сброшены для нового месяца!");
                return true;
            }

            // Показываем помощь
            plugin.getMessageManager().sendMessages(sender, plugin.getConfig().getStringList("help"));
        }
        return true;
    }
}
