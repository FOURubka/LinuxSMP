package com.rubka.linuxsmp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SwapCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public SwapCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Text.c("&7Usage: &f/distroswap <distro>"));
            return true;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        Distro target = plugin.getDistroManager().get(args[0].toLowerCase());
        if (target == null || !data.unlocked.contains(target.id())) {
            player.sendMessage(Text.c("&cYou don't have that distro. See &f/distros"));
            return true;
        }
        if (target.id().equals(data.active)) {
            player.sendMessage(Text.c("&7" + target.displayName() + " &7is already active."));
            return true;
        }
        if (data.isTempBricked(target.id())) {
            player.sendMessage(Text.c("&c" + target.displayName() + " &cis bricked for another "
                    + data.tempBrickSecondsLeft(target.id()) + "s."));
            return true;
        }

        long now = System.currentTimeMillis();
        var config = plugin.getConfig();

        // First install with no active distro: free, no check.
        if (data.active == null) {
            data.active = target.id();
            data.lastMaintenance = now;
            PassiveTask.onSwitch(plugin, player, data);
            player.sendMessage(Text.c("&aBooted " + target.displayName() + "&a."));
            return true;
        }

        if (now < data.swapCooldownUntil) {
            player.sendMessage(Text.c("&cReconfiguring on cooldown: &f"
                    + (data.swapCooldownUntil - now) / 1000 + "s"));
            return true;
        }
        data.swapCooldownUntil = now + config.getInt("swap.cooldown-seconds", 15) * 1000L;

        SkillCheckService checks = plugin.getSkillCheckService();
        if (!checks.check(data, target, SkillCheckService.CheckType.SWAP)) {
            checks.brick(player, data, target);
            return true;
        }

        boolean inCombat = plugin.getCombatTracker()
                .inCombat(player, config.getInt("swap.combat-window-seconds", 10));
        data.active = target.id();
        data.lastMaintenance = now;
        // Mid-battle swap = a hacked-together respec: the build becomes "mixed"
        // and permanently high-maintenance. A clean swap out of combat resets it.
        data.mixedBuild = inCombat;
        PassiveTask.onSwitch(plugin, player, data);

        int penalty = config.getInt("swap.penalty-slowness-seconds", 5);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, penalty * 20, 1));
        player.sendMessage(Text.c("&aSwapped to " + target.displayName()
                + "&a. &7Reconfiguring penalty: Slowness " + penalty + "s."));
        if (inCombat) {
            player.sendMessage(Text.c("&eMid-battle respec - your build is now &6mixed&e: "
                    + "it needs regular &f/pacman-syu &eupkeep."));
        }
        return true;
    }
}
