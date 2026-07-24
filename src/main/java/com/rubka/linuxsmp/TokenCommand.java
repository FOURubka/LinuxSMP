package com.rubka.linuxsmp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TokenCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public TokenCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Text.c("&7Usage: &f/distrotoken <distro> &7- eject a distro as a fusable token"));
            return true;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        Distro distro = plugin.getDistroManager().get(args[0].toLowerCase());
        if (distro == null || !data.unlocked.contains(distro.id())) {
            player.sendMessage(Text.c("&cYou don't have that distro."));
            return true;
        }
        if (data.isTempBricked(distro.id())) {
            player.sendMessage(Text.c("&cCan't eject a bricked distro."));
            return true;
        }
        data.unlocked.remove(distro.id());
        if (distro.id().equals(data.active)) {
            data.active = null;
            data.mixedBuild = false;
            PassiveTask.onSwitch(plugin, player, data);
        }
        player.getInventory().addItem(plugin.getItemFactory().token(distro))
                .values().forEach(left -> player.getWorld().dropItem(player.getLocation(), left));
        player.sendMessage(Text.c("&aEjected " + distro.displayName() + " &aas a token. "
                + "&7Craft two tokens together to fuse, or right-click to reinstall."));
        return true;
    }
}
