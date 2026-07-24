package com.rubka.linuxsmp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/** A floating "kernel panic" screen with sparks and smoke shown when a distro bricks. */
public class BrickAnimation {

    private static final int DURATION = 70;

    private final LinuxSMPPlugin plugin;
    private final Player player;
    private final Distro distro;

    public BrickAnimation(LinuxSMPPlugin plugin, Player player, Distro distro) {
        this.plugin = plugin;
        this.player = player;
        this.distro = distro;
    }

    public void start() {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        // 2.5 blocks in front of the player, facing them.
        Location screenLoc = eye.clone().add(eye.getDirection().normalize().multiply(2.5));
        float yaw = (float) (eye.getYaw() + 180.0);

        BlockDisplay panel = world.spawn(screenLoc, BlockDisplay.class, d -> {
            d.setBlock(Material.BLACK_CONCRETE.createBlockData());
            d.setBrightness(new Display.Brightness(15, 15));
            d.setRotation(yaw, 0f);
            // Wide flat panel, centred on the anchor.
            d.setTransformation(new Transformation(
                    new Vector3f(-1.6f, -1.0f, 0.02f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(3.2f, 2.0f, 0.05f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
        });

        TextDisplay text = world.spawn(screenLoc, TextDisplay.class, d -> {
            d.text(panicText());
            d.setBillboard(Display.Billboard.FIXED);
            d.setRotation(yaw, 0f);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setSeeThrough(false);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setLineWidth(220);
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0.55f, 0.04f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(0.6f, 0.6f, 0.6f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)));
        });

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);

        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (!player.isOnline() || t >= DURATION) {
                    panel.remove();
                    text.remove();
                    return;
                }
                // Flicker the screen off briefly, like a dying monitor.
                boolean on = !(t > 12 && t % 9 == 0);
                text.setViewRange(on ? 1.0f : 0.0f);

                world.spawnParticle(Particle.ELECTRIC_SPARK, screenLoc, 6, 1.4, 0.9, 0.1, 0.05);
                if (t % 2 == 0) {
                    world.spawnParticle(Particle.SMOKE, screenLoc.clone().add(0, -0.8, 0), 4, 1.3, 0.3, 0.1, 0.02);
                    world.spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 3, 0.4, 0.6, 0.4, 0.01);
                }
                if (t % 6 == 0) {
                    world.spawnParticle(Particle.LAVA, screenLoc, 1, 1.2, 0.6, 0.05, 0);
                    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.6f, 0.6f);
                }
                if (t == 4) {
                    player.playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 0.7f, 0.4f);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Component panicText() {
        TextColor red = TextColor.color(0xFF3355);
        TextColor gray = TextColor.color(0xB0B0B0);
        String name = distro.displayName().replaceAll("&[0-9a-fk-or]", "");
        return Component.text("*** KERNEL PANIC ***", red)
                .append(Component.newline())
                .append(Component.text("not syncing: Attempted to kill init!", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("VFS: Unable to mount root fs on " + name, gray))
                .append(Component.newline())
                .append(Component.text("CPU: 0 PID: 1 Comm: systemd Tainted", gray))
                .append(Component.newline())
                .append(Component.text("---[ end Kernel panic ]---", red));
    }
}
