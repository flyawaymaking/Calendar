package com.flyaway.calendar;

import com.flyaway.calendar.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Pattern;

public class RewardManager {
    private final CalendarPlugin plugin;
    private FileConfiguration rewardsConfig;
    private final List<RewardConfig> sequentialRewards = new ArrayList<>();

    public RewardManager(CalendarPlugin plugin) {
        this.plugin = plugin;
        loadRewards();
    }

    public void loadRewards() {
        File rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
        sequentialRewards.clear();

        // Загружаем награды в последовательном порядке
        loadSequentialRewards();

        plugin.getLogger().info("Загружено " + sequentialRewards.size() + " последовательных наград");
    }

    private void loadSequentialRewards() {
        // Собираем все дни из всех типов наград и сортируем их
        Set<Integer> allDays = new TreeSet<>();
        Map<Integer, RewardConfig> dayToReward = new HashMap<>();

        // Получаем все ключи из корня конфига (все типы наград)
        Set<String> rewardTypes = rewardsConfig.getKeys(false);

        for (String rewardType : rewardTypes) {
            // Пропускаем не-награды (если будут комментарии или другие поля)
            if (!rewardsConfig.contains(rewardType + ".days")) {
                continue;
            }
            processRewardType(rewardType, allDays, dayToReward);
        }

        // Создаем последовательный список наград
        int sequentialDay = 1;
        for (Integer day : allDays) {
            RewardConfig originalReward = dayToReward.get(day);
            if (originalReward != null) {
                RewardConfig sequentialReward = new RewardConfig();
                sequentialReward.sequentialDay = sequentialDay;
                sequentialReward.items = originalReward.items;
                sequentialReward.commands = originalReward.commands;
                sequentialReward.message = originalReward.message;
                sequentialRewards.add(sequentialReward);
                sequentialDay++;
            }
        }

        plugin.getLogger().info("Загружено " + sequentialRewards.size() + " последовательных наград из " + rewardTypes.size() + " типов наград");
    }

    private void processRewardType(String type, Set<Integer> allDays, Map<Integer, RewardConfig> dayToReward) {
        if (rewardsConfig.contains(type)) {
            RewardConfig rewardConfig = new RewardConfig();
            rewardConfig.days = rewardsConfig.getStringList(type + ".days");
            rewardConfig.commands = rewardsConfig.getStringList(type + ".commands");
            rewardConfig.message = rewardsConfig.getString(type + ".message", "");

            // Загружаем предметы
            if (rewardsConfig.contains(type + ".items")) {
                List<Map<?, ?>> items = rewardsConfig.getMapList(type + ".items");
                for (Map<?, ?> itemMap : items) {
                    try {
                        RewardItem item = new RewardItem();
                        item.material = Material.valueOf(itemMap.get("material").toString().toUpperCase());
                        item.name = itemMap.get("name").toString();
                        item.amount = Integer.parseInt(itemMap.get("amount").toString());
                        rewardConfig.items.add(item);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Неверный материал в награде " + type + ": " + itemMap.get("material"));
                    }
                }
            }

            // Добавляем дни этого типа в общий список
            for (String range : rewardConfig.days) {
                for (Integer day : parseDayRange(range)) {
                    allDays.add(day);
                    dayToReward.put(day, rewardConfig);
                }
            }
        }
    }

    private List<Integer> parseDayRange(String range) {
        List<Integer> days = new ArrayList<>();
        if (range.contains("-")) {
            String[] parts = range.split("-");
            try {
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                for (int day = start; day <= end; day++) {
                    days.add(day);
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Неверный формат диапазона дней: " + range);
            }
        } else {
            try {
                days.add(Integer.parseInt(range.trim()));
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Неверный формат дня: " + range);
            }
        }
        return days;
    }

    public RewardConfig getRewardForSequentialDay(int sequentialDay) {
        if (sequentialDay > 0 && sequentialDay <= sequentialRewards.size()) {
            return sequentialRewards.get(sequentialDay - 1);
        }
        return null;
    }

    public int getTotalRewards() {
        return sequentialRewards.size();
    }

    public void giveReward(Player player, int sequentialDay) {
        RewardConfig rewardConfig = getRewardForSequentialDay(sequentialDay);

        if (rewardConfig == null) {
            plugin.getLogger().warning("Награда не найдена для последовательного дня " + sequentialDay);
            return;
        }

        // Выдаем предметы
        for (RewardItem item : rewardConfig.items) {
            ItemStack itemStack = createItemStack(item);
            if (itemStack != null) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);

                // Если инвентарь полный, выкидываем предметы на землю
                for (ItemStack left : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                    plugin.getMessageManager().sendMessage(player, "&cПредмет " + itemStack.getType() + " выпал на землю из-за нехватки места в инвентаре!");
                }
            }
        }

        // Выполняем команды
        for (String command : rewardConfig.commands) {
            executeCommand(player, command);
        }

        // Отправляем сообщение
        if (!rewardConfig.message.isEmpty()) {
            String formattedMessage = rewardConfig.message.replace("%day%", String.valueOf(sequentialDay));
            plugin.getMessageManager().sendMessage(player, formattedMessage);
        }

        // Проигрываем звук
        playClaimSound(player);
    }

    private ItemStack createItemStack(RewardItem rewardItem) {
        try {
            return ItemBuilder.createItemStack(
                rewardItem.material,
                rewardItem.amount,
                rewardItem.name,
                Collections.emptyList()
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка создания предмета: " + rewardItem.material);
            return null;
        }
    }

    private void executeCommand(Player player, String command) {
        try {
            String formattedCommand = command.replace("%player%", player.getName());

            // Обрабатываем специальные команды с NBT-тегами
            if (formattedCommand.startsWith("give ") && formattedCommand.contains("[")) {
                giveItemWithNBT(player, formattedCommand);
            } else {
                // Обычные команды
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), formattedCommand);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка выполнения команды: " + command);
            e.printStackTrace();
        }
    }

    private void giveItemWithNBT(Player player, String command) {
        try {
            Pattern pattern = Pattern.compile("give\\s+\\S+\\s+(\\w+)\\[([^]]+)]\\s+(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(command);

            if (matcher.find()) {
                String materialName = matcher.group(1).toUpperCase();
                String nbtData = matcher.group(2);
                int amount = Integer.parseInt(matcher.group(3));

                if (materialName.equals("ENCHANTED_BOOK")) {
                    ItemStack enchantedBook = ItemBuilder.createEnchantedBook(nbtData);
                    enchantedBook.setAmount(amount);

                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(enchantedBook);
                    for (ItemStack left : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка парсинга NBT команды: " + command);
            e.printStackTrace();
        }
    }

    private void playClaimSound(Player player) {
        // Реализация звуков
    }

    public int getCurrentMonth() {
        return LocalDate.now().getMonthValue();
    }

    public int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    public int getDaysInCurrentMonth() {
        YearMonth yearMonth = YearMonth.of(getCurrentYear(), getCurrentMonth());
        return yearMonth.lengthOfMonth();
    }

    public int getCurrentDayOfMonth() {
        return LocalDate.now().getDayOfMonth();
    }

    public static class RewardConfig {
        public int sequentialDay;
        public List<String> days = new ArrayList<>();
        public List<RewardItem> items = new ArrayList<>();
        public List<String> commands = new ArrayList<>();
        public String message = "";
    }

    public static class RewardItem {
        public Material material;
        public String name;
        public int amount;
    }
}
