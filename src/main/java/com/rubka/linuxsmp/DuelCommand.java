package com.rubka.linuxsmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public DuelCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Text.c("&7Usage: &f/distroduel <player|accept|deny>"));
            return true;
        }
        ArenaManager arena = plugin.getArenaManager();
        switch (args[0].toLowerCase()) {
            case "accept" -> arena.accept(player);
            case "deny" -> arena.deny(player);
            default -> {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    player.sendMessage(Text.c("&cPlayer not found."));
                } else {
                    arena.challenge(player, target);
                }
            }
        }
        return true;
    }
}
