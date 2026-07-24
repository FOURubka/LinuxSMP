package com.rubka.linuxsmp;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Particle / colour helpers so effects stay consistent across the plugin. */
public final class Fx {

    private Fx() {
    }

    public static Color rarityColor(Rarity rarity) {
        return switch (rarity) {
            case COMMON -> Color.fromRGB(0xAAAAAA);
            case RARE -> Color.fromRGB(0x3FC7EB);
            case EPIC -> Color.fromRGB(0xB14BFF);
            case LEGENDARY -> Color.fromRGB(0xFFB000);
            case MYTHIC -> Color.fromRGB(0xFF3355);
        };
    }

    /** A ring of dust of the given colour around the player's waist. */
    public static void dustRing(Player player, Color color, double radius, int points) {
        Location base = player.getLocation().add(0, 1.0, 0);
        World world = player.getWorld();
        Particle.DustOptions opt = new Particle.DustOptions(color, 1.2f);
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            Location p = base.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, opt);
        }
    }

    /** A vertical helix of dust rising around the player, phase animates it. */
    public static void helix(Player player, Color color, double phase) {
        World world = player.getWorld();
        Location base = player.getLocation();
        Particle.DustOptions opt = new Particle.DustOptions(color, 1.4f);
        for (int arm = 0; arm < 2; arm++) {
            double a = phase + arm * Math.PI;
            for (double y = 0; y <= 2.2; y += 0.35) {
                double r = 0.9;
                Location p = base.clone().add(Math.cos(a + y * 2) * r, y, Math.sin(a + y * 2) * r);
                world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, opt);
            }
        }
    }

    public static void burst(Player player, Rarity rarity) {
        World world = player.getWorld();
        Location loc = player.getLocation().add(0, 1.0, 0);
        Particle.DustOptions opt = new Particle.DustOptions(rarityColor(rarity), 1.6f);
        world.spawnParticle(Particle.DUST, loc, 60, 0.6, 0.8, 0.6, 0, opt);
        world.spawnParticle(Particle.END_ROD, loc, 30, 0.3, 0.5, 0.3, 0.05);
        if (rarity == Rarity.LEGENDARY || rarity == Rarity.MYTHIC) {
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.5, 0.7, 0.5, 0.3);
            world.spawnParticle(Particle.FIREWORK, loc, 20, 0.4, 0.6, 0.4, 0.08);
        }
    }

    public static void spawn(Player player, Particle particle, int count, double spread, double speed) {
        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1.0, 0),
                count, spread, spread, spread, speed);
    }
}
