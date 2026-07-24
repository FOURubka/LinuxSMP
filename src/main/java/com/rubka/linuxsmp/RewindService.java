package com.rubka.linuxsmp;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * NixOS time machine. Continuously records the last 10 seconds (position, health,
 * food) of every player running a rollback distro; on activation the player is
 * visibly pulled backwards along their own trail and their state 10s ago is restored.
 */
public class RewindService extends BukkitRunnable {

    private record Snapshot(Location loc, double health, int food) {
    }

    private static final int RECORD_PERIOD_TICKS = 2;
    private static final int MAX_SNAPSHOTS = 100;   // 10s of history
    // A player teleport is only accepted once the client acks its teleport id; teleporting
    // every tick outruns the ack and leaves the client unable to move. 4 ticks (200ms)
    // between steps stays above any sane ping, so every step is confirmed.
    private static final int REWIND_STEPS = 12;
    private static final int STEP_PERIOD_TICKS = 4; // 12 * 4 = 48 ticks (~2.4s)
    private static final long COOLDOWN_MS = 45_000L;

    private final LinuxSMPPlugin plugin;
    private final Map<UUID, Deque<Snapshot>> trails = new HashMap<>();
    private final Set<UUID> rewinding = new HashSet<>();

    public RewindService(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        runTaskTimer(plugin, RECORD_PERIOD_TICKS, RECORD_PERIOD_TICKS);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (rewinding.contains(id)) {
                continue;
            }
            PlayerDistroData data = plugin.getPlayerDataManager().get(player);
            Distro distro = plugin.getDistroManager().get(data.active);
            if (distro == null || !"rollback".equals(distro.ability())) {
                trails.remove(id);
                continue;
            }
            Deque<Snapshot> trail = trails.computeIfAbsent(id, k -> new ArrayDeque<>());
            trail.addLast(new Snapshot(player.getLocation().clone(), player.getHealth(), player.getFoodLevel()));
            while (trail.size() > MAX_SNAPSHOTS) {
                trail.removeFirst();
            }
        }
    }

    public boolean isRewinding(Player player) {
        return rewinding.contains(player.getUniqueId());
    }

    /** Pulls the player back along the recorded trail with a progress bar and ghost trail. */
    public void rewind(Player player, PlayerDistroData data) {
        long now = System.currentTimeMillis();
        if (now < data.rollbackCooldownUntil) {
            player.sendActionBar(Text.c("&7Rollback recharging: &f"
                    + (data.rollbackCooldownUntil - now) / 1000 + "s"));
            return;
        }
        Deque<Snapshot> trail = trails.get(player.getUniqueId());
        if (trail == null || trail.size() < 10) {
            player.sendActionBar(Text.c("&cNo generations recorded yet - live a little first."));
            return;
        }
        List<Snapshot> path = new ArrayList<>(trail);   // oldest .. newest
        trails.remove(player.getUniqueId());
        rewinding.add(player.getUniqueId());
        data.rollbackCooldownUntil = now + COOLDOWN_MS;

        // Ghost preview of the whole trail you are about to run back through.
        for (int i = 0; i < path.size(); i += 4) {
            player.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                    path.get(i).loc().clone().add(0, 1, 0), 2, 0.05, 0.05, 0.05, 0);
        }
        player.setInvulnerable(true);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.6f);
        player.sendActionBar(Text.c("&b:: nixos-rebuild --rollback"));
        plugin.getBossBars().countdown(player, "rollback", "<aqua>Rolling back 10s</aqua>",
                BossBar.Color.BLUE, (long) REWIND_STEPS * STEP_PERIOD_TICKS * 50L);

        Snapshot target = path.get(0);
        new BukkitRunnable() {
            int step = 0;
            int lastIdx = path.size() - 1;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    finish();
                    cancel();
                    return;
                }
                if (step >= REWIND_STEPS) {
                    // Cancel and release the player first: if restore() ever throws, the
                    // exception must not leave this timer alive re-teleporting the player.
                    cancel();
                    finish();
                    restore(player, data, target);
                    return;
                }
                double progress = step / (double) REWIND_STEPS;
                int idx = Math.max(0, (int) Math.round((path.size() - 1) * (1.0 - progress)));
                // Draw the whole segment we skip over, so the trail stays as dense as before.
                for (int i = lastIdx; i >= idx; i--) {
                    player.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                            path.get(i).loc().clone().add(0, 1, 0), 3, 0.2, 0.4, 0.2, 0.02);
                }
                lastIdx = idx;
                player.teleport(look(path.get(idx).loc(), player));
                player.setVelocity(new Vector(0, 0, 0));
                player.setFallDistance(0f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME,
                        0.7f, 0.6f + (float) progress);
                step++;
            }

            private void finish() {
                rewinding.remove(player.getUniqueId());
                player.setInvulnerable(false);
            }
        }.runTaskTimer(plugin, 0L, STEP_PERIOD_TICKS);
    }

    /** Recorded position, but the player keeps looking where they are looking now. */
    private static Location look(Location recorded, Player player) {
        Location loc = recorded.clone();
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(player.getLocation().getPitch());
        return loc;
    }

    private void restore(Player player, PlayerDistroData data, Snapshot target) {
        player.teleport(look(target.loc(), player));
        double max = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.clamp(target.health(), 1.0, max));
        player.setFoodLevel(target.food());
        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.getActivePotionEffects().forEach(pe -> player.removePotionEffect(pe.getType()));
        PassiveTask.onSwitch(plugin, player, data);
        // FLASH carries a Color payload on every supported API (1.21.11 - 26.2); without it
        // spawnParticle throws "missing required data class org.bukkit.Color".
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0),
                1, 0, 0, 0, 0, Color.AQUA);
        player.getWorld().spawnParticle(Particle.REVERSE_PORTAL,
                player.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.15);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        player.sendActionBar(Text.c("&b:: rolled back to the previous generation (-10s)"));
        plugin.getBossBars().countdown(player, "rollback",
                "<aqua>Rollback recharging</aqua>", BossBar.Color.BLUE, COOLDOWN_MS);
    }
}
