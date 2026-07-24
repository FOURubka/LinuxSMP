package com.rubka.linuxsmp;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof LinuxGui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof RosterGUI roster
                && event.getClickedInventory() == event.getInventory()) {
            roster.handleClick(player, event.getRawSlot());
        } else if (event.getInventory().getHolder() instanceof FusionGUI fusion) {
            fusion.handleClick(player, event);
        } else if (event.getInventory().getHolder() instanceof FusionTreeGUI tree
                && event.getClickedInventory() == event.getInventory()) {
            tree.handleClick(player, event.getRawSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof LinuxGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof FusionGUI fusion
                && event.getPlayer() instanceof Player player) {
            fusion.returnInputs(player);
        }
    }
}
