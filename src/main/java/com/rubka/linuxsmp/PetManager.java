package com.rubka.linuxsmp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** A small floating logo of the active distro that orbits each player. */
public class PetManager extends BukkitRunnable {

    private final LinuxSMPPlugin plugin;
    private final Map<UUID, ItemDisplay> pets = new HashMap<>();
    private final Map<UUID, String> petDistro = new HashMap<>();
    private double phase;

    public PetManager(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        phase += 0.18;

        // Drop pets whose owner has logged off.
        Iterator<Map.Entry<UUID, ItemDisplay>> it = pets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ItemDisplay> e = it.next();
            if (Bukkit.getPlayer(e.getKey()) == null) {
                e.getValue().remove();
                petDistro.remove(e.getKey());
                it.remove();
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            PlayerDistroData data = plugin.getPlayerDataManager().get(player);
            Distro distro = plugin.getDistroManager().get(data.active);
            boolean usable = distro != null && !data.isTempBricked(distro.id())
                    && !data.permaBricked.contains(distro.id());

            if (!usable) {
                remove(id);
                continue;
            }

            ItemDisplay pet = pets.get(id);
            if (pet == null || !pet.isValid()) {
                pet = spawn(player, distro);
                pets.put(id, pet);
                petDistro.put(id, distro.id());
            } else if (!distro.id().equals(petDistro.get(id))) {
                pet.setItemStack(plugin.getItemFactory().token(distro));
                petDistro.put(id, distro.id());
            }
            pet.teleport(orbit(player));
        }
    }

    public void remove(UUID id) {
        ItemDisplay pet = pets.remove(id);
        if (pet != null) {
            pet.remove();
        }
        petDistro.remove(id);
    }

    public void stopAll() {
        pets.values().forEach(ItemDisplay::remove);
        pets.clear();
        petDistro.clear();
        cancel();
    }

    private ItemDisplay spawn(Player player, Distro distro) {
        return player.getWorld().spawn(orbit(player), ItemDisplay.class, d -> {
            d.setItemStack(plugin.getItemFactory().token(distro));
            d.setBillboard(Display.Billboard.CENTER);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTeleportDuration(3);
            d.setPersistent(false);
            d.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(0.45f, 0.45f, 0.45f), new AxisAngle4f(0f, 0f, 0f, 1f)));
        });
    }

    private Location orbit(Player player) {
        Location base = player.getLocation();
        double r = 0.9;
        double x = Math.cos(phase) * r;
        double z = Math.sin(phase) * r;
        double y = 1.9 + Math.sin(phase * 2) * 0.15;
        return base.clone().add(x, y, z);
    }
}
