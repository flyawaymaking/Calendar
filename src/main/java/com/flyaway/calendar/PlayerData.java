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
        PlayerCalendarData data = new PlayerCalendarData();

        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            data.lastClaimYear = config.getInt("lastClaimYear", 0);
            data.lastClaimMonth = config.getInt("lastClaimMonth", 0);
            data.lastClaimDate = config.getString("lastClaimDate", "");
            data.claimedDays.addAll(config.getIntegerList("claimedDays"));
        }

        // Всегда проверяем сброс месяца при загрузке данных
        checkMonthReset(data);

        return data;
    }

    private void checkMonthReset(PlayerCalendarData data) {
        int currentYear = plugin.getRewardManager().getCurrentYear();
        int currentMonth = plugin.getRewardManager().getCurrentMonth();

        if (data.lastClaimYear != currentYear || data.lastClaimMonth != currentMonth) {
            // Месяц сменился - сбрасываем прогресс
            data.claimedDays.clear();
            data.lastClaimYear = currentYear;
            data.lastClaimMonth = currentMonth;
            data.lastClaimDate = "";
            plugin.getLogger().info("Сброшены награды для нового месяца: " + currentMonth + "/" + currentYear);
        }
    }

    public void savePlayerData(Player player, PlayerCalendarData data) {
        File playerFile = new File(playersFolder, player.getUniqueId() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("lastClaimYear", data.lastClaimYear);
        config.set("lastClaimMonth", data.lastClaimMonth);
        config.set("lastClaimDate", data.lastClaimDate);
        config.set("claimedDays", new ArrayList<>(data.claimedDays));

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить данные игрока: " + player.getName());
        }
    }

    public void claimDay(Player player, int sequentialDay) {
        PlayerCalendarData data = getPlayerData(player);
        data.claimedDays.add(sequentialDay);
        savePlayerData(player, data);
    }

    public boolean hasClaimedDay(Player player, int sequentialDay) {
        PlayerCalendarData data = getPlayerData(player);
        return data.claimedDays.contains(sequentialDay);
    }

    public int getLastClaimedDay(Player player) {
        PlayerCalendarData data = getPlayerData(player);
        return data.claimedDays.stream().max(Integer::compareTo).orElse(0);
    }

    public int getNextClaimDay(Player player) {
        return getLastClaimedDay(player) + 1;
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

    public static class PlayerCalendarData {
        public int lastClaimYear = 0;
        public int lastClaimMonth = 0;
        public String lastClaimDate = "";
        public Set<Integer> claimedDays = new HashSet<>();
    }
}
