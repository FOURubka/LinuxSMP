package com.rubka.linuxsmp;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MaintenanceCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public MaintenanceCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        Distro distro = plugin.getDistroManager().get(data.active);
        if (distro == null) {
            player.sendMessage(Text.c("&cNo active distro. Roll one with &f/distroroll"));
            return true;
        }

        SkillCheckService checks = plugin.getSkillCheckService();
        if (!checks.check(data, distro, SkillCheckService.CheckType.MAINTENANCE)) {
            checks.brick(player, data, distro);
            return true;
        }
        data.lastMaintenance = System.currentTimeMillis();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
        player.sendMessage(Text.c("&a:: System is up to date. &7(" + distro.displayName() + "&7)"));
        return true;
    }
}
