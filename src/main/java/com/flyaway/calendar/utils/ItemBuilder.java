package com.flyaway.calendar.utils;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemBuilder {
    private final ItemStack item;

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
    }

    public ItemBuilder setName(Component name) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && !name.equals(Component.empty())) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder setLore(List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && !lore.equals(Component.empty())) {
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return item;
    }

    public static ItemStack createItemStack(Material material, int amount, Component name, List<Component> lore) {
        return new ItemBuilder(material, amount).setName(name).setLore(lore).build();
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
            if (name == null || name.isBlank()) return null;

            String key = name.trim().toLowerCase();

            NamespacedKey nsKey = NamespacedKey.minecraft(key);

            Registry<@NotNull Enchantment> enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

            return enchantmentRegistry.get(nsKey);
        } catch (Exception e) {
            return null;
        }
    }
}
