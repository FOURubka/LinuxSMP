package com.rubka.linuxsmp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Temporary, self-reverting block transforms. Abilities can reshape the world for
 * a few seconds (compile terrain, raise a fortress, tile the floor) without leaving
 * permanent grief: originals are snapshotted and restored on a timer.
 */
public class BlockTransformService {

    private record Saved(Block block, BlockData data, Material to) {
    }

    // Never touched: unbreakable, functional or item-holding blocks.
    private static final Set<Material> PROTECTED = EnumSet.of(
            Material.BEDROCK, Material.BARRIER, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK, Material.STRUCTURE_BLOCK, Material.JIGSAW,
            Material.END_PORTAL, Material.END_PORTAL_FRAME, Material.NETHER_PORTAL, Material.END_GATEWAY,
            Material.SPAWNER, Material.RESPAWN_ANCHOR, Material.BEACON, Material.CONDUIT);

    private final LinuxSMPPlugin plugin;

    public BlockTransformService(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean safe(Block b) {
        if (PROTECTED.contains(b.getType())) {
            return false;
        }
        return !(b.getState() instanceof InventoryHolder); // never eat chests, furnaces, etc.
    }

    /** Convert every solid (non-passable) block in a sphere to {@code to}. Reveal/compile style. */
    public void fillSolid(Location center, int radius, Material to, long revertTicks) {
        transform(center, radius, to, revertTicks,
                b -> b.getType().isSolid() && !b.isPassable());
    }

    /** Build a hollow shell at the sphere surface into passable space. Fortress style. */
    public void encase(Location center, int radius, Material to, long revertTicks) {
        World world = center.getWorld();
        List<Saved> saved = new ArrayList<>();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        double outer = radius + 0.5, inner = radius - 0.5;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius + 1; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d < inner || d > outer) {
                        continue;
                    }
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (!safe(b) || !b.isPassable() || b.isLiquid()) {
                        continue;
                    }
                    saved.add(new Saved(b, b.getBlockData(), to));
                    b.setType(to, false);
                }
            }
        }
        scheduleRevert(saved, revertTicks);
    }

    /** Lay a flat square platform one block below the feet, filling passable space. Tiling style. */
    public void platform(Location center, int half, Material to, long revertTicks) {
        World world = center.getWorld();
        List<Saved> saved = new ArrayList<>();
        int cx = center.getBlockX(), cy = center.getBlockY() - 1, cz = center.getBlockZ();
        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                Block b = world.getBlockAt(cx + x, cy, cz + z);
                if (!safe(b) || !b.isPassable() || b.isLiquid()) {
                    continue;
                }
                saved.add(new Saved(b, b.getBlockData(), to));
                b.setType(to, false);
            }
        }
        scheduleRevert(saved, revertTicks);
    }

    private void transform(Location center, int radius, Material to, long revertTicks,
                           java.util.function.Predicate<Block> filter) {
        World world = center.getWorld();
        List<Saved> saved = new ArrayList<>();
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int r2 = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > r2) {
                        continue;
                    }
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (!safe(b) || !filter.test(b)) {
                        continue;
                    }
                    saved.add(new Saved(b, b.getBlockData(), to));
                    b.setType(to, false);
                }
            }
        }
        scheduleRevert(saved, revertTicks);
    }

    private void scheduleRevert(List<Saved> saved, long revertTicks) {
        if (saved.isEmpty()) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Saved s : saved) {
                    // Only revert blocks the player has not already changed themselves.
                    if (s.block().getType() == s.to()) {
                        s.block().setBlockData(s.data(), false);
                    }
                }
            }
        }.runTaskLater(plugin, revertTicks);
    }
}
