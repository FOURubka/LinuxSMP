package com.rubka.linuxsmp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RosterCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public RosterCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        new RosterGUI(plugin, player).open(player);
        return true;
    }
}
