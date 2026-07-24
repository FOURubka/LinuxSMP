package com.rubka.linuxsmp;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Environmental synergies: an extra contextual buff on top of the base passive,
 * felt only while the active distro is "in its element" (mountains, night, a
 * village, a storm and so on). Picking the right distro for the situation pays off.
 */
public final class Synergy {

    /** One or two effects granted while {@code when} holds for the active distro. */
    public record Bonus(String label, Predicate<Player> when, List<PotionEffect> effects) {
    }

    // Reusable environment tests.
    private static boolean night(Player p) {
        long t = p.getWorld().getTime();
        return t >= 13000 && t <= 23000;
    }

    private static boolean underSky(Player p) {
        return p.getWorld().getHighestBlockYAt(p.getLocation()) <= p.getLocation().getBlockY();
    }

    private static boolean villagerNearby(Player p) {
        return p.getNearbyEntities(12, 12, 12).stream().anyMatch(e -> e instanceof Villager);
    }

    private static boolean playerNearby(Player p) {
        return p.getNearbyEntities(16, 16, 16).stream().anyMatch(e -> e instanceof Player);
    }

    private static int y(Player p) {
        return p.getLocation().getBlockY();
    }

    private static PotionEffect fx(PotionEffectType type, int amplifier) {
        // Short-lived and refreshed every second by SynergyTask, ambient + HUD icon.
        return new PotionEffect(type, 60, amplifier, true, false, true);
    }

    // Synergy per distro id.
    public static final Map<String, Bonus> BY_DISTRO = Map.ofEntries(
            Map.entry("alpine", new Bonus("Mountain air", p -> y(p) >= 120,
                    List.of(fx(PotionEffectType.SPEED, 1), fx(PotionEffectType.JUMP_BOOST, 1)))),
            Map.entry("tails", new Bonus("Cover of night", p -> night(p) && underSky(p),
                    List.of(fx(PotionEffectType.INVISIBILITY, 0)))),
            Map.entry("debian", new Bonus("Among the community", Synergy::villagerNearby,
                    List.of(fx(PotionEffectType.REGENERATION, 0)))),
            Map.entry("ubuntu", new Bonus("Strength in numbers", Synergy::playerNearby,
                    List.of(fx(PotionEffectType.RESISTANCE, 0)))),
            Map.entry("mint", new Bonus("Fresh air", p -> !night(p) && underSky(p),
                    List.of(fx(PotionEffectType.REGENERATION, 0)))),
            Map.entry("popos", new Bonus("Full throttle", Player::isSprinting,
                    List.of(fx(PotionEffectType.SPEED, 1)))),
            Map.entry("fedora", new Bonus("Bleeding edge", p -> p.getWorld().isThundering(),
                    List.of(fx(PotionEffectType.HASTE, 1), fx(PotionEffectType.STRENGTH, 0)))),
            Map.entry("manjaro", new Bonus("Digging the AUR", p -> y(p) < 40,
                    List.of(fx(PotionEffectType.HASTE, 1)))),
            Map.entry("kali", new Bonus("In the shadows",
                    p -> p.getLocation().getBlock().getLightLevel() <= 7,
                    List.of(fx(PotionEffectType.SPEED, 1), fx(PotionEffectType.NIGHT_VISION, 0)))),
            Map.entry("arch", new Bonus("Peak, btw", p -> y(p) >= 150,
                    List.of(fx(PotionEffectType.STRENGTH, 1)))),
            Map.entry("gentoo", new Bonus("Compiling heat",
                    p -> p.getFireTicks() > 0 || p.getWorld().getEnvironment() == World.Environment.NETHER,
                    List.of(fx(PotionEffectType.FIRE_RESISTANCE, 0), fx(PotionEffectType.STRENGTH, 0)))),
            Map.entry("lfs", new Bonus("From the ground up", p -> y(p) <= 20,
                    List.of(fx(PotionEffectType.STRENGTH, 1), fx(PotionEffectType.HASTE, 1)))),
            Map.entry("nixos", new Bonus("Rolled back clean",
                    p -> p.isInWater() || (p.getWorld().hasStorm() && underSky(p)),
                    List.of(fx(PotionEffectType.RESISTANCE, 1)))),
            Map.entry("slackware", new Bonus("Still standing", p -> !night(p) && underSky(p),
                    List.of(fx(PotionEffectType.RESISTANCE, 0))))
    );

    /** Every effect type a synergy can grant - stripped alongside passives on a switch. */
    public static final Set<PotionEffectType> EFFECT_TYPES = BY_DISTRO.values().stream()
            .flatMap(b -> b.effects().stream())
            .map(PotionEffect::getType)
            .collect(Collectors.toUnmodifiableSet());

    private Synergy() {
    }

    public static Bonus of(String distroId) {
        return distroId == null ? null : BY_DISTRO.get(distroId);
    }
}
