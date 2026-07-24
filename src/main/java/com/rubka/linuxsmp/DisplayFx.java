package com.rubka.linuxsmp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Display-entity animation helpers. All spawned displays are non-persistent
 * and remove themselves when their animation ends.
 */
public final class DisplayFx {

    private DisplayFx() {
    }

    private static BlockDisplay spawnBlock(Location loc, Material mat, float scale) {
        return loc.getWorld().spawn(loc, BlockDisplay.class, d -> {
            d.setBlock(mat.createBlockData());
            d.setPersistent(false);
            d.setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 0, -scale / 2), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
        });
    }

    /** Expanding horizontal shockwave ring of small block displays. */
    public static void ringWave(LinuxSMPPlugin plugin, Location center, Material mat,
                                double maxRadius, int points, int durationTicks) {
        Location base = center.clone().add(0, 0.1, 0);
        List<BlockDisplay> ring = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            ring.add(spawnBlock(base, mat, 0.35f));
        }
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= durationTicks) {
                    ring.forEach(Entity::remove);
                    cancel();
                    return;
                }
                double r = 0.5 + (maxRadius - 0.5) * tick / (double) durationTicks;
                for (int i = 0; i < ring.size(); i++) {
                    double a = 2 * Math.PI * i / ring.size();
                    ring.get(i).teleport(base.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Items orbiting a player at chest height with a slow bob; removed after durationTicks. */
    public static void orbitItems(LinuxSMPPlugin plugin, Player player, List<ItemStack> items,
                                  double radius, int durationTicks) {
        List<ItemDisplay> orbs = new ArrayList<>();
        for (ItemStack it : items) {
            orbs.add(player.getWorld().spawn(player.getLocation(), ItemDisplay.class, d -> {
                d.setItemStack(it);
                d.setPersistent(false);
                d.setBillboard(Display.Billboard.CENTER);
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(0.6f, 0.6f, 0.6f), new AxisAngle4f()));
            }));
        }
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= durationTicks || !player.isOnline()) {
                    orbs.forEach(Entity::remove);
                    cancel();
                    return;
                }
                Location base = player.getLocation().add(0, 1.2, 0);
                double phase = tick * 0.25;
                for (int i = 0; i < orbs.size(); i++) {
                    double a = phase + 2 * Math.PI * i / orbs.size();
                    orbs.get(i).teleport(base.clone().add(
                            Math.cos(a) * radius, Math.sin(tick * 0.15 + i) * 0.25, Math.sin(a) * radius));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** A block display that falls from {@code height} above target and lands in fallTicks. */
    public static void drop(LinuxSMPPlugin plugin, Location target, Material mat,
                            double height, int fallTicks, Runnable onLand) {
        Location start = target.clone().add(0, height, 0);
        BlockDisplay box = spawnBlock(start, mat, 0.9f);
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= fallTicks) {
                    box.remove();
                    onLand.run();
                    cancel();
                    return;
                }
                double p = (tick + 1) / (double) fallTicks;
                box.teleport(start.clone().add(0, -height * p * p, 0)); // accelerating fall
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Rising tower of block-display rings stacking up around center (one material per
     * layer), then a shatter burst that removes everything.
     */
    public static void tower(LinuxSMPPlugin plugin, Location center, List<Material> layers,
                             int ticksPerLayer, int holdTicks) {
        List<BlockDisplay> parts = new ArrayList<>();
        new BukkitRunnable() {
            int layer = 0;
            int hold = 0;

            @Override
            public void run() {
                if (layer < layers.size()) {
                    double y = 0.2 + layer * 0.9;
                    Material mat = layers.get(layer);
                    for (int i = 0; i < 6; i++) {
                        double a = 2 * Math.PI * i / 6 + layer * 0.4;
                        parts.add(spawnBlock(center.clone().add(Math.cos(a) * 1.6, y, Math.sin(a) * 1.6), mat, 0.5f));
                    }
                    center.getWorld().playSound(center, Sound.BLOCK_STONE_PLACE, 1f, 0.7f + layer * 0.15f);
                    layer++;
                    return;
                }
                if (hold++ < holdTicks / Math.max(1, ticksPerLayer)) {
                    return;
                }
                for (BlockDisplay part : parts) {
                    part.getWorld().spawnParticle(Particle.END_ROD, part.getLocation(), 3, 0.1, 0.1, 0.1, 0.15);
                    part.remove();
                }
                cancel();
            }
        }.runTaskTimer(plugin, 0L, ticksPerLayer);
    }

    /** Floating text label that follows an entity, removed after ticks. */
    public static void tag(LinuxSMPPlugin plugin, LivingEntity target, String legacyText, int ticks) {
        TextDisplay td = target.getWorld().spawn(target.getLocation().add(0, 2.3, 0), TextDisplay.class, d -> {
            d.text(Text.c(legacyText));
            d.setPersistent(false);
            d.setBillboard(Display.Billboard.CENTER);
            d.setDefaultBackground(false);
        });
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick++ >= ticks || !target.isValid()) {
                    td.remove();
                    cancel();
                    return;
                }
                td.teleport(target.getLocation().add(0, 2.3, 0));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
