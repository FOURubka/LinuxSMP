package com.rubka.linuxsmp;

public enum Rarity {
    COMMON("&7", 1),
    RARE("&b", 2),
    EPIC("&d", 4),
    LEGENDARY("&6", 7),
    MYTHIC("&4", 12);

    public final String color;
    public final int points;

    Rarity(String color, int points) {
        this.color = color;
        this.points = points;
    }
}
