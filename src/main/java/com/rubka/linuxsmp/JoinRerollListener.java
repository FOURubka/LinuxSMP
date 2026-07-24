package com.rubka.linuxsmp;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Automatic distro reroll on login, fully driven by the `join-reroll` config section. */
public class JoinRerollListener implements Listener {

    private final LinuxSMPPlugin plugin;

    public JoinRerollListener(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("join-reroll.enabled", false)) {
            return;
        }
        Player player = event.getPlayer();
        long delay = Math.max(1, config.getLong("join-reroll.delay-ticks", 40));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                tryReroll(player);
            }
        }, delay);
    }

    private void tryReroll(Player player) {
        FileConfiguration config = plugin.getConfig();
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        boolean announce = config.getBoolean("join-reroll.announce", true);

        if ("FIRST_JOIN_ONLY".equalsIgnoreCase(config.getString("join-reroll.mode", "EVERY_JOIN"))
                && data.joinRerolled) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownMs = config.getLong("join-reroll.cooldown-seconds", 0) * 1000L;
        if (cooldownMs > 0 && now - data.lastJoinReroll < cooldownMs) {
            return;
        }
        long cost = config.getLong("join-reroll.cost-packages", 0);
        if (cost > 0 && data.packages < cost) {
            if (announce) {
                Text.msg(player, "&7Join reroll skipped - it costs &e" + cost + " Packages&7.");
            }
            return;
        }

        Distro result = roll(config);
        if (result == null) {
            return;
        }
        data.packages -= cost;
        data.lastJoinReroll = now;
        data.joinRerolled = true;

        if (!config.getBoolean("join-reroll.keep-unlocked", true)) {
            data.unlocked.clear();
            data.active = null;
            data.mixedBuild = false;
        }

        boolean duplicate = data.unlocked.contains(result.id());
        long refund = 0;
        if (duplicate) {
            if (config.getBoolean("join-reroll.duplicate-refund", true)) {
                refund = config.getLong("roll.duplicate-refund." + result.rarity().name(), 25);
                data.packages += refund;
            }
        } else {
            data.unlocked.add(result.id());
            data.permaBricked.remove(result.id());
        }
        if (!duplicate && (config.getBoolean("join-reroll.set-active", true) || data.active == null)) {
            data.active = result.id();
            data.lastMaintenance = now;
            data.mixedBuild = false;
            PassiveTask.onSwitch(plugin, player, data);
        }

        if (config.getBoolean("join-reroll.animation", true)) {
            new RollAnimation(plugin, player, result, duplicate, refund).start();
        } else if (announce) {
            Text.msg(player, "&7Boot roll: " + result.displayName()
                    + " &8[" + result.rarity().color + result.rarity() + "&8]"
                    + (duplicate ? " &7(duplicate, +&e" + refund + " Packages&7)" : ""));
        }
    }

    /** Weighted roll over the pool allowed by `allowed-rarities` / `excluded-distros`. */
    private Distro roll(FileConfiguration config) {
        Set<Rarity> allowed = EnumSet.noneOf(Rarity.class);
        for (String name : config.getStringList("join-reroll.allowed-rarities")) {
            try {
                allowed.add(Rarity.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("join-reroll: unknown rarity '" + name + "' ignored.");
            }
        }
        Set<String> excluded = new HashSet<>(config.getStringList("join-reroll.excluded-distros"));
        List<Distro> pool = plugin.getDistroManager().all().values().stream()
                .filter(d -> allowed.isEmpty() || allowed.contains(d.rarity()))
                .filter(d -> !excluded.contains(d.id()))
                .toList();
        if (pool.isEmpty()) {
            return null;
        }

        Map<Rarity, Integer> weights = new EnumMap<>(Rarity.class);
        if (config.getBoolean("join-reroll.premium-weights", false)) {
            var section = config.getConfigurationSection("premium-rarity-weights");
            if (section != null) {
                for (Rarity rarity : Rarity.values()) {
                    int weight = section.getInt(rarity.name(), 0);
                    if (weight > 0) {
                        weights.put(rarity, weight);
                    }
                }
            }
        } else {
            weights.putAll(plugin.getDistroManager().rarityWeights());
        }
        weights.keySet().retainAll(pool.stream().map(Distro::rarity).collect(java.util.stream.Collectors.toSet()));

        Rarity rarity = rollRarity(weights);
        List<Distro> ofRarity = new ArrayList<>();
        for (Distro d : pool) {
            if (rarity == null || d.rarity() == rarity) {
                ofRarity.add(d);
            }
        }
        if (ofRarity.isEmpty()) {
            ofRarity = pool;
        }
        return ofRarity.get(ThreadLocalRandom.current().nextInt(ofRarity.size()));
    }

    private Rarity rollRarity(Map<Rarity, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<Rarity, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return null;
    }
}
