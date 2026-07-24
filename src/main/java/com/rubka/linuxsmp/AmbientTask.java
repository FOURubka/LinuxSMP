package com.rubka.linuxsmp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/** Subtle rarity-coloured aura around each player's active distro. */
public class AmbientTask extends BukkitRunnable {

    private final LinuxSMPPlugin plugin;
    private double phase;

    public AmbientTask(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        phase += 0.4;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerDistroData data = plugin.getPlayerDataManager().get(player);
            Distro distro = plugin.getDistroManager().get(data.active);
            if (distro == null) {
                continue;
            }
            // Only auras for EPIC+ so the world does not get too busy.
            if (distro.rarity().ordinal() >= Rarity.EPIC.ordinal()) {
                Fx.helix(player, Fx.rarityColor(distro.rarity()), phase);
            }
        }
    }
}
