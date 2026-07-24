package com.rubka.linuxsmp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/** Per-distro shortcut command (e.g. /nixos) that boots the distro and fires its ability. */
public class DistroAbilityCommand implements CommandExecutor, TabCompleter {

    private final LinuxSMPPlugin plugin;

    public DistroAbilityCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        Distro distro = plugin.getDistroManager().get(command.getName().toLowerCase());
        if (distro == null) {
            player.sendMessage(Text.c("&cUnknown distro command."));
            return true;
        }
        plugin.getAbilityListener().activateAs(player, distro);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Distro distro = plugin.getDistroManager().get(command.getName().toLowerCase());
        if (distro == null || args.length != 1) {
            return List.of();
        }
        String keyword = Abilities.of(distro.ability()).name().toLowerCase().replace(' ', '-');
        return keyword.startsWith(args[0].toLowerCase()) ? List.of(keyword) : List.of();
    }
}
