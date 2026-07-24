package com.rubka.linuxsmp;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Collection set bonuses: unlock every member of a set for a permanent passive buff. */
public final class Sets {

    public record Bonus(String name, String description, List<String> members,
                        PotionEffectType effect, int amplifier) {
        boolean owns(PlayerDistroData data) {
            return data.unlocked.containsAll(members);
        }
    }

    public static final List<Bonus> ALL = List.of(
            new Bonus("Common Collector", "+1 heart",
                    List.of("ubuntu", "mint", "popos"), PotionEffectType.HEALTH_BOOST, 0),
            new Bonus("Debian Family", "Regeneration",
                    List.of("debian", "ubuntu", "mint", "kali"), PotionEffectType.REGENERATION, 0),
            new Bonus("Arch Family", "Extra strength",
                    List.of("arch", "manjaro"), PotionEffectType.STRENGTH, 0),
            new Bonus("Source Masters", "Haste",
                    List.of("gentoo", "lfs", "slackware"), PotionEffectType.HASTE, 0),
            new Bonus("Full Distrohopper", "Resistance",
                    List.of("ubuntu", "mint", "popos", "debian", "fedora", "alpine", "manjaro",
                            "kali", "arch", "gentoo", "lfs", "nixos", "tails", "slackware"),
                    PotionEffectType.RESISTANCE, 1)
    );

    private Sets() {
    }

    public static List<Bonus> active(PlayerDistroData data) {
        List<Bonus> result = new ArrayList<>();
        for (Bonus b : ALL) {
            if (b.owns(data)) {
                result.add(b);
            }
        }
        return result;
    }

    /** Refresh every owned set's passive effect on the player. */
    public static void apply(Player player, PlayerDistroData data) {
        for (Bonus b : ALL) {
            if (b.owns(data)) {
                player.addPotionEffect(new PotionEffect(b.effect, 140, b.amplifier, true, false, true));
            }
        }
    }

    /** How many members of a set the player owns (for GUI progress). */
    public static int owned(Bonus b, PlayerDistroData data) {
        int n = 0;
        Set<String> unlocked = data.unlocked;
        for (String m : b.members) {
            if (unlocked.contains(m)) {
                n++;
            }
        }
        return n;
    }
}
