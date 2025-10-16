package com.flyaway.calendar.listeners;

import com.flyaway.calendar.CalendarPlugin;
import com.flyaway.calendar.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDate;

public class PlayerJoinListener implements Listener {
    private final CalendarPlugin plugin;

    public PlayerJoinListener(CalendarPlugin plugin) {
        this.plugin = plugin;
    }

    private void sendMessage(Player player, String message) {
        plugin.getMessageManager().sendMessage(player, message);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Всегда загружаем свежие данные из файла
        PlayerData.PlayerCalendarData data = plugin.getPlayerData().getPlayerData(player);

        // Получаем текущую дату
        String currentDate = getCurrentDate();

        // Проверяем, получал ли игрок награду сегодня
        if (currentDate.equals(data.lastClaimDate)) {
            sendMessage(player, "&eВы уже получали награду сегодня. Следующую награду можно будет получить завтра!");
            showProgress(player, data);
            return;
        }

        // Если игрок еще не получал награду сегодня И у него есть доступные дни
        int nextSequentialDay = data.claimedDays.size() + 1;

        if (nextSequentialDay <= plugin.getRewardManager().getTotalRewards()) {
            // Автоматически выдаем награду при входе
            if (hasInventorySpace(player)) {
                plugin.getRewardManager().giveReward(player, nextSequentialDay);

                // Обновляем данные и сохраняем
                data.claimedDays.add(nextSequentialDay);
                data.lastClaimDate = currentDate;
                plugin.getPlayerData().savePlayerData(player, data);
            } else {
                sendMessage(player, "&eУ вас нет места в инвентаре для награды! Используйте &6/calendar &eчтобы получить её позже.");
            }
        } else if (data.claimedDays.size() >= plugin.getRewardManager().getTotalRewards()) {
            sendMessage(player, "&6&lПоздравляем! &eВы получили все награды за этот месяц! Ждём вас в следующем месяце.");
        }

        // Показываем прогресс
        showProgress(player, data);
    }

    private void showProgress(Player player, PlayerData.PlayerCalendarData data) {
        int totalRewards = plugin.getRewardManager().getTotalRewards();
        int claimed = data.claimedDays.size();

        if (claimed > 0 && claimed < totalRewards) {
            sendMessage(player, "&7Ваш прогресс: &e" + claimed + "&7/&6" + totalRewards + " &7наград получено");
        }
    }

    private String getCurrentDate() {
        // Возвращает дату в формате "2024-01-15"
        LocalDate today = LocalDate.now();
        return today.toString();
    }

    private boolean hasInventorySpace(Player player) {
        return player.getInventory().firstEmpty() != -1;
    }
}
