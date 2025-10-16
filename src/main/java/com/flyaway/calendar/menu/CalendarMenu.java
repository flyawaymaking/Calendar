package com.flyaway.calendar.menu;

import com.flyaway.calendar.CalendarPlugin;
import com.flyaway.calendar.utils.ItemBuilder;
import com.flyaway.calendar.utils.ColorUtils;
import com.flyaway.calendar.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.time.LocalDate;
import java.util.*;

public class CalendarMenu implements InventoryHolder {
    private final CalendarPlugin plugin;
    private final Player player;
    private Inventory inventory;
    private final FileConfiguration menuConfig;

    public CalendarMenu(CalendarPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.menuConfig = plugin.getMenuManager().getMenuConfig();
        createInventory();
    }

    private void createInventory() {
        String title = formatColor(menuConfig.getString("title", "&0&lКалендарь"));
        int rows = menuConfig.getInt("rows", 6);
        this.inventory = Bukkit.createInventory(this, rows * 9, title);

        fillBackground();
        addCloseButton();
        addPresents();
    }

    private void fillBackground() {
        if (menuConfig.contains("filler")) {
            Material material = Material.valueOf(menuConfig.getString("filler.material").toUpperCase());
            String name = menuConfig.getString("filler.name", "");
            List<String> lore = menuConfig.getStringList("filler.lore");
            List<Integer> slots = parseSlots(menuConfig.getStringList("filler.slot"));

            ItemStack fillerItem = ItemBuilder.createItemStack(material, 1, name, lore);
            for (int slot : slots) {
                if (slot < inventory.getSize()) {
                    inventory.setItem(slot, fillerItem);
                }
            }
        }
    }

    private void addCloseButton() {
        if (menuConfig.contains("close")) {
            Material material = Material.valueOf(menuConfig.getString("close.material").toUpperCase());
            String name = menuConfig.getString("close.name", "");
            List<String> lore = menuConfig.getStringList("close.lore");
            int slot = menuConfig.getInt("close.slot", 49);

            ItemStack closeItem = ItemBuilder.createItemStack(material, 1, name, lore);
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, closeItem);
            }
        }
    }

    private void addPresents() {
        // Получаем данные игрока
        int nextClaimDay = plugin.getPlayerData().getPlayerData(player).claimedDays.size() + 1;
        int totalRewards = plugin.getRewardManager().getTotalRewards();

        // Создаем мапу день -> слот
        Map<Integer, Integer> dayToSlot = createDayToSlotMap();

        for (int day = 1; day <= totalRewards; day++) {
            Integer slot = dayToSlot.get(day);
            if (slot != null && slot < inventory.getSize()) {
                ItemStack presentItem = createPresentItem(day, nextClaimDay);
                if (presentItem != null) {
                    inventory.setItem(slot, presentItem);
                }
            }
        }
    }

    private ItemStack createPresentItem(int day, int nextClaimDay) {
        PlayerData.PlayerCalendarData playerData = plugin.getPlayerData().getPlayerData(player);
        boolean isClaimed = playerData.claimedDays.contains(day);
        boolean isNextClaim = day == nextClaimDay;

        boolean canClaim = isNextClaim && !isTodayClaimed(playerData);

        String presentType = getPresentTypeForDay(day);
        String itemPath = getItemPath(presentType, isClaimed, canClaim, day < nextClaimDay && !isClaimed);

        if (!menuConfig.contains(itemPath)) {
            return null;
        }

        Material material = Material.valueOf(menuConfig.getString(itemPath + ".material").toUpperCase());
        String name = formatColor(menuConfig.getString(itemPath + ".name", "").replace("%day%", String.valueOf(day)));
        List<String> lore = new ArrayList<>();

        for (String line : menuConfig.getStringList(itemPath + ".lore")) {
            line = line.replace("%day%", String.valueOf(day));

            if (line.contains("%time%")) {
                if (isNextClaim) {
                    if (isTodayClaimed(playerData)) {
                        line = line.replace("%time%", "завтра");
                    } else {
                        line = line.replace("%time%", "сейчас");
                    }
                } else if (day > nextClaimDay) {
                    line = line.replace("%time%", "скоро");
                } else {
                    line = line.replace("%time%", "недоступно");
                }
            }

            lore.add(formatColor(line));
        }

        // Для голов с текстурами
        if (material == Material.PLAYER_HEAD && menuConfig.contains(itemPath + ".texture")) {
            String texture = menuConfig.getString(itemPath + ".texture");
            return createHeadWithTexture(name, lore, texture);
        }

        return ItemBuilder.createItemStack(material, 1, name, lore);
    }

    private ItemStack createHeadWithTexture(String name, List<String> lore, String texture) {
        try {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(lore);

                // Создаем PlayerProfile с текстурой
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "CalendarReward");
                ProfileProperty property = new ProfileProperty("textures", texture);
                profile.setProperty(property);
                meta.setPlayerProfile(profile);

                head.setItemMeta(meta);
            }

            return head;
        } catch (Exception e) {
            // Если возникла ошибка, создаем обычную голову
            plugin.getLogger().warning("Ошибка при создании головы с текстурой: " + e.getMessage());
            return ItemBuilder.createItemStack(Material.PLAYER_HEAD, 1, name, lore);
        }
    }

    private boolean isTodayClaimed(PlayerData.PlayerCalendarData playerData) {
        String currentDate = getCurrentDate();
        return currentDate.equals(playerData.lastClaimDate);
    }

    private String getCurrentDate() {
        LocalDate today = LocalDate.now();
        return today.toString();
    }

    private String getItemPath(String presentType, boolean isClaimed, boolean canClaim, boolean isExpired) {
        String basePath = "presents." + presentType + ".";

        if (isClaimed) {
            return basePath + "item-claimed";
        } else if (canClaim) {
            return basePath + "item-claimable";
        } else if (isExpired && menuConfig.contains(basePath + "item-expired")) {
            return basePath + "item-expired";
        } else {
            return basePath + "item-unclaimable";
        }
    }

    private String getPresentTypeForDay(int day) {
        if (!menuConfig.contains("presents")) {
            return "weekday"; // fallback
        }

        // Получаем все ключи из раздела presents
        Set<String> presentTypes = menuConfig.getConfigurationSection("presents").getKeys(false);

        for (String type : presentTypes) {
            String daysPath = "presents." + type + ".days";
            if (menuConfig.contains(daysPath)) {
                for (String dayRange : menuConfig.getStringList(daysPath)) {
                    if (isDayInRange(day, dayRange)) {
                        return type;
                    }
                }
            }
        }
        return "weekday"; // fallback
    }

    private boolean isDayInRange(int day, String range) {
        if (range.contains("-")) {
            String[] parts = range.split("-");
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());
            return day >= start && day <= end;
        } else {
            return day == Integer.parseInt(range.trim());
        }
    }

    private Map<Integer, Integer> createDayToSlotMap() {
        Map<Integer, Integer> dayToSlot = new HashMap<>();

        if (!menuConfig.contains("presents")) {
            return dayToSlot;
        }

        Set<String> presentTypes = menuConfig.getConfigurationSection("presents").getKeys(false);
        Set<Integer> allDays = new TreeSet<>();

        // Собираем все дни
        for (String type : presentTypes) {
            String daysPath = "presents." + type + ".days";
            if (menuConfig.contains(daysPath)) {
                for (String dayRange : menuConfig.getStringList(daysPath)) {
                    allDays.addAll(parseDayRange(dayRange));
                }
            }
        }

        // Сопоставляем дни со слотами
        for (String type : presentTypes) {
            String slotsPath = "presents." + type + ".slot";
            if (menuConfig.contains(slotsPath)) {
                List<Integer> slots = parseSlots(menuConfig.getStringList(slotsPath));
                List<Integer> typeDays = new ArrayList<>();

                // Получаем дни для этого типа
                String daysPath = "presents." + type + ".days";
                if (menuConfig.contains(daysPath)) {
                    for (String dayRange : menuConfig.getStringList(daysPath)) {
                        typeDays.addAll(parseDayRange(dayRange));
                    }
                }

                // Сопоставляем слоты с днями
                for (int i = 0; i < typeDays.size() && i < slots.size(); i++) {
                    dayToSlot.put(typeDays.get(i), slots.get(i));
                }
            }
        }

        return dayToSlot;
    }

    private List<Integer> parseSlots(List<String> slotStrings) {
        List<Integer> slots = new ArrayList<>();
        for (String slotStr : slotStrings) {
            if (slotStr.contains("-")) {
                String[] parts = slotStr.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                for (int i = start; i <= end; i++) {
                    slots.add(i);
                }
            } else {
                slots.add(Integer.parseInt(slotStr));
            }
        }
        return slots;
    }

    private List<Integer> parseDayRange(String range) {
        List<Integer> days = new ArrayList<>();
        if (range.contains("-")) {
            String[] parts = range.split("-");
            int start = Integer.parseInt(parts[0].trim());
            int end = Integer.parseInt(parts[1].trim());
            for (int day = start; day <= end; day++) {
                days.add(day);
            }
        } else {
            days.add(Integer.parseInt(range.trim()));
        }
        return days;
    }

    private String formatColor(String text) {
        return ColorUtils.formatColor(text);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public void handleClick(int slot) {
        // Обработка клика по кнопке закрытия
        if (menuConfig.contains("close")) {
            int closeSlot = menuConfig.getInt("close.slot", 49);
            if (slot == closeSlot) {
                player.closeInventory();
                return;
            }
        }

        // Обработка клика по present
        int day = getDayBySlot(slot);
        if (day != -1) {
            handlePresentClick(day);
        }
    }

    private int getDayBySlot(int slot) {
        Map<Integer, Integer> dayToSlot = createDayToSlotMap();
        for (Map.Entry<Integer, Integer> entry : dayToSlot.entrySet()) {
            if (entry.getValue() == slot) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private void sendMessage(String message) {
        plugin.getMessageManager().sendMessage(player, message);
    }

    private void handlePresentClick(int day) {
        // Всегда загружаем свежие данные
        PlayerData.PlayerCalendarData playerData = plugin.getPlayerData().getPlayerData(player);
        int nextClaimDay = playerData.claimedDays.size() + 1;

        if (playerData.claimedDays.contains(day)) {
            sendMessage(plugin.getConfig().getString("error.already-claimed", "&#FF3333Вы уже получили этот подарок!"));
            return;
        }

        if (day == nextClaimDay) {
            // Проверяем, получал ли игрок награду сегодня
            if (isTodayClaimed(playerData)) {
                sendMessage("&cВы уже получали награду сегодня. Следующую награду можно будет получить завтра!");
                return;
            }

            if (hasInventorySpace(player)) {
                plugin.getRewardManager().giveReward(player, day);

                // Обновляем данные и сохраняем
                playerData.claimedDays.add(day);
                playerData.lastClaimDate = getCurrentDate();
                plugin.getPlayerData().savePlayerData(player, playerData);

                updateMenu();
            } else {
                sendMessage("&cНедостаточно места в инвентаре!");
            }
        } else if (day < nextClaimDay) {
            sendMessage("&cВы пропустили этот день!");
        } else {
            sendMessage(plugin.getConfig().getString("error.too-early", "&#FF3333Вы ещё не можете получить этот подарок!"));
        }
    }

    private boolean hasInventorySpace(Player player) {
        return player.getInventory().firstEmpty() != -1;
    }

    private void updateMenu() {
        createInventory(); // Пересоздаем инвентарь с обновленными данными
        player.openInventory(inventory);
    }
}
