package com.flyaway.calendar.listeners;

import com.flyaway.calendar.CalendarPlugin;
import com.flyaway.calendar.PlayerCalendarData;
import com.flyaway.calendar.PlayerData;
import com.flyaway.calendar.managers.RewardManager;
import com.flyaway.calendar.managers.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDate;
import java.util.Map;

public class PlayerJoinListener implements Listener {
    private final CalendarPlugin plugin;
    private final MessageManager messageManager;
    private final RewardManager rewardManager;

    public PlayerJoinListener(CalendarPlugin plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.rewardManager = plugin.getRewardManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerData();

        PlayerCalendarData data = playerData.getPlayerData(player);

        String today = LocalDate.now().toString();

        // Если уже получал сегодня — просто показать прогресс
        if (today.equals(data.getLastClaimDate())) {
            messageManager.sendMessageByKey(player, "already-claimed-today", Map.of("player", player.getName()));
            return;
        }

        int nextDay = data.getNextClaimDay();
        int maxDays = rewardManager.getTotalRewards();

        // Если больше наград нет — сообщаем и выходим
        if (nextDay > maxDays) {
            messageManager.sendMessageByKey(player, "all-rewards-claimed");
            return;
        }

        // Есть награда — пытаемся выдать
        if (!hasInventorySpace(player)) {
            messageManager.sendMessageByKey(player, "no-inventory-space");
            return;
        }

        // Выдача награды
        rewardManager.giveReward(player, nextDay);

        data.claimDay(nextDay, today);
        playerData.savePlayerData(player, data);

        showProgress(player, data);
    }

    private void showProgress(Player player, PlayerCalendarData data) {
        int total = rewardManager.getTotalRewards();
        int claimed = data.getLastClaimedDay();

        if (claimed == 0 || claimed >= total)
            return;

        messageManager.sendMessageByKey(player, "progress", Map.of(
                "claimed", String.valueOf(claimed),
                "total", String.valueOf(total)));
    }

    private boolean hasInventorySpace(Player player) {
        return player.getInventory().firstEmpty() != -1;
    }
}
