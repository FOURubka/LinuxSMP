package com.rubka.linuxsmp;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies the active distro's environmental synergy every second, and toasts the
 * player when a synergy switches on or off so the "right distro for the moment"
 * choice is felt live.
 */
public class SynergyTask extends BukkitRunnable {

    private final LinuxSMPPlugin plugin;
    private final Map<UUID, String> activeLabel = new HashMap<>();

    public SynergyTask(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerDistroData data = plugin.getPlayerDataManager().get(player);
            Distro distro = plugin.getDistroManager().get(data.active);

            Synergy.Bonus bonus = null;
            if (distro != null && !data.isTempBricked(distro.id()) && !data.mixedBuild) {
                Synergy.Bonus candidate = Synergy.of(distro.id());
                if (candidate != null && candidate.when().test(player)) {
                    bonus = candidate;
                }
            }

            String previous = activeLabel.get(player.getUniqueId());
            if (bonus != null) {
                player.addPotionEffects(bonus.effects());
                if (!bonus.label().equals(previous)) {
                    activeLabel.put(player.getUniqueId(), bonus.label());
                    player.sendActionBar(Text.c("&a✔ Synergy: &f" + bonus.label()));
                    player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.6f);
                }
            } else if (previous != null) {
                activeLabel.remove(player.getUniqueId());
                player.sendActionBar(Text.c("&8Synergy faded: " + previous));
            }
        }
    }
}
