package com.rubka.linuxsmp;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatTracker implements Listener {

    private final Map<UUID, Long> lastCombat = new HashMap<>();

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        long now = System.currentTimeMillis();
        if (event.getEntity() instanceof Player victim) {
            lastCombat.put(victim.getUniqueId(), now);
        }
        if (event.getDamager() instanceof Player damager) {
            lastCombat.put(damager.getUniqueId(), now);
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            lastCombat.put(shooter.getUniqueId(), now);
        }
    }

    public boolean inCombat(Player player, int windowSeconds) {
        Long last = lastCombat.get(player.getUniqueId());
        return last != null && System.currentTimeMillis() - last < windowSeconds * 1000L;
    }
}
