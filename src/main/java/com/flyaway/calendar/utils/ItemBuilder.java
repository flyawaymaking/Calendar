package com.flyaway.calendar.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemBuilder {
    private final ItemStack item;

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
    }

    public ItemBuilder setName(String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(formatColor(name));
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }

    public ItemBuilder setLore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(formatColor(lore));
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return item;
    }

    public static ItemStack createItemStack(Material material, int amount, String name, List<String> lore) {
        return new ItemBuilder(material, amount)
                .setName(name)
                .setLore(lore)
                .build();
    }

    public static ItemStack createEnchantedBook(String nbtData) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();

        if (meta != null && nbtData.contains("stored_enchantments")) {
            Pattern pattern = Pattern.compile("\\{([^}]+)}");
            Matcher matcher = pattern.matcher(nbtData);

            if (matcher.find()) {
                String enchantStr = matcher.group(1);
                String[] enchants = enchantStr.split(",");

                for (String enchant : enchants) {
                    String[] parts = enchant.split(":");
                    if (parts.length == 2) {
                        String enchantName = parts[0].trim();
                        int level = Integer.parseInt(parts[1].trim());

                        Enchantment enchantment = getEnchantmentByName(enchantName);
                        if (enchantment != null) {
                            meta.addStoredEnchant(enchantment, level, true);
                        }
                    }
                }
            }
            book.setItemMeta(meta);
        }

        return book;
    }

    private static Enchantment getEnchantmentByName(String name) {
        try {
            // Для Minecraft 1.21
            return switch (name.toLowerCase()) {
                case "protection" -> Enchantment.PROTECTION;
                case "sharpness" -> Enchantment.SHARPNESS;
                case "unbreaking" -> Enchantment.UNBREAKING;
                case "fire_protection" -> Enchantment.FIRE_PROTECTION;
                case "feather_falling" -> Enchantment.FEATHER_FALLING;
                case "blast_protection" -> Enchantment.BLAST_PROTECTION;
                case "projectile_protection" -> Enchantment.PROJECTILE_PROTECTION;
                case "respiration" -> Enchantment.RESPIRATION;
                case "aqua_affinity" -> Enchantment.AQUA_AFFINITY;
                case "thorns" -> Enchantment.THORNS;
                case "depth_strider" -> Enchantment.DEPTH_STRIDER;
                case "frost_walker" -> Enchantment.FROST_WALKER;
                case "mending" -> Enchantment.MENDING;
                case "vanishing_curse" -> Enchantment.VANISHING_CURSE;
                case "binding_curse" -> Enchantment.BINDING_CURSE;
                case "soul_speed" -> Enchantment.SOUL_SPEED;
                case "swift_sneak" -> Enchantment.SWIFT_SNEAK;
                case "wind_burst" -> Enchantment.WIND_BURST;
                case "density" -> Enchantment.DENSITY;
                case "breach" -> Enchantment.BREACH;
                default ->
                    // Пробуем найти через NamespacedKey
                        Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
            };
        } catch (Exception e) {
            return null;
        }
    }

    private String formatColor(String text) {
        return ColorUtils.formatColor(text);
    }

    private List<String> formatColor(List<String> texts) {
        return ColorUtils.formatColor(texts);
    }
}
