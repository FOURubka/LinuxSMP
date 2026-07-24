package com.rubka.linuxsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The "distro install" reroll: the player levitates while a small floating
 * island builds under them and candidate distro tokens orbit and spiral in,
 * until the rolled distro flies into them and is revealed.
 */
public class RollAnimation {

    private static final int ORBIT_COUNT = 5;
    private static final int REVEAL_TICK = 70;
    private static final int END_TICK = 90;

    private final LinuxSMPPlugin plugin;
    private final Player player;
    private final Distro result;
    private final boolean duplicate;
    private final long refund;

    private final List<ItemDisplay> orbiters = new ArrayList<>();
    private final List<BlockDisplay> mound = new ArrayList<>();
    private final Location center;

    public RollAnimation(LinuxSMPPlugin plugin, Player player, Distro result, boolean duplicate, long refund) {
        this.plugin = plugin;
        this.player = player;
        this.result = result;
        this.duplicate = duplicate;
        this.refund = refund;
        this.center = player.getLocation().clone();
    }

    public void start() {
        World world = player.getWorld();
        List<Distro> pool = new ArrayList<>(plugin.getDistroManager().all().values());

        // Orbiting candidate tokens (billboarded so the icons face the player).
        for (int i = 0; i < ORBIT_COUNT; i++) {
            Distro shown = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            ItemDisplay disp = world.spawn(center.clone().add(0, 1.2, 0), ItemDisplay.class, d -> {
                d.setItemStack(plugin.getItemFactory().token(shown));
                d.setBillboard(Display.Billboard.CENTER);
                d.setBrightness(new Display.Brightness(15, 15));
            });
            orbiters.add(disp);
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 1, false, false, false));
        player.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.8f);

        Material moundMat = moundMaterial(result.rarity());

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cleanup();
                    cancel();
                    return;
                }

                // Build the island in stages under the player.
                if (t == 2) placeLayer(moundMat, -1, 1);
                if (t == 8) placeLayer(moundMat, 0, 1);
                if (t == 14) placeLayer(Material.MOSS_BLOCK, 1, 0);

                // Orbit + spiral inward.
                if (t < REVEAL_TICK) {
                    double progress = t / (double) REVEAL_TICK;
                    double radius = 2.6 - progress * 2.0;
                    double phase = t * 0.35;
                    Location around = player.getLocation().add(0, 1.1, 0);
                    for (int i = 0; i < orbiters.size(); i++) {
                        double a = phase + 2 * Math.PI * i / orbiters.size();
                        Location p = around.clone().add(Math.cos(a) * radius, Math.sin(t * 0.2 + i) * 0.3, Math.sin(a) * radius);
                        orbiters.get(i).teleport(p);
                    }
                    Fx.helix(player, Fx.rarityColor(result.rarity()), phase);
                    if (t % 6 == 0) {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, (float) (1.0 + progress));
                    }
                    t++;
                    return;
                }

                if (t == REVEAL_TICK) {
                    reveal();
                    t++;
                    return;
                }

                if (t >= END_TICK) {
                    cleanup();
                    cancel();
                    return;
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void reveal() {
        // Tokens vanish, the winner bursts out.
        for (ItemDisplay d : orbiters) {
            d.getWorld().spawnParticle(Particle.END_ROD, d.getLocation(), 6, 0.1, 0.1, 0.1, 0.02);
            d.remove();
        }
        orbiters.clear();

        Fx.burst(player, result.rarity());
        boolean topTier = result.rarity() == Rarity.LEGENDARY || result.rarity() == Rarity.MYTHIC;
        player.playSound(player.getLocation(),
                topTier ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0, false, false, false));

        Component top = Text.c(result.displayName());
        Component sub = Text.mm(rarityGradient(result.rarity()));
        player.showTitle(Title.title(top, sub,
                Title.Times.times(Duration.ofMillis(150), Duration.ofSeconds(2), Duration.ofMillis(600))));

        Text.msg(player, "&7You rolled " + result.displayName() + " &8[" + result.rarity().color + result.rarity() + "&8]");
        Text.msg(player, "&8\"" + result.lore() + "\"");
        if (duplicate) {
            Text.msg(player, "&7Duplicate - refunded &e" + refund + " Packages&7.");
        } else {
            Text.msg(player, "&aUnlocked! &7Set active with &f/distroswap " + result.id());
        }
    }

    private void placeLayer(Material material, double dy, int radius) {
        World world = player.getWorld();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Location loc = center.clone().add(dx, dy, dz);
                BlockDisplay bd = world.spawn(loc, BlockDisplay.class,
                        d -> d.setBlock(material.createBlockData()));
                mound.add(bd);
            }
        }
        world.spawnParticle(Particle.CLOUD, center.clone().add(0, dy + 0.5, 0), 20, radius, 0.2, radius, 0.02);
        player.playSound(center, Sound.BLOCK_ROOTED_DIRT_PLACE, 1.0f, 0.8f);
    }

    private void cleanup() {
        for (ItemDisplay d : orbiters) {
            d.remove();
        }
        orbiters.clear();
        for (BlockDisplay d : mound) {
            d.getWorld().spawnParticle(Particle.BLOCK, d.getLocation().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0,
                    Material.MOSS_BLOCK.createBlockData());
            d.remove();
        }
        mound.clear();
    }

    private Material moundMaterial(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Material.LIGHT_GRAY_CONCRETE;
            case RARE -> Material.CYAN_CONCRETE;
            case EPIC -> Material.PURPLE_CONCRETE;
            case LEGENDARY -> Material.ORANGE_CONCRETE;
            case MYTHIC -> Material.RED_CONCRETE;
        };
    }

    private String rarityGradient(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "<gradient:#c0c0c0:#8a8a8a><bold>COMMON</bold></gradient>";
            case RARE -> "<gradient:#3fc7eb:#2f80ed><bold>RARE</bold></gradient>";
            case EPIC -> "<gradient:#b14bff:#7f5af0><bold>EPIC</bold></gradient>";
            case LEGENDARY -> "<gradient:#ffd200:#ff8a00><bold>LEGENDARY</bold></gradient>";
            case MYTHIC -> "<gradient:#ff3355:#ff00aa:#ffd200><bold>MYTHIC</bold></gradient>";
        };
    }
}
