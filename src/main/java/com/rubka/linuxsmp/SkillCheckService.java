package com.rubka.linuxsmp;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class SkillCheckService {

    public enum CheckType { MAINTENANCE, SWAP, FUSION, ABILITY }

    private final LinuxSMPPlugin plugin;

    public SkillCheckService(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public double failChance(PlayerDistroData data, Distro distro, CheckType type) {
        var config = plugin.getConfig();
        double modifier = config.getDouble("skill-check.modifiers." + type.name().toLowerCase(Locale.ROOT), 1.0);
        double fail = distro.brickRisk().baseFailChance()
                + distro.learningCurve() * modifier * config.getDouble("skill-check.learning-curve-factor", 0.01)
                - data.skillLevel * config.getDouble("skill-check.skill-factor", 0.001);
        double min = config.getDouble("skill-check.min-fail-chance", 0.01);
        double max = config.getDouble("skill-check.max-fail-chance", 0.6);
        return Math.clamp(fail, min, max);
    }

    /** Rolls the check. On success grants skill. Does NOT brick on fail - callers decide. */
    public boolean check(PlayerDistroData data, Distro distro, CheckType type) {
        boolean success = ThreadLocalRandom.current().nextDouble() >= failChance(data, distro, type);
        if (success) {
            int gain = plugin.getConfig().getInt("skill-check.skill-gain-per-success", 1);
            int max = plugin.getConfig().getInt("skill-check.max-skill", 200);
            data.skillLevel = Math.min(max, data.skillLevel + gain);
        }
        return success;
    }

    /** Applies the brick outcome for the given distro and shows kernel-panic feedback. */
    public void brick(Player player, PlayerDistroData data, Distro distro) {
        new BrickAnimation(plugin, player, distro).start();
        if (distro.brickRisk().permaBrick()) {
            data.unlocked.remove(distro.id());
            data.permaBricked.add(distro.id());
            kernelPanic(player, distro.displayName() + " &4is PERMANENTLY bricked!");
            player.sendMessage(Text.c("&7A &bRecovery ISO &7and &f/distrorepair " + distro.id()
                    + " &7can restore it."));
        } else {
            long durationMs = distro.brickRisk().tempBrickDurationSeconds() * 1000L;
            data.tempBrickedUntil.put(distro.id(), System.currentTimeMillis() + durationMs);
            kernelPanic(player, distro.displayName() + " &cis bricked for "
                    + distro.brickRisk().tempBrickDurationSeconds() + "s");
            player.sendMessage(Text.c("&7Repair early: &f/distrorepair " + distro.id()
                    + " &7(&e" + distro.brickRisk().repairCostSoft() + " Packages&7)"));
            plugin.getBossBars().countdown(player, "brick",
                    "<red>Bricked: " + distro.displayName().replaceAll("&[0-9a-fk-or]", "") + "</red>",
                    BossBar.Color.RED, durationMs);
        }
        if (distro.id().equals(data.active)) {
            data.active = null;
            data.mixedBuild = false;
            PassiveTask.onSwitch(plugin, player, data);
        }
    }

    public void kernelPanic(Player player, String subtitle) {
        player.showTitle(Title.title(
                Text.c("&4&lKERNEL PANIC"),
                Text.c("&csystem halted"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        player.sendMessage(Text.c("&4&l[KERNEL PANIC] &c" + subtitle));
        player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.7f);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 1.0f, 0.5f);
    }
}
