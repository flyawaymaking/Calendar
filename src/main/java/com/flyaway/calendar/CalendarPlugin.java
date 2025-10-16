package com.flyaway.calendar;

import com.flyaway.calendar.commands.CalendarCommand;
import com.flyaway.calendar.listeners.PlayerJoinListener;
import com.flyaway.calendar.listeners.MenuListener;
import com.flyaway.calendar.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class CalendarPlugin extends JavaPlugin {
    private static CalendarPlugin instance;
    private RewardManager rewardManager;
    private MenuManager menuManager;
    private MessageManager messageManager;
    private PlayerData playerData;
    private int taskId;

    @Override
    public void onEnable() {
        instance = this;

        // Создаем папки если их нет
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        new File(getDataFolder(), "players").mkdirs();

        // Загружаем конфиги
        saveDefaultConfig();
        saveResource("rewards.yml", false);
        saveResource("menu.yml", false);

        // Инициализируем менеджеры
        this.playerData = new PlayerData(this);
        this.rewardManager = new RewardManager(this);
        this.menuManager = new MenuManager(this);
        this.messageManager = new MessageManager(this);

        // Регистрируем команды и слушатели
        getCommand("calendar").setExecutor(new CalendarCommand(this));
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        // Запускаем задачу для проверки сброса 1 числа каждого месяца
        startMonthlyResetTask();

        getLogger().info("Плагин календаря включен!");
    }

    private void startMonthlyResetTask() {
        // Задача выполняется каждый день в 00:01
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            checkAndResetMonthly();
        }, 20L * 60, 20L * 60 * 60 * 24); // Проверка каждый день
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    private void checkAndResetMonthly() {
        int currentDay = rewardManager.getCurrentDayOfMonth();

        // Если сегодня 1 число, сбрасываем награды
        if (currentDay == 1) {
            playerData.forceResetAllPlayers();
            getLogger().info("Автоматически сброшены награды для всех игроков (1 число месяца)");

            // Уведомляем онлайн-игроков
            for (Player player : getServer().getOnlinePlayers()) {
                messageManager.sendMessage(player, "&6&lНаступил новый месяц! &eСистема ежедневных наград сброшена. Заходите каждый день чтобы получить все награды!");
            }
        }
    }

    @Override
    public void onDisable() {
        // Отменяем задачу если она запущена
        if (taskId != 0) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        getLogger().info("Плагин календаря выключен!");
    }

    public void reloadPlugin() {
        reloadConfig();
        rewardManager.loadRewards();
        menuManager.loadMenuConfig();
        messageManager.loadMessageConfig();
        getLogger().info("Конфиги перезагружены!");
    }

    public static CalendarPlugin getInstance() {
        return instance;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }
}
