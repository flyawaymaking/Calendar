package com.flyaway.calendar.listeners;

import com.flyaway.calendar.CalendarPlugin;
import com.flyaway.calendar.menu.CalendarMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {
    private final CalendarPlugin plugin;

    public MenuListener(CalendarPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        // Проверяем, что это меню нашего плагина
        if (!(holder instanceof CalendarMenu calendarMenu)) {
            return;
        }

        event.setCancelled(true);

        // Проверяем, что клик в верхнем инвентаре
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        // Передаем обработку в класс меню
        calendarMenu.handleClick(event.getRawSlot());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Можно добавить логику очистки если нужно
    }
}
