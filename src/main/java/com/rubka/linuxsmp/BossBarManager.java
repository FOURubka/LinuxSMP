package com.rubka.linuxsmp;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Transient countdown boss bars (ability cooldown, compiling, brick timer, ...). */
public class BossBarManager {

    private static final class Timer {
        final BossBar bar;
        final long end;
        final long total;
        final String label;

        Timer(BossBar bar, long end, long total, String label) {
            this.bar = bar;
            this.end = end;
            this.total = total;
            this.label = label;
        }
    }

    private final LinuxSMPPlugin plugin;
    private final Map<UUID, Map<String, Timer>> bars = new HashMap<>();

    public BossBarManager(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 5L, 2L);
    }

    /** Show/replace a named countdown bar. Label is MiniMessage (no trailing time). */
    public void countdown(Player player, String key, String label, BossBar.Color color, long totalMs) {
        Map<String, Timer> m = bars.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        Timer existing = m.remove(key);
        if (existing != null) {
            player.hideBossBar(existing.bar);
        }
        BossBar bar = BossBar.bossBar(Text.mm(label), 1f, color, BossBar.Overlay.PROGRESS);
        player.showBossBar(bar);
        m.put(key, new Timer(bar, System.currentTimeMillis() + totalMs, totalMs, label));
    }

    public void clear(Player player, String key) {
        Map<String, Timer> m = bars.get(player.getUniqueId());
        if (m == null) {
            return;
        }
        Timer t = m.remove(key);
        if (t != null) {
            player.hideBossBar(t.bar);
        }
    }

    public void clearAll(Player player) {
        Map<String, Timer> m = bars.remove(player.getUniqueId());
        if (m != null) {
            m.values().forEach(t -> player.hideBossBar(t.bar));
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Map<String, Timer>> e : bars.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            Iterator<Map.Entry<String, Timer>> it = e.getValue().entrySet().iterator();
            while (it.hasNext()) {
                Timer t = it.next().getValue();
                long rem = t.end - now;
                if (p == null || !p.isOnline() || rem <= 0) {
                    if (p != null) {
                        p.hideBossBar(t.bar);
                    }
                    it.remove();
                    continue;
                }
                t.bar.progress(Math.max(0f, Math.min(1f, (float) rem / t.total)));
                t.bar.name(Text.mm(t.label + " <white>" + (rem / 1000 + 1) + "s</white>"));
            }
        }
    }
}
