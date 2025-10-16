package com.flyaway.calendar;

import com.flyaway.calendar.menu.CalendarMenu;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MenuManager {
    private final CalendarPlugin plugin;
    private FileConfiguration menuConfig;

    public MenuManager(CalendarPlugin plugin) {
        this.plugin = plugin;
        loadMenuConfig();
    }

    public void loadMenuConfig() {
        File menuFile = new File(plugin.getDataFolder(), "menu.yml");
        if (!menuFile.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        menuConfig = YamlConfiguration.loadConfiguration(menuFile);
    }

    public void openCalendarMenu(Player player) {
        CalendarMenu calendarMenu = new CalendarMenu(plugin, player);
        player.openInventory(calendarMenu.getInventory());
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }
}
