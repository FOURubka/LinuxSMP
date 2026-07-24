package com.rubka.linuxsmp;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DistroManager {

    public record Fusion(String inputA, String inputB, String result) {
    }

    private final LinuxSMPPlugin plugin;
    private final Map<String, Distro> distros = new LinkedHashMap<>();
    private final Map<Rarity, Integer> rarityWeights = new EnumMap<>(Rarity.class);
    private final List<Fusion> fusions = new ArrayList<>();

    public DistroManager(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        distros.clear();
        rarityWeights.clear();
        fusions.clear();

        File file = new File(plugin.getDataFolder(), "distros.yml");
        if (!file.exists()) {
            plugin.saveResource("distros.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection weights = config.getConfigurationSection("rarity-weights");
        for (Rarity rarity : Rarity.values()) {
            int weight = weights != null ? weights.getInt(rarity.name(), 0) : 0;
            if (weight > 0) {
                rarityWeights.put(rarity, weight);
            }
        }

        ConfigurationSection section = config.getConfigurationSection("distros");
        if (section == null) {
            plugin.getLogger().warning("distros.yml has no 'distros' section.");
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }
            Rarity rarity;
            try {
                rarity = Rarity.valueOf(entry.getString("rarity", "").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Distro '" + id + "': unknown rarity, skipped.");
                continue;
            }

            Map<String, Integer> stats = new LinkedHashMap<>();
            ConfigurationSection statsSection = entry.getConfigurationSection("stats");
            if (statsSection != null) {
                for (String key : statsSection.getKeys(false)) {
                    stats.put(key, statsSection.getInt(key));
                }
            }

            BrickRisk risk = BrickRisk.DEFAULT;
            ConfigurationSection riskSection = entry.getConfigurationSection("brick-risk");
            if (riskSection != null) {
                risk = new BrickRisk(
                        riskSection.getDouble("base-fail-chance", 0.05),
                        "PERMA".equalsIgnoreCase(riskSection.getString("brick-type", "TEMP")),
                        riskSection.getInt("temp-brick-duration-seconds", 180),
                        riskSection.getInt("repair-cost-soft-currency", 500)
                );
            }

            int cmd = entry.getInt("custom-model-data",
                    plugin.getConfig().getInt("custom-model-data." + rarity.name(), 1001));

            distros.put(id, new Distro(
                    id,
                    rarity,
                    entry.getString("display-name", id),
                    Collections.unmodifiableMap(stats),
                    entry.getString("ability", ""),
                    entry.getString("lore", ""),
                    List.copyOf(entry.getStringList("description")),
                    entry.getBoolean("high-maintenance", false),
                    risk,
                    cmd
            ));
        }

        for (Map<?, ?> raw : config.getMapList("fusions")) {
            Object inputs = raw.get("inputs");
            Object result = raw.get("result");
            if (!(inputs instanceof List<?> list) || list.size() != 2 || result == null) {
                plugin.getLogger().warning("Invalid fusion entry skipped: " + raw);
                continue;
            }
            String a = String.valueOf(list.get(0));
            String b = String.valueOf(list.get(1));
            String r = String.valueOf(result);
            if (distros.containsKey(a) && distros.containsKey(b) && distros.containsKey(r)) {
                fusions.add(new Fusion(a, b, r));
            } else {
                plugin.getLogger().warning("Fusion references unknown distro, skipped: " + a + " + " + b + " -> " + r);
            }
        }

        plugin.getLogger().info("Loaded " + distros.size() + " distros, " + fusions.size() + " fusion recipes.");
    }

    public Distro get(String id) {
        return id == null ? null : distros.get(id);
    }

    public Map<String, Distro> all() {
        return Collections.unmodifiableMap(distros);
    }

    public List<Fusion> fusions() {
        return Collections.unmodifiableList(fusions);
    }

    public Map<Rarity, Integer> rarityWeights() {
        return Collections.unmodifiableMap(rarityWeights);
    }

    public Distro roll() {
        return roll(rarityWeights);
    }

    /** Weighted-random roll: pick a rarity by weight, then a random distro of that rarity. */
    public Distro roll(Map<Rarity, Integer> weights) {
        if (distros.isEmpty()) {
            return null;
        }
        Rarity rarity = rollRarity(weights);
        List<Distro> pool = new ArrayList<>();
        for (Distro distro : distros.values()) {
            if (distro.rarity() == rarity) {
                pool.add(distro);
            }
        }
        if (pool.isEmpty()) {
            pool.addAll(distros.values());
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private Rarity rollRarity(Map<Rarity, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            Rarity[] values = Rarity.values();
            return values[ThreadLocalRandom.current().nextInt(values.length)];
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<Rarity, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return Rarity.COMMON;
    }
}
