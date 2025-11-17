package com.flyaway.calendar.managers;

import com.flyaway.calendar.CalendarPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class MenuConfigManager {
    private final CalendarPlugin plugin;
    private FileConfiguration menuConfig;

    public MenuConfigManager(CalendarPlugin plugin) {
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

    public String getTitle() {
        return menuConfig.getString("title", "<dark_purple>Calendar</dark_purple>");
    }

    public ConfigurationSection getFillerSection() {
        if (menuConfig.contains("filler")) {
            return menuConfig.getConfigurationSection("filler");
        }
        return null;
    }

    public ConfigurationSection getCloseSection() {
        if (menuConfig.contains("close")) {
            return menuConfig.getConfigurationSection("close");
        }
        return null;
    }

    public ConfigurationSection getPresentsSection() {
        if (menuConfig.contains("presents")) {
            return menuConfig.getConfigurationSection("presents");
        }
        return null;
    }

    public Integer getRows() {
        return menuConfig.getInt("rows", 6);
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }
}
