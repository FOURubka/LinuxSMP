package com.rubka.linuxsmp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Continuous passive buff of the active distro (felt all the time, not only on Shift+F),
 * plus any collection set bonuses. Refreshed on a short interval.
 */
public class PassiveTask extends BukkitRunnable {

    private record Passive(PotionEffectType type, int amplifier) {
    }

    // Passive buff per ability id - the flavour of "running that distro".
    private static final Map<String, Passive> PASSIVES = Map.ofEntries(
            Map.entry("arch_btw", new Passive(PotionEffectType.STRENGTH, 0)),
            Map.entry("stable_fortress", new Passive(PotionEffectType.HEALTH_BOOST, 1)),
            Map.entry("featherweight", new Passive(PotionEffectType.SPEED, 0)),
            Map.entry("bleeding_edge", new Passive(PotionEffectType.HASTE, 0)),
            Map.entry("pentest", new Passive(PotionEffectType.NIGHT_VISION, 0)),
            Map.entry("community_support", new Passive(PotionEffectType.HEALTH_BOOST, 0)),
            Map.entry("auto_tiling", new Passive(PotionEffectType.JUMP_BOOST, 0)),
            Map.entry("aur_access", new Passive(PotionEffectType.LUCK, 0)),
            Map.entry("compile_everything", new Passive(PotionEffectType.STRENGTH, 1)),
            Map.entry("from_scratch", new Passive(PotionEffectType.STRENGTH, 1)),
            Map.entry("rollback", new Passive(PotionEffectType.RESISTANCE, 0)),
            Map.entry("anonymity", new Passive(PotionEffectType.SPEED, 0)),
            Map.entry("old_school", new Passive(PotionEffectType.STRENGTH, 1)),
            Map.entry("fresh_start", new Passive(PotionEffectType.REGENERATION, 0))
    );

    private final LinuxSMPPlugin plugin;

    public PassiveTask(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerDistroData data = plugin.getPlayerDataManager().get(player);
            applyActive(plugin, player, data);
            Sets.apply(player, data);
        }
    }

    private static void applyActive(LinuxSMPPlugin plugin, Player player, PlayerDistroData data) {
        Distro distro = plugin.getDistroManager().get(data.active);
        if (distro != null && !data.isTempBricked(distro.id()) && !data.mixedBuild) {
            Passive p = PASSIVES.get(distro.ability());
            if (p != null) {
                // ambient, no particles, with icon so the choice is visible in the HUD.
                player.addPotionEffect(new PotionEffect(p.type(), 140, p.amplifier(), true, false, true));
            }
        }
    }

    /**
     * Call whenever the active distro changes: strip the previous passive and
     * immediately re-apply the current distro's passive plus collection sets,
     * so effects don't linger for up to a tick interval after a switch.
     */
    public static void onSwitch(LinuxSMPPlugin plugin, Player player, PlayerDistroData data) {
        // A distro switch is a reinstall: nothing from the previous system survives,
        // including lingering ability buffs.
        for (PotionEffect pe : player.getActivePotionEffects()) {
            player.removePotionEffect(pe.getType());
        }
        applyActive(plugin, player, data);
        Sets.apply(player, data);
    }
}
