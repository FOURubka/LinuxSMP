package com.rubka.linuxsmp;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager implements Listener {

    private final LinuxSMPPlugin plugin;
    private final Map<UUID, PlayerDistroData> cache = new HashMap<>();
    private final File folder;

    public PlayerDataManager(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerDistroData get(Player player) {
        return get(player.getUniqueId());
    }

    public PlayerDistroData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    private PlayerDistroData load(UUID uuid) {
        PlayerDistroData data = new PlayerDistroData(uuid);
        File file = new File(folder, uuid + ".yml");
        if (!file.exists()) {
            return data;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.unlocked.addAll(config.getStringList("unlocked"));
        data.permaBricked.addAll(config.getStringList("perma-bricked"));
        var brickSection = config.getConfigurationSection("temp-bricked");
        if (brickSection != null) {
            for (String id : brickSection.getKeys(false)) {
                data.tempBrickedUntil.put(id, brickSection.getLong(id));
            }
        }
        data.active = config.getString("active");
        data.mixedBuild = config.getBoolean("mixed-build");
        data.lastMaintenance = config.getLong("last-maintenance");
        data.skillLevel = config.getInt("skill-level");
        data.joinRerolled = config.getBoolean("join-rerolled");
        data.lastJoinReroll = config.getLong("last-join-reroll");
        data.packages = config.getLong("packages");
        data.repoTokens = config.getLong("repo-tokens");
        data.dotfiles = config.getLong("dotfiles");
        return data;
    }

    public void save(UUID uuid) {
        PlayerDistroData data = cache.get(uuid);
        if (data == null) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("unlocked", data.unlocked.stream().toList());
        config.set("perma-bricked", data.permaBricked.stream().toList());
        for (Map.Entry<String, Long> entry : data.tempBrickedUntil.entrySet()) {
            config.set("temp-bricked." + entry.getKey(), entry.getValue());
        }
        config.set("active", data.active);
        config.set("mixed-build", data.mixedBuild);
        config.set("last-maintenance", data.lastMaintenance);
        config.set("skill-level", data.skillLevel);
        config.set("join-rerolled", data.joinRerolled);
        config.set("last-join-reroll", data.lastJoinReroll);
        config.set("packages", data.packages);
        config.set("repo-tokens", data.repoTokens);
        config.set("dotfiles", data.dotfiles);
        try {
            config.save(new File(folder, uuid + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
    }

    /** Unlocked-distro lists for every known player (saved files + live cache). */
    public Map<UUID, List<String>> collectionSnapshot() {
        Map<UUID, List<String>> out = new HashMap<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    out.put(uuid, config.getStringList("unlocked"));
                } catch (IllegalArgumentException ignored) {
                    // Not a player-data file.
                }
            }
        }
        // Online players may have fresher, unsaved data.
        for (Map.Entry<UUID, PlayerDistroData> entry : cache.entrySet()) {
            out.put(entry.getKey(), new ArrayList<>(entry.getValue().unlocked));
        }
        return out;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        save(uuid);
        cache.remove(uuid);
    }
}
