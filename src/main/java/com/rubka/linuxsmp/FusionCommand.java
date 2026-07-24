package com.rubka.linuxsmp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FusionCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public FusionCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("recipes")) {
            new FusionTreeGUI(plugin).open(player);
            return true;
        }
        new FusionGUI(plugin).open(player);
        return true;
    }
}
