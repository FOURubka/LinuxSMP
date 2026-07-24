package com.rubka.linuxsmp;

public record BrickRisk(
        double baseFailChance,
        boolean permaBrick,
        int tempBrickDurationSeconds,
        int repairCostSoft
) {
    public static final BrickRisk DEFAULT = new BrickRisk(0.05, false, 180, 500);
}
