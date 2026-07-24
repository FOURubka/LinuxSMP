package com.rubka.linuxsmp;

import java.util.List;
import java.util.Map;

public record Distro(
        String id,
        Rarity rarity,
        String displayName,
        Map<String, Integer> stats,
        String ability,
        String lore,
        List<String> description,
        boolean highMaintenance,
        BrickRisk brickRisk,
        int customModelData
) {
    public int stat(String key) {
        return stats.getOrDefault(key, 0);
    }

    public int learningCurve() {
        return stat("learning-curve");
    }
}
