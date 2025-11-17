package com.flyaway.calendar.holders;

import com.flyaway.calendar.CalendarPlugin;
import com.flyaway.calendar.PlayerCalendarData;
import com.flyaway.calendar.managers.MenuConfigManager;
import com.flyaway.calendar.managers.MessageManager;
import com.flyaway.calendar.managers.RewardManager;
import com.flyaway.calendar.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.time.LocalDate;
import java.util.*;

public class MenuHolder implements InventoryHolder {
    private final CalendarPlugin plugin;
    private final Player player;
    private Inventory inventory;
    private final MenuConfigManager menuConfigManager;
    private final MessageManager messageManager;
    private final RewardManager rewardManager;

    public MenuHolder(CalendarPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.menuConfigManager = plugin.getMenuConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.rewardManager = plugin.getRewardManager();
        createInventory();
    }

    public static void openCalendarMenu(CalendarPlugin plugin, Player player) {
        MenuHolder menuHolder = new MenuHolder(plugin, player);
        player.openInventory(menuHolder.getInventory());
    }

    private void createInventory() {
        Component title = messageManager.convertToComponent(menuConfigManager.getTitle());
        int rows = menuConfigManager.getRows();
        this.inventory = Bukkit.createInventory(this, rows * 9, title);

        fillBackground();
        addCloseButton();
        addPresents();
    }

    private void fillBackground() {
        ConfigurationSection fillerSection = menuConfigManager.getFillerSection();
        if (fillerSection != null) {
            Material material = Material.valueOf(fillerSection.getString("material", "").toUpperCase());
            Component name = messageManager.convertToComponent(fillerSection.getString("name"));

            String loreTemplate = messageManager.formatMessage(fillerSection.getString("lore"), null);
            String[] loreLines = loreTemplate.split("\n");
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(messageManager.convertToComponent(line));
            }

            List<Integer> slots = parseSlots(fillerSection.getStringList("slot"));

            ItemStack fillerItem = ItemBuilder.createItemStack(material, 1, name, lore);
            for (int slot : slots) {
                if (slot < inventory.getSize()) {
                    inventory.setItem(slot, fillerItem);
                }
            }
        }
    }

    private void addCloseButton() {
        ConfigurationSection closeSection = menuConfigManager.getCloseSection();
        if (closeSection != null) {
            Material material = Material.valueOf(closeSection.getString("material", "").toUpperCase());
            Component name = messageManager.convertToComponent(closeSection.getString("name"));

            String loreTemplate = messageManager.formatMessage(closeSection.getString("lore"), null);
            String[] loreLines = loreTemplate.split("\n");
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(messageManager.convertToComponent(line));
            }

            int slot = closeSection.getInt("slot", 49);

            ItemStack closeItem = ItemBuilder.createItemStack(material, 1, name, lore);
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, closeItem);
            }
        }
    }

    private void addPresents() {
        // Получаем данные игрока
        int nextClaimDay = plugin.getPlayerData().getPlayerData(player).getNextClaimDay();
        int totalRewards = rewardManager.getTotalRewards();

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
        ConfigurationSection presentsSection = menuConfigManager.getPresentsSection();
        if (presentsSection == null) return null;

        PlayerCalendarData playerData = plugin.getPlayerData().getPlayerData(player);
        boolean isClaimed = playerData.hasClaimedDay(day);
        boolean isNextClaim = day == nextClaimDay;

        boolean canClaim = isNextClaim && !isTodayClaimed(playerData);

        String presentType = getPresentTypeForDay(day);
        String presentPath = getPresentPath(presentsSection, presentType, isClaimed, canClaim, day < nextClaimDay && !isClaimed);

        if (!presentsSection.contains(presentPath)) {
            return null;
        }

        Material material = Material.valueOf(presentsSection.getString(presentPath + ".material", "").toUpperCase());
        Component name = messageManager.convertToComponent(
                presentsSection.getString(presentPath + ".name"),
                Map.of("day", String.valueOf(day))
        );

        String time = getTimeStatus(day, nextClaimDay, isTodayClaimed(playerData));

        String loreTemplate = messageManager.formatMessage(
                presentsSection.getString(presentPath + ".lore"),
                Map.of("day", String.valueOf(day), "time", time)
        );

        String[] loreLines = loreTemplate.split("\n");
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(messageManager.convertToComponent(line));
        }

        // Для голов с текстурами
        if (material == Material.PLAYER_HEAD && presentsSection.contains(presentPath + ".texture")) {
            String texture = presentsSection.getString(presentPath + ".texture", "");
            return createHeadWithTexture(name, lore, texture);
        }

        return ItemBuilder.createItemStack(material, 1, name, lore);
    }

    private String getTimeStatus(int day, int nextClaimDay, boolean isTodayClaimed) {
        if (day == nextClaimDay) {
            return isTodayClaimed ? messageManager.getMessage("time-until-claim.today-claimed") : messageManager.getMessage("time-until-claim.available-now");
        }
        return day > nextClaimDay ? messageManager.getMessage("time-until-claim.coming-soon") : messageManager.getMessage("time-until-claim.unavailable");
    }

    private ItemStack createHeadWithTexture(Component name, List<Component> lore, String texture) {
        try {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                if (!name.equals(Component.empty())) {
                    meta.displayName(name);
                }
                if (!lore.equals(Component.empty())) {
                    meta.lore(lore);
                }

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

    private boolean isTodayClaimed(PlayerCalendarData playerData) {
        String currentDate = getCurrentDate();
        return currentDate.equals(playerData.getLastClaimDate());
    }

    private String getCurrentDate() {
        LocalDate today = LocalDate.now();
        return today.toString();
    }

    private String getPresentPath(ConfigurationSection presentsSection, String presentType, boolean isClaimed, boolean canClaim, boolean isExpired) {
        String basePath = presentType + ".";
        if (isClaimed) {
            return basePath + "item-claimed";
        } else if (canClaim) {
            return basePath + "item-claimable";
        } else if (isExpired && presentsSection.contains(basePath + "item-expired")) {
            return basePath + "item-expired";
        }
        return basePath + "item-unclaimable";
    }

    private String getPresentTypeForDay(int day) {
        ConfigurationSection presentsSection = menuConfigManager.getPresentsSection();
        if (presentsSection != null) {
            Set<String> presentTypes = presentsSection.getKeys(false);

            for (String type : presentTypes) {
                String daysPath = type + ".days";
                if (presentsSection.contains(daysPath)) {
                    for (String dayRange : presentsSection.getStringList(daysPath)) {
                        if (isDayInRange(day, dayRange)) {
                            return type;
                        }
                    }
                }
            }
        }
        return "weekday";
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
        ConfigurationSection presentsSection = menuConfigManager.getPresentsSection();
        Map<Integer, Integer> dayToSlot = new HashMap<>();
        if (presentsSection != null) {
            Set<String> presentTypes = presentsSection.getKeys(false);

            // Сопоставляем дни со слотами
            for (String type : presentTypes) {
                String slotsPath = type + ".slot";
                if (presentsSection.contains(slotsPath)) {
                    List<Integer> slots = parseSlots(presentsSection.getStringList(slotsPath));
                    List<Integer> typeDays = new ArrayList<>();

                    // Получаем дни для этого типа
                    String daysPath = type + ".days";
                    if (presentsSection.contains(daysPath)) {
                        for (String dayRange : presentsSection.getStringList(daysPath)) {
                            typeDays.addAll(parseDayRange(dayRange));
                        }
                    }

                    // Сопоставляем слоты с днями
                    for (int i = 0; i < typeDays.size() && i < slots.size(); i++) {
                        dayToSlot.put(typeDays.get(i), slots.get(i));
                    }
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(int slot) {
        // Обработка клика по кнопке закрытия
        ConfigurationSection closeSection = menuConfigManager.getCloseSection();
        if (closeSection != null) {
            int closeSlot = closeSection.getInt("slot", 49);
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

    private void handlePresentClick(int day) {
        // Всегда загружаем свежие данные
        PlayerCalendarData playerData = plugin.getPlayerData().getPlayerData(player);
        int nextClaimDay = playerData.getNextClaimDay();

        if (playerData.hasClaimedDay(day)) {
            messageManager.sendMessageByKey(player, "error-already-claimed");
            return;
        }

        if (day == nextClaimDay) {
            // Проверяем, получал ли игрок награду сегодня
            if (isTodayClaimed(playerData)) {
                messageManager.sendMessageByKey(player, "already-claimed-today");
                return;
            }

            if (hasInventorySpace(player)) {
                plugin.getRewardManager().giveReward(player, day);

                // Обновляем данные и сохраняем
                playerData.claimDay(day, getCurrentDate());
                plugin.getPlayerData().savePlayerData(player, playerData);

                updateMenu();
            } else {
                messageManager.sendMessageByKey(player, "error-no-space");
            }
        } else if (day < nextClaimDay) {
            messageManager.sendMessageByKey(player, "error-missed-day");
        } else {
            messageManager.sendMessageByKey(player, "error-too-early");
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
