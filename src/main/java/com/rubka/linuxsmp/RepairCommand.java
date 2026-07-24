package com.rubka.linuxsmp;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RepairCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public RepairCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Text.c("&7Usage: &f/distrorepair <distro>"));
            return true;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        Distro distro = plugin.getDistroManager().get(args[0].toLowerCase());
        if (distro == null) {
            player.sendMessage(Text.c("&cUnknown distro."));
            return true;
        }
        int cost = distro.brickRisk().repairCostSoft();

        if (data.isTempBricked(distro.id())) {
            if (data.packages < cost) {
                player.sendMessage(Text.c("&cRepair costs &e" + cost + " Packages&c. You have "
                        + data.packages + "."));
                return true;
            }
            data.packages -= cost;
            data.tempBrickedUntil.remove(distro.id());
            repaired(player, distro);
            return true;
        }

        if (data.permaBricked.contains(distro.id())) {
            ItemStack iso = findRecoveryIso(player);
            if (iso == null) {
                player.sendMessage(Text.c("&cYou need a &bRecovery ISO &cto restore "
                        + distro.displayName() + "&c."));
                return true;
            }
            if (data.packages < cost) {
                player.sendMessage(Text.c("&cRestore also costs &e" + cost + " Packages&c. You have "
                        + data.packages + "."));
                return true;
            }
            data.packages -= cost;
            iso.setAmount(iso.getAmount() - 1);
            data.permaBricked.remove(distro.id());
            data.unlocked.add(distro.id());
            repaired(player, distro);
            return true;
        }

        player.sendMessage(Text.c("&7" + distro.displayName() + " &7is not bricked."));
        return true;
    }

    private ItemStack findRecoveryIso(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.getItemFactory().isRecoveryIso(item)) {
                return item;
            }
        }
        return null;
    }

    private void repaired(Player player, Distro distro) {
        plugin.getBossBars().clear(player, "brick");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.4f);
        player.sendMessage(Text.c("&aReflashed " + distro.displayName() + "&a - system restored."));
    }
}
