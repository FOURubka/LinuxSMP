package com.rubka.linuxsmp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class MaintenanceTask extends BukkitRunnable {

    private final LinuxSMPPlugin plugin;

    public MaintenanceTask(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        var config = plugin.getConfig();
        long intervalMs = config.getInt("maintenance.interval-seconds", 300) * 1000L;
        int debuffTicks = config.getInt("maintenance.debuff-seconds", 8) * 20;
        int minCurve = config.getInt("piloting.min-curve-for-penalty", 6);
        int reqPerPoint = config.getInt("piloting.required-skill-per-point", 8);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerDistroData data = plugin.getPlayerDataManager().get(player);
            Distro distro = plugin.getDistroManager().get(data.active);
            if (distro == null) {
                continue;
            }

            // Piloting penalty: hard distro, under-leveled player.
            int curve = distro.learningCurve();
            if (curve >= minCurve && data.skillLevel < curve * reqPerPoint) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 7 * 20, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 7 * 20, 0, true, false));
            }

            // Upkeep: high-maintenance distro or mixed build.
            boolean needsUpkeep = distro.highMaintenance() || data.mixedBuild;
            if (needsUpkeep && System.currentTimeMillis() - data.lastMaintenance > intervalMs) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, debuffTicks, 0, true, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, debuffTicks, 0, true, false));
                player.sendActionBar(Component.text()
                        .append(Text.c("&cSystem degraded - run &f/pacman-syu"))
                        .build());
            }
        }
    }
}
