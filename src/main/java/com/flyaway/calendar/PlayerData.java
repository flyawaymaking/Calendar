package com.flyaway.calendar;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerData {
    private final CalendarPlugin plugin;
    private final File playersFolder;

    public PlayerData(CalendarPlugin plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "players");
    }

    public PlayerCalendarData getPlayerData(Player player) {
        return loadPlayerData(player);
    }

    private PlayerCalendarData loadPlayerData(Player player) {
        File playerFile = new File(playersFolder, player.getUniqueId() + ".yml");

        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            return new PlayerCalendarData(config);
        } else {
            return new PlayerCalendarData();
        }
    }

    public void savePlayerData(Player player, PlayerCalendarData data) {
        File playerFile = new File(playersFolder, player.getUniqueId() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        data.saveToConfig(config);

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить данные игрока: " + player.getName());
        }
    }

    public void forceResetAllPlayers() {
        // Сбрасываем данные в файлах
        File[] playerFiles = playersFolder.listFiles();
        if (playerFiles != null) {
            for (File file : playerFiles) {
                if (file.getName().endsWith(".yml")) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    config.set("lastClaimYear", plugin.getRewardManager().getCurrentYear());
                    config.set("lastClaimMonth", plugin.getRewardManager().getCurrentMonth());
                    config.set("lastClaimDate", "");
                    config.set("claimedDays", new ArrayList<Integer>());
                    try {
                        config.save(file);
                    } catch (IOException e) {
                        plugin.getLogger().warning("Не удалось сбросить файл: " + file.getName());
                    }
                }
            }
        }

        plugin.getLogger().info("Принудительно сброшены награды для всех игроков");
    }
}
