package com.flyaway.calendar;

import com.flyaway.calendar.commands.CalendarCommand;
import com.flyaway.calendar.listeners.PlayerJoinListener;
import com.flyaway.calendar.listeners.MenuListener;
import com.flyaway.calendar.managers.MenuConfigManager;
import com.flyaway.calendar.managers.MessageManager;
import com.flyaway.calendar.managers.RewardManager;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CalendarPlugin extends JavaPlugin {
    private MessageManager messageManager;
    private RewardManager rewardManager;
    private MenuConfigManager menuConfigManager;
    private PlayerData playerData;

    private ScheduledTask monthlyTask;

    @Override
    public void onEnable() {

        // Создаём директорию плагина
        try {
            Files.createDirectories(getDataFolder().toPath());
            Files.createDirectories(getDataFolder().toPath().resolve("players"));
        } catch (IOException e) {
            getLogger().severe("Не удалось создать папки плагина!");
        }

        // Загружаем конфиги
        saveDefaultConfig();
        saveResource("rewards.yml", false);
        saveResource("menu.yml", false);

        this.messageManager = new MessageManager(this);
        this.playerData = new PlayerData(this);
        this.rewardManager = new RewardManager(this);
        this.menuConfigManager = new MenuConfigManager(this);

        CalendarCommand cmd = new CalendarCommand(this);
        if (getCommand("calendar") != null) {
            getCommand("calendar").setExecutor(cmd);
            getCommand("calendar").setTabCompleter(cmd);
        } else {
            getLogger().warning("Команда /calendar не найдена в plugin.yml!");
        }

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);

        startMonthlyResetTask();

        getLogger().info("Плагин календаря включён!");
    }

    private void startMonthlyResetTask() {

        if (monthlyTask != null && !monthlyTask.isCancelled()) {
            monthlyTask.cancel();
        }

        // Вычисляем время, когда должна сработать следующая проверка
        long delayMillis = calculateNextRunDelay();

        monthlyTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                this,
                task -> checkAndResetMonthly(),
                delayMillis,
                1000L * 60 * 60 * 24,
                TimeUnit.MILLISECONDS
        );

        getLogger().info("Ежедневная задача сброса наград запланирована через " + delayMillis + "ms");
    }

    private long calculateNextRunDelay() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime next = now
                .withHour(0).withMinute(1).withSecond(0).withNano(0);

        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }

        return ChronoUnit.MILLIS.between(now, next);
    }

    private void checkAndResetMonthly() {
        int day = rewardManager.getCurrentDayOfMonth();

        if (day != 1) return;

        playerData.forceResetAllPlayers();
        getLogger().info("Автоматический сброс наград (1 число нового месяца)");

        for (Player player : Bukkit.getOnlinePlayers()) {
            messageManager.sendMessageByKey(player, "new-month", Map.of("player", player.getName()));
        }
    }

    @Override
    public void onDisable() {
        if (monthlyTask != null) {
            monthlyTask.cancel();
        }
        getLogger().info("Плагин календаря выключен!");
    }

    public void reloadPlugin() {
        reloadConfig();
        rewardManager.loadRewards();
        menuConfigManager.loadMenuConfig();
        messageManager.loadMessageConfig();
        getLogger().info("Конфиги перезагружены!");
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public MenuConfigManager getMenuConfigManager() {
        return menuConfigManager;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
