package com.rubka.linuxsmp;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class AbilityListener implements Listener {

    /** Gentoo emerge window: helpless, and one player hit kills the build. */
    private static final int COMPILE_SECONDS = 8;
    /** How long the freshly built -march=native binaries stay hot. */
    private static final int NATIVE_SECONDS = 20;

    private final LinuxSMPPlugin plugin;
    // Debian "Stable Fortress" stance: while active the player reflects and shrugs off hits.
    private final Map<UUID, Long> fortressUntil = new HashMap<>();
    // Tails "Ghost Protocol": while vanished the first strike hits double and blows cover.
    private final Map<UUID, Long> vanishUntil = new HashMap<>();
    // Gentoo "Compile Everything": mid-emerge you cannot fight and any hit aborts the build.
    private final Map<UUID, Long> compilingUntil = new HashMap<>();
    // Gentoo post-emerge: every melee hit detonates while the native binaries last.
    private final Map<UUID, Long> nativeUntil = new HashMap<>();
    // The splash damage is itself a damage event; without this the blast chains into itself.
    private boolean detonating = false;

    public AbilityListener(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    /** While a Debian player holds the Fortress stance, incoming hits are halved and bounced back. */
    @EventHandler(ignoreCancelled = true)
    public void onFortressHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Long until = fortressUntil.get(victim.getUniqueId());
        if (until == null || System.currentTimeMillis() > until) {
            return;
        }
        event.setDamage(event.getDamage() * 0.25);
        if (event.getDamager() instanceof LivingEntity attacker && !attacker.equals(victim)) {
            attacker.damage(3, victim);
            pushAway(victim, attacker, 1.4);
            attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.1);
        }
    }

    /** A vanished Tails player's first strike deals double damage and reveals them. */
    @EventHandler(ignoreCancelled = true)
    public void onVanishStrike(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        Long until = vanishUntil.get(attacker.getUniqueId());
        if (until == null || System.currentTimeMillis() > until) {
            return;
        }
        event.setDamage(event.getDamage() * 2.0);
        reveal(attacker);
        attacker.getWorld().spawnParticle(Particle.CRIT,
                event.getEntity().getLocation().add(0, 1, 0), 25, 0.4, 0.5, 0.4, 0.2);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
        attacker.sendActionBar(Text.c("&5:: backstab from the shadows - cover blown"));
    }

    /**
     * The two halves of Gentoo's emerge gamble. Mid-compile you have no working toolchain,
     * so nothing you swing lands - and a single hit from another player kills the build.
     * Survive it and every hit comes out of an optimised binary, detonating on impact.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCompileHit(EntityDamageByEntityEvent event) {
        long now = System.currentTimeMillis();
        if (event.getDamager() instanceof Player attacker) {
            Long compiling = compilingUntil.get(attacker.getUniqueId());
            if (compiling != null && now < compiling) {
                event.setCancelled(true);
                attacker.sendActionBar(Text.c("&8:: no binaries yet - emerge still running"));
                return;
            }
            Long optimised = nativeUntil.get(attacker.getUniqueId());
            if (!detonating && optimised != null && now < optimised
                    && event.getEntity() instanceof LivingEntity target && !target.equals(attacker)) {
                nativeDetonation(attacker, target);
            }
        }
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player breaker
                && !breaker.equals(victim)) {
            Long until = compilingUntil.get(victim.getUniqueId());
            if (until != null && now < until) {
                abortCompile(victim, breaker);
            }
        }
    }

    private boolean isCompiling(Player player) {
        Long until = compilingUntil.get(player.getUniqueId());
        return until != null && System.currentTimeMillis() < until;
    }

    /**
     * Mid-emerge the toolchain owns the machine: you are pinned to the spot. Looking around
     * stays free - watching someone walk up to you is the point - but position does not.
     * Potion slowness alone never held anyone still: you can still sprint-jump out of it.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCompileMove(PlayerMoveEvent event) {
        if (compilingUntil.isEmpty() || !isCompiling(event.getPlayer())) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }
        Location pinned = from.clone();
        pinned.setYaw(to.getYaw());
        pinned.setPitch(to.getPitch());
        event.setTo(pinned);
    }

    /** No pearls, no potions, no gapples, no riptide - a compile owns every core. */
    @EventHandler(ignoreCancelled = true)
    public void onCompileInteract(PlayerInteractEvent event) {
        if (isCompiling(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Text.c("&8:: make -j$(nproc) is holding every core"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCompileBreak(BlockBreakEvent event) {
        if (isCompiling(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCompilePlace(BlockPlaceEvent event) {
        if (isCompiling(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /** A boat would carry you out of the root, since vehicles bypass PlayerMoveEvent. */
    @EventHandler(ignoreCancelled = true)
    public void onCompileVehicle(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player && isCompiling(player)) {
            event.setCancelled(true);
        }
    }

    /** Right-clicking a distro token installs (unlocks) the distro. */
    @EventHandler
    public void onTokenUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) {
            return;
        }
        ItemStack item = event.getItem();
        String distroId = plugin.getItemFactory().tokenDistroId(item);
        if (distroId == null || isCompiling(event.getPlayer())) {
            return;   // this handler is not ignoreCancelled, so the compile lock is checked here too
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Distro distro = plugin.getDistroManager().get(distroId);
        if (distro == null) {
            Text.msg(player, "&cUnknown distro on this token.");
            return;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        if (data.unlocked.contains(distroId)) {
            Text.msg(player, "&7You already have " + distro.displayName() + "&7 installed.");
            return;
        }
        item.setAmount(item.getAmount() - 1);
        data.unlocked.add(distroId);
        data.permaBricked.remove(distroId);
        if (data.active == null) {
            data.active = distroId;
            data.lastMaintenance = System.currentTimeMillis();
            PassiveTask.onSwitch(plugin, player, data);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        Fx.spawn(player, Particle.HAPPY_VILLAGER, 15, 0.5, 0.1);
        Text.msg(player, "&aInstalled " + distro.displayName() + "&a!");
    }

    /** Sneak + swap-hands (Shift+F) activates the active distro's ability. */
    @EventHandler
    public void onAbilityTrigger(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        if (plugin.getDistroManager().get(data.active) == null) {
            return;
        }
        event.setCancelled(true);
        activate(player);
    }

    /**
     * Boots the given distro (if unlocked and not bricked) and fires its ability.
     * Used by the per-distro commands like /nixos.
     */
    public void activateAs(Player player, Distro distro) {
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        if (!data.unlocked.contains(distro.id())) {
            Text.msg(player, "&c" + distro.displayName() + " &cis not installed - roll or fuse it first.");
            return;
        }
        if (data.isTempBricked(distro.id()) || data.permaBricked.contains(distro.id())) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f);
            Text.msg(player, "&c" + distro.displayName() + " &cis bricked.");
            return;
        }
        if (!distro.id().equals(data.active)) {
            data.active = distro.id();
            data.lastMaintenance = System.currentTimeMillis();
            data.mixedBuild = false;
            PassiveTask.onSwitch(plugin, player, data);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            Fx.spawn(player, Particle.HAPPY_VILLAGER, 10, 0.4, 0.1);
            Text.msg(player, "&aBooted " + distro.displayName() + "&a.");
        }
        activate(player);
    }

    /** Fires the active distro's ability, running cooldown and skill checks. */
    public void activate(Player player) {
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        Distro distro = plugin.getDistroManager().get(data.active);
        if (distro == null) {
            Text.msg(player, "&cNo active distro to use.");
            return;
        }

        // Rollback (NixOS) manages its own timing, not the shared cooldown.
        if (distro.ability().equals("rollback")) {
            if (!plugin.getRewind().isRewinding(player)) {
                plugin.getRewind().rewind(player, data);
            }
            return;
        }

        long now = System.currentTimeMillis();
        if (now < data.abilityCooldownUntil) {
            player.sendActionBar(Text.c("&7Ability on cooldown: &f"
                    + (data.abilityCooldownUntil - now) / 1000 + "s"));
            return;
        }
        long cooldownMs = plugin.getConfig().getInt("ability.cooldown-seconds", 30) * 1000L;
        data.abilityCooldownUntil = now + cooldownMs;

        SkillCheckService checks = plugin.getSkillCheckService();
        if (!checks.check(data, distro, SkillCheckService.CheckType.ABILITY)) {
            checks.brick(player, data, distro);
            return;
        }
        applyAbility(player, data, distro);
        plugin.getBossBars().countdown(player, "ability",
                "<red>Ability cooldown</red>", BossBar.Color.RED, cooldownMs);
    }

    private void applyAbility(Player player, PlayerDistroData data, Distro distro) {
        Location loc = player.getLocation();
        switch (distro.ability()) {

            // Arch - "I use Arch, btw": brutal offence if maintained, real self-damage if not.
            case "arch_btw" -> {
                boolean maintained = System.currentTimeMillis() - data.lastMaintenance < 60_000;
                if (maintained) {
                    effect(player, PotionEffectType.STRENGTH, 15, 2);
                    effect(player, PotionEffectType.SPEED, 15, 1);
                    effect(player, PotionEffectType.HASTE, 15, 2);
                    // Shockwave: a visible ring of glass races outward, everything close is blasted.
                    int hit = damageNearby(player, 6, 8, 1.2);
                    DisplayFx.ringWave(plugin, loc, Material.LIGHT_BLUE_STAINED_GLASS, 6, 20, 12);
                    explosionFx(loc, Particle.SOUL_FIRE_FLAME);
                    player.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.6f);
                    announce(player, "&b:: I use Arch, btw &7(+strength, shockwave x" + hit + ")",
                            Sound.ITEM_TRIDENT_THUNDER, 1.4f);
                } else {
                    player.damage(6, player);
                    effect(player, PotionEffectType.WEAKNESS, 8, 1);
                    effect(player, PotionEffectType.SLOWNESS, 8, 1);
                    announce(player, "&cUnmaintained Arch broke on you - run /pacman-syu!", Sound.BLOCK_ANVIL_LAND, 0.7f);
                    Fx.spawn(player, Particle.SMOKE, 30, 0.4, 0.02);
                }
            }

            // Gentoo - Compile Everything: an all-or-nothing emerge. For 8s you are rooted,
            // your swings do nothing and everyone nearby watches your progress bar. One hit
            // from another player and the build dies with you still holding the bag; ride it
            // out and you walk away with -march=native binaries that detonate on contact.
            case "compile_everything" -> {
                long compileMs = COMPILE_SECONDS * 1000L;
                compilingUntil.put(player.getUniqueId(), System.currentTimeMillis() + compileMs);
                // +1s because the loop only starts on the next second; the root is what
                // actually holds you, these two are for the HUD and the feel.
                effect(player, PotionEffectType.SLOWNESS, COMPILE_SECONDS + 1, 4);
                effect(player, PotionEffectType.MINING_FATIGUE, COMPILE_SECONDS + 1, 2);
                plugin.getBossBars().countdown(player, "compile",
                        "<light_purple>emerge --emptytree @world</light_purple>", BossBar.Color.PURPLE, compileMs);
                // A compiling Gentoo is the fattest target on the map, and everyone sees it.
                for (Entity e : player.getNearbyEntities(30, 20, 30)) {
                    if (e instanceof Player viewer) {
                        plugin.getBossBars().countdown(viewer, "compile",
                                "<light_purple>" + player.getName() + " is compiling</light_purple>",
                                BossBar.Color.PURPLE, compileMs);
                    }
                }
                DisplayFx.orbitItems(plugin, player, List.of(
                        new ItemStack(Material.BOOK), new ItemStack(Material.ENCHANTED_BOOK),
                        new ItemStack(Material.WRITABLE_BOOK)), 1.6, COMPILE_SECONDS * 20);
                announce(player, "&d:: emerge --emptytree @world &7- do not get hit",
                        Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f);
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (!player.isOnline() || player.isDead()) {
                            clearCompile(player);
                            cancel();
                            return;
                        }
                        // Missing from the map means abortCompile() already punished the player.
                        if (!compilingUntil.containsKey(player.getUniqueId())) {
                            cancel();
                            return;
                        }
                        if (t >= COMPILE_SECONDS) {
                            compilingUntil.remove(player.getUniqueId());
                            finishCompile(player, distro);
                            cancel();
                            return;
                        }
                        // Compiling runs hot. It never kills you outright - it just leaves you
                        // one bad hit away from it, which is the whole point of the gamble.
                        if (player.getHealth() > 1.5) {
                            player.damage(1.0);
                        }
                        Location at = player.getLocation();
                        plugin.getBlockTransforms().platform(at, 1 + t / 2, Material.BOOKSHELF,
                                (COMPILE_SECONDS - t) * 20L + 60L);
                        player.sendActionBar(Text.c("&d>>> Emerging (" + (t + 1) + " of " + COMPILE_SECONDS
                                + ") &7sys-kernel/gentoo-sources"));
                        Fx.spawn(player, Particle.SMOKE, 14, 0.5, 0.02);
                        at.getWorld().spawnParticle(Particle.ENCHANT, at.clone().add(0, 2.2, 0),
                                30, 0.4, 0.8, 0.4, 0.6);
                        player.playSound(at, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 0.6f + t * 0.12f);
                        t++;
                    }
                }.runTaskTimer(plugin, 20L, 20L);
            }

            // Debian - Stable Fortress: a real red-glass dome rises around you while you
            // reflect 75% of damage and bounce attackers back. An immovable bastion.
            case "stable_fortress" -> {
                fortressUntil.put(player.getUniqueId(), System.currentTimeMillis() + 6000L);
                effect(player, PotionEffectType.ABSORPTION, 8, 2);
                knockbackNearby(player, 5, 1.3);
                plugin.getBlockTransforms().encase(loc, 4, Material.RED_STAINED_GLASS, 120L);
                plugin.getBossBars().countdown(player, "fortress",
                        "<red>Stable Fortress</red>", BossBar.Color.RED, 6000L);
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (t++ >= 6 || !player.isOnline()) { cancel(); return; }
                        Fx.dustRing(player, Fx.rarityColor(distro.rarity()), 1.5, 24);
                    }
                }.runTaskTimer(plugin, 0L, 20L);
                explosionFx(loc, Particle.CRIT);
                announce(player, "&c:: Stable Fortress - dome raised, reflecting for 6s!",
                        Sound.BLOCK_NETHERITE_BLOCK_PLACE, 0.9f);
            }

            // Kali - Pentest: radar sweep animation, cripple nearby enemies, steal their
            // buffs and DDoS their ability cooldowns; wide wallhack recon.
            case "pentest" -> {
                radarSweep(player, 10, 40);
                int hit = 0;
                for (Entity e : player.getNearbyEntities(7, 4, 7)) {
                    if (e instanceof Player victim && !victim.equals(player)) {
                        // Strip their beneficial effects onto yourself.
                        for (PotionEffect pe : victim.getActivePotionEffects()) {
                            if (isBeneficial(pe.getType())) {
                                victim.removePotionEffect(pe.getType());
                                player.addPotionEffect(pe);
                            }
                        }
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 8 * 20, 1));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 8 * 20, 2));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 8 * 20, 2));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 4 * 20, 0));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 5 * 20, 1));
                        victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 10 * 20, 0));
                        victim.damage(4, player);
                        // DDoS: their ability goes on an extra 8s of cooldown.
                        PlayerDistroData vdata = plugin.getPlayerDataManager().get(victim);
                        vdata.abilityCooldownUntil = Math.max(vdata.abilityCooldownUntil,
                                System.currentTimeMillis()) + 8_000L;
                        DisplayFx.tag(plugin, victim, "&4root@" + victim.getName() + " &8- pwned", 60);
                        victim.getWorld().spawnParticle(Particle.WITCH, victim.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0);
                        Text.msg(victim, "&8You have been &5pwned&8 by " + player.getName());
                        hit++;
                    }
                }
                effect(player, PotionEffectType.SPEED, 8, 1);
                effect(player, PotionEffectType.NIGHT_VISION, 12, 0);
                // nmap sweep - every entity in a wide radius glows THROUGH walls for 12s.
                int scanned = 0;
                for (Entity e : player.getNearbyEntities(24, 12, 24)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 12 * 20, 0));
                        scanned++;
                    }
                }
                announce(player, hit > 0 ? "&5:: root@target - " + hit + " host(s) owned + " + scanned + " on radar"
                        : "&5:: nmap sweep - " + scanned + " host(s) exposed on radar", Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.2f);
                Fx.spawn(player, Particle.SCULK_SOUL, 40, 0.5, 0.05);
            }

            // Alpine - Featherweight: shrink to a few megabytes. Tiny hitbox,
            // weightless, slips into 1-block gaps for 12s.
            case "featherweight" -> {
                shrink(player, 0.55, 12 * 20);
                effect(player, PotionEffectType.SPEED, 12, 2);
                effect(player, PotionEffectType.JUMP_BOOST, 12, 2);
                effect(player, PotionEffectType.SLOW_FALLING, 12, 0);
                dash(player, 1.4, 0.5);
                announce(player, "&f:: Featherweight - shrunk to a few megabytes!", Sound.ENTITY_PHANTOM_FLAP, 1.8f);
                Fx.spawn(player, Particle.CLOUD, 40, 0.4, 0.06);
            }

            // Tails (MYTHIC) - Ghost Protocol: true vanish. Other players cannot see you at
            // all, a decoy of you stays behind, mobs forget you, and your first strike
            // from the shadows hits double.
            case "anonymity" -> {
                long until = System.currentTimeMillis() + 12_000L;
                vanishUntil.put(player.getUniqueId(), until);
                effect(player, PotionEffectType.INVISIBILITY, 12, 0);
                effect(player, PotionEffectType.SPEED, 12, 2);
                spawnDecoy(player, 12 * 20);
                int forgot = 0;
                for (Entity e : player.getNearbyEntities(24, 12, 24)) {
                    if (e instanceof Mob mob && player.equals(mob.getTarget())) {
                        mob.setTarget(null);
                        forgot++;
                    }
                }
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (!viewer.equals(player)) {
                        viewer.hidePlayer(plugin, player);
                    }
                }
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> reveal(player), 12 * 20L);
                plugin.getBossBars().countdown(player, "vanish",
                        "<dark_purple>Ghost Protocol</dark_purple>", BossBar.Color.PURPLE, 12_000L);
                loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 1, 0), 80, 0.6, 0.9, 0.6, 0.04);
                announce(player, "&5:: ghost protocol - off the grid for 12s, " + forgot
                        + " lost your trail, first strike x2", Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f);
            }

            // Ubuntu - Community Support: the LTS wave. A golden ring rolls out and every
            // ally it reaches is healed, shielded and hasted.
            case "community_support" -> {
                int helped = 0;
                heal(player, 8);
                effect(player, PotionEffectType.REGENERATION, 8, 2);
                effect(player, PotionEffectType.ABSORPTION, 30, 2);
                for (Entity e : player.getNearbyEntities(10, 5, 10)) {
                    if (e instanceof Player ally && !ally.equals(player)) {
                        heal(ally, 8);
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 8 * 20, 2));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 30 * 20, 2));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 8 * 20, 1));
                        ally.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 8 * 20, 0));
                        heartColumn(ally);
                        Text.msg(ally, "&6" + player.getName() + " &7shielded and healed you");
                        helped++;
                    }
                }
                DisplayFx.ringWave(plugin, loc, Material.ORANGE_STAINED_GLASS, 10, 24, 24);
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (t >= 20 || !player.isOnline()) { cancel(); return; }
                        Fx.helix(player, Color.fromRGB(0xE95420), t * 0.3);
                        t++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                announce(player, "&6:: Community Support - LTS wave healed " + helped + " ally(s)",
                        Sound.ENTITY_PLAYER_LEVELUP, 1.0f);
            }

            // Slackware - Old School: one hand-typed command with no autocomplete. Small
            // chance it backfires, but when it lands it calls down a REAL thunderstorm on
            // the strike zone - actual bolts that shock, burn and set the ground alight.
            case "old_school" -> {
                if (ThreadLocalRandom.current().nextDouble() < 0.12) {
                    player.damage(5, player);
                    effect(player, PotionEffectType.SLOWNESS, 6, 1);
                    effect(player, PotionEffectType.MINING_FATIGUE, 6, 1);
                    announce(player, "&8:: no autocomplete... the command backfired", Sound.BLOCK_ANVIL_LAND, 0.7f);
                    Fx.spawn(player, Particle.ASH, 30, 0.4, 0.02);
                } else {
                    var ray = player.rayTraceEntities(30);
                    LivingEntity primary = ray != null && ray.getHitEntity() instanceof LivingEntity le
                            && !le.equals(player) ? le : null;
                    Location stormCenter;
                    if (primary != null) {
                        stormCenter = primary.getLocation();
                    } else {
                        var tb = player.getTargetBlockExact(30);
                        stormCenter = tb != null ? tb.getLocation()
                                : player.getLocation().add(player.getLocation().getDirection().multiply(12));
                    }

                    // A real barrage: staggered live bolts rain across the zone (each one
                    // deals its own lightning damage and ignites what it hits).
                    final Location center = stormCenter.clone();
                    new BukkitRunnable() {
                        int bolts = 0;
                        @Override public void run() {
                            if (bolts++ >= 6) { cancel(); return; }
                            Location at = center.clone().add(
                                    ThreadLocalRandom.current().nextDouble(-3.5, 3.5), 0,
                                    ThreadLocalRandom.current().nextDouble(-3.5, 3.5));
                            at.getWorld().strikeLightning(at);
                        }
                    }.runTaskTimer(plugin, 0L, 3L);

                    // One heavy shock pass across the strike zone on top of the live bolts.
                    int hit = 0;
                    for (Entity e : stormCenter.getWorld().getNearbyEntities(stormCenter, 5, 5, 5)) {
                        if (e instanceof LivingEntity le2 && !le2.equals(player)) {
                            le2.damage(le2.equals(primary) ? 24 : 13, player);
                            le2.setFireTicks(Math.max(le2.getFireTicks(), 60));
                            dustLine(stormCenter.clone().add(0, 8, 0), le2.getLocation().add(0, 1, 0),
                                    Color.fromRGB(0xFFF06E));
                            hit++;
                        }
                    }
                    announce(player, "&2:: Old School - one command called down the storm on " + hit + " target(s)",
                            Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f);
                    Fx.spawn(player, Particle.ELECTRIC_SPARK, 60, 0.7, 0.25);
                }
            }

            // Mint - Fresh Start: a clean reinstall. Full cleanse, full food, moss blooms
            // underfoot and even your swap cooldown is wiped.
            case "fresh_start" -> {
                player.getActivePotionEffects().stream()
                        .filter(pe -> !isBeneficial(pe.getType()))
                        .forEach(pe -> player.removePotionEffect(pe.getType()));
                player.setFireTicks(0);
                heal(player, 10);
                player.setFoodLevel(20);
                player.setSaturation(20f);
                effect(player, PotionEffectType.REGENERATION, 10, 2);
                effect(player, PotionEffectType.ABSORPTION, 20, 1);
                data.swapCooldownUntil = 0;
                plugin.getBlockTransforms().platform(loc, 3, Material.MOSS_BLOCK, 200L);
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (t >= 24 || !player.isOnline()) { cancel(); return; }
                        Fx.helix(player, Color.fromRGB(0x87CF3E), t * 0.3);
                        t++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                Fx.spawn(player, Particle.COMPOSTER, 40, 0.6, 0.1);
                announce(player, "&a:: fresh install - cleansed, fed, swap ready", Sound.BLOCK_BREWING_STAND_BREW, 1.4f);
            }

            // Pop!_OS - Auto-Tiling: the tiling WM takes over. You dash, your inventory is
            // tiled, and every enemy window nearby gets snapped in place.
            case "auto_tiling" -> {
                effect(player, PotionEffectType.SPEED, 12, 2);
                effect(player, PotionEffectType.JUMP_BOOST, 12, 2);
                effect(player, PotionEffectType.HASTE, 12, 1);
                dash(player, 1.3, 0.5);
                int tidied = tileInventory(player);
                int locked = 0;
                for (Entity e : player.getNearbyEntities(6, 4, 6)) {
                    if (e instanceof LivingEntity le && !le.equals(player)) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 5));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 3 * 20, 2));
                        le.getWorld().spawnParticle(Particle.END_ROD, le.getLocation().add(0, 1, 0), 12, 0.3, 0.6, 0.3, 0);
                        locked++;
                    }
                }
                tileGrid(loc);
                DisplayFx.ringWave(plugin, loc, Material.WHITE_STAINED_GLASS, 6, 16, 14);
                announce(player, "&3:: auto-tiling - inventory tiled (" + tidied + "), "
                        + locked + " window(s) snapped", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f);
            }

            // Fedora - Bleeding Edge: the newest kernel spins up a blade aura - swords
            // orbit you for 8s and shred anyone who stays close.
            case "bleeding_edge" -> {
                effect(player, PotionEffectType.SPEED, 12, 1);
                effect(player, PotionEffectType.HASTE, 12, 2);
                effect(player, PotionEffectType.STRENGTH, 12, 1);
                DisplayFx.orbitItems(plugin, player, List.of(
                        new ItemStack(Material.DIAMOND_SWORD), new ItemStack(Material.DIAMOND_SWORD),
                        new ItemStack(Material.DIAMOND_SWORD), new ItemStack(Material.DIAMOND_SWORD)), 2.2, 8 * 20);
                new BukkitRunnable() {
                    int t = 0;
                    @Override public void run() {
                        if (t++ >= 16 || !player.isOnline()) { cancel(); return; }
                        boolean cut = false;
                        for (Entity e : player.getNearbyEntities(3, 2.5, 3)) {
                            if (e instanceof LivingEntity le && !le.equals(player)) {
                                le.damage(2, player);
                                le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 2 * 20, 0));
                                cut = true;
                            }
                        }
                        if (cut) {
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);
                        }
                    }
                }.runTaskTimer(plugin, 10L, 10L);
                announce(player, "&9:: bleeding edge - blade aura online (8s)", Sound.BLOCK_BEACON_ACTIVATE, 1.5f);
                Fx.spawn(player, Particle.SOUL_FIRE_FLAME, 30, 0.4, 0.06);
            }

            // Manjaro - AUR Access: yay -Syu. A shulker crate air-drops from the sky and
            // bursts into real packages at your feet.
            case "aur_access" -> {
                effect(player, PotionEffectType.HASTE, 12, 1);
                announce(player, "&2:: yay -Syu - package incoming...", Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f);
                Location target = loc.clone();
                DisplayFx.drop(plugin, target, Material.LIME_SHULKER_BOX, 12, 14, () -> {
                    World world = target.getWorld();
                    world.spawnParticle(Particle.BLOCK, target.clone().add(0, 0.4, 0), 40, 0.5, 0.3, 0.5, 0,
                            Material.LIME_SHULKER_BOX.createBlockData());
                    world.playSound(target, Sound.BLOCK_SHULKER_BOX_OPEN, 1f, 1.1f);
                    StringBuilder got = new StringBuilder();
                    for (int i = 0; i < 2; i++) {
                        ItemStack pkg = randomAurPackage();
                        if (player.isOnline()) {
                            player.getInventory().addItem(pkg).values()
                                    .forEach(left -> world.dropItemNaturally(target, left));
                        } else {
                            world.dropItemNaturally(target, pkg);
                        }
                        if (!got.isEmpty()) {
                            got.append("&7, &2");
                        }
                        got.append(pkg.getType().name().toLowerCase()).append(" &7x").append(pkg.getAmount());
                    }
                    if (ThreadLocalRandom.current().nextDouble() < 0.12 && player.isOnline()) {
                        effect(player, PotionEffectType.NAUSEA, 6, 0);
                        world.spawnParticle(Particle.LARGE_SMOKE, target.clone().add(0, 1, 0), 30, 0.4, 0.5, 0.4, 0.02);
                        Text.msg(player, "&c:: AUR comment: \"this package is malware\" - too late.");
                    }
                    if (player.isOnline()) {
                        announce(player, "&2:: yay -S &2" + got, Sound.ENTITY_ITEM_PICKUP, 1.0f);
                        Fx.burst(player, distro.rarity());
                    }
                });
            }

            // LFS - From Scratch (MYTHIC): the whole system is rebuilt around you - a tower
            // stacks up layer by layer, then detonates into a god-tier rebuild.
            case "from_scratch" -> {
                announce(player, "&4:: rebuilding the system from scratch...", Sound.BLOCK_ANVIL_USE, 0.8f);
                DisplayFx.tower(plugin, loc, List.of(
                        Material.DIRT, Material.OAK_PLANKS, Material.STONE,
                        Material.IRON_BLOCK, Material.DIAMOND_BLOCK, Material.BEACON), 3, 6);
                player.getActivePotionEffects().stream()
                        .filter(pe -> !isBeneficial(pe.getType()))
                        .forEach(pe -> player.removePotionEffect(pe.getType()));
                heal(player, 40);
                effect(player, PotionEffectType.ABSORPTION, 25, 3);
                effect(player, PotionEffectType.STRENGTH, 12, 2);
                effect(player, PotionEffectType.RESISTANCE, 12, 2);
                effect(player, PotionEffectType.REGENERATION, 12, 2);
                effect(player, PotionEffectType.SPEED, 12, 1);
                effect(player, PotionEffectType.FIRE_RESISTANCE, 12, 0);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    Location at = player.getLocation();
                    int hit = damageNearby(player, 8, 10, 1.5);
                    explosionFx(at, Particle.EXPLOSION_EMITTER);
                    player.playSound(at, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
                    announce(player, "&4&l:: SYSTEM REBUILT FROM SCRATCH &r&8(blasted x" + hit + ")",
                            Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f);
                    Fx.burst(player, distro.rarity());
                }, 20L);
            }

            default -> {
                effect(player, PotionEffectType.SPEED, 5, 0);
                announce(player, "&7:: " + distro.ability(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f);
            }
        }
    }

    /** Damages every other living entity in range and knocks them away. Returns count hit. */
    private int damageNearby(Player player, double radius, double damage, double knockback) {
        int hit = 0;
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity le && !le.equals(player)) {
                le.damage(damage, player);
                pushAway(player, le, knockback);
                hit++;
            }
        }
        return hit;
    }

    /** Knocks nearby entities away without dealing damage. */
    private void knockbackNearby(Player player, double radius, double strength) {
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity le && !le.equals(player)) {
                pushAway(player, le, strength);
            }
        }
    }

    private void pushAway(Player source, LivingEntity target, double strength) {
        Vector dir = target.getLocation().toVector().subtract(source.getLocation().toVector());
        if (dir.lengthSquared() < 0.01) {
            dir = source.getLocation().getDirection();
        }
        dir = dir.setY(0).normalize().multiply(strength).setY(0.45);
        target.setVelocity(target.getVelocity().add(dir));
    }

    /** Merges partial stacks and sorts the main inventory (slots 9-35). Returns slots used. */
    private int tileInventory(Player player) {
        var inv = player.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int s = 9; s <= 35; s++) {
            ItemStack it = inv.getItem(s);
            if (it == null || it.getType().isAir()) {
                continue;
            }
            boolean done = false;
            for (ItemStack ex : items) {
                if (ex.isSimilar(it) && ex.getAmount() < ex.getMaxStackSize()) {
                    int move = Math.min(ex.getMaxStackSize() - ex.getAmount(), it.getAmount());
                    ex.setAmount(ex.getAmount() + move);
                    it.setAmount(it.getAmount() - move);
                    if (it.getAmount() <= 0) {
                        done = true;
                        break;
                    }
                }
            }
            if (!done && it.getAmount() > 0) {
                items.add(it);
            }
        }
        items.sort(Comparator.comparing((ItemStack i) -> i.getType().name())
                .thenComparingInt(i -> -i.getAmount()));
        for (int s = 9; s <= 35; s++) {
            inv.setItem(s, null);
        }
        int idx = 9;
        for (ItemStack it : items) {
            inv.setItem(idx++, it);
        }
        player.updateInventory();
        return items.size();
    }

    /** Temporarily shrinks the player model (and hitbox) via the SCALE attribute. */
    private void shrink(Player player, double scale, int ticks) {
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.SCALE);
        if (attr == null) {
            return;
        }
        attr.setBaseValue(scale);
        new BukkitRunnable() {
            @Override
            public void run() {
                var a = player.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                if (a != null) {
                    a.setBaseValue(1.0);
                }
            }
        }.runTaskLater(plugin, ticks);
    }

    /** A random useful "package" pulled from the AUR. */
    private ItemStack randomAurPackage() {
        record Pkg(Material mat, int amount) {
        }
        List<Pkg> pool = List.of(
                new Pkg(Material.GOLDEN_APPLE, 1),
                new Pkg(Material.ENDER_PEARL, 2),
                new Pkg(Material.EXPERIENCE_BOTTLE, 6),
                new Pkg(Material.ARROW, 16),
                new Pkg(Material.COOKED_BEEF, 8),
                new Pkg(Material.IRON_INGOT, 3),
                new Pkg(Material.TNT, 2),
                new Pkg(Material.TORCH, 12),
                new Pkg(Material.GOLDEN_CARROT, 6));
        Pkg p = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        return new ItemStack(p.mat(), p.amount());
    }

    /** Ends a Tails vanish: visible to everyone again, invisibility stripped. */
    private void reveal(Player player) {
        vanishUntil.remove(player.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.showPlayer(plugin, player);
        }
        if (player.isOnline()) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            plugin.getBossBars().clear(player, "vanish");
            player.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                    player.getLocation().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.02);
        }
    }

    /** Leaves a look-alike armor stand behind so "you" are still seen standing there. */
    private void spawnDecoy(Player player, int ticks) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        ArmorStand decoy = player.getWorld().spawn(player.getLocation(), ArmorStand.class, as -> {
            as.setPersistent(false);
            as.customName(Text.c("&7" + player.getName()));
            as.setCustomNameVisible(true);
            as.setArms(true);
            as.getEquipment().setHelmet(head);
            as.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        });
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (decoy.isValid()) {
                decoy.getWorld().spawnParticle(Particle.CLOUD, decoy.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                decoy.remove();
            }
        }, ticks);
    }

    /** Rising heart column above an ally (Ubuntu support). */
    private void heartColumn(Player ally) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 12 || !ally.isOnline()) { cancel(); return; }
                ally.getWorld().spawnParticle(Particle.HEART,
                        ally.getLocation().add(0, 1.2 + t * 0.15, 0), 2, 0.25, 0.05, 0.25, 0);
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Rotating green radar line sweeping around the player (Kali). */
    private void radarSweep(Player player, double range, int ticks) {
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= ticks || !player.isOnline()) { cancel(); return; }
                double a = 4 * Math.PI * t / ticks; // two full sweeps
                Location base = player.getLocation().add(0, 0.2, 0);
                Particle.DustOptions opt = new Particle.DustOptions(Color.fromRGB(0x00FF66), 1.1f);
                for (double d = 1; d <= range; d += 0.5) {
                    player.getWorld().spawnParticle(Particle.DUST,
                            base.clone().add(Math.cos(a) * d, 0, Math.sin(a) * d), 1, 0, 0, 0, 0, opt);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** END_ROD grid lines on the floor - the tiling WM's wallpaper (Pop!_OS). */
    private void tileGrid(Location center) {
        World world = center.getWorld();
        for (int i = -4; i <= 4; i += 2) {
            for (double d = -4; d <= 4; d += 0.5) {
                world.spawnParticle(Particle.END_ROD, center.clone().add(i, 0.15, d), 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, center.clone().add(d, 0.15, i), 1, 0, 0, 0, 0);
            }
        }
    }

    /** Straight dust line between two points (Slackware chain arcs). */
    private void dustLine(Location from, Location to, Color color) {
        Vector dir = to.toVector().subtract(from.toVector());
        double len = dir.length();
        if (len < 0.1) {
            return;
        }
        dir.normalize();
        Particle.DustOptions opt = new Particle.DustOptions(color, 1.2f);
        for (double d = 0; d <= len; d += 0.4) {
            from.getWorld().spawnParticle(Particle.DUST,
                    from.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0, opt);
        }
    }

    private void dash(Player player, double power, double lift) {
        player.setVelocity(player.getLocation().getDirection().multiply(power).setY(lift));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.2, 0.1, 0.2, 0.05);
    }

    private void heal(Player player, double amount) {
        double max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.min(max, player.getHealth() + amount));
    }

    /** The emerge landed: everything within 10 blocks eats the linker, and the binaries go hot. */
    private void finishCompile(Player player, Distro distro) {
        clearCompile(player);
        // The build is done - none of the compile drag should bleed into the native window.
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        Location at = player.getLocation();
        int hit = damageNearby(player, 10, 16, 1.6);
        nativeUntil.put(player.getUniqueId(), System.currentTimeMillis() + NATIVE_SECONDS * 1000L);
        plugin.getBlockTransforms().fillSolid(at, 5, Material.BOOKSHELF, 300L);
        plugin.getBossBars().countdown(player, "native",
                "<light_purple>-march=native</light_purple>", BossBar.Color.PURPLE, NATIVE_SECONDS * 1000L);
        effect(player, PotionEffectType.STRENGTH, NATIVE_SECONDS, 2);
        effect(player, PotionEffectType.SPEED, NATIVE_SECONDS, 1);
        effect(player, PotionEffectType.RESISTANCE, NATIVE_SECONDS, 1);
        effect(player, PotionEffectType.HASTE, NATIVE_SECONDS, 3);
        effect(player, PotionEffectType.ABSORPTION, NATIVE_SECONDS, 3);
        at.getWorld().spawnParticle(Particle.ENCHANT, at.clone().add(0, 1, 0), 160, 5, 3, 5, 1);
        explosionFx(at, Particle.EXPLOSION_EMITTER);
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 8, 2.5, 1, 2.5, 0);
        player.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        player.playSound(at, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.4f);
        Text.msg(player, "&d>>> Emerged &f@world&d - " + NATIVE_SECONDS + "s of -march=native");
        announce(player, "&d:: compiled clean, blast radius x" + hit, Sound.BLOCK_BEACON_ACTIVATE, 1.4f);
        Fx.burst(player, distro.rarity());
    }

    /** Someone landed a hit mid-emerge: the build dies and the wasted hours are charged back. */
    private void abortCompile(Player player, Player breaker) {
        clearCompile(player);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.MINING_FATIGUE);
        effect(player, PotionEffectType.WEAKNESS, 10, 1);
        effect(player, PotionEffectType.SLOWNESS, 10, 1);
        player.damage(8);
        // A failed build costs double: you wait out two full cooldowns before trying again.
        long penalty = plugin.getConfig().getInt("ability.cooldown-seconds", 30) * 2000L;
        plugin.getPlayerDataManager().get(player).abilityCooldownUntil = System.currentTimeMillis() + penalty;
        plugin.getBossBars().countdown(player, "ability",
                "<red>emerge failed - rebuilding</red>", BossBar.Color.RED, penalty);
        Location at = player.getLocation();
        at.getWorld().spawnParticle(Particle.LARGE_SMOKE, at.clone().add(0, 1, 0), 60, 0.8, 1, 0.8, 0.05);
        player.playSound(at, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
        Text.msg(player, "&4!!! ERROR: sys-kernel/gentoo-sources failed (compile phase)");
        Text.msg(player, "&7    build interrupted by &f" + breaker.getName());
        announce(player, "&c:: emerge aborted - " + COMPILE_SECONDS + "s of CPU burned for nothing",
                Sound.ENTITY_ITEM_BREAK, 0.7f);
        breaker.sendActionBar(Text.c("&a:: you killed " + player.getName() + "'s build"));
    }

    /** Drops the compile state and every boss bar it put up, on the caster and on watchers. */
    private void clearCompile(Player player) {
        compilingUntil.remove(player.getUniqueId());
        plugin.getBossBars().clear(player, "compile");
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            plugin.getBossBars().clear(viewer, "compile");
        }
    }

    /** Every hit from a native build carries the blast into whatever stands next to the target. */
    private void nativeDetonation(Player attacker, LivingEntity target) {
        Location at = target.getLocation();
        detonating = true;
        try {
            for (Entity e : target.getNearbyEntities(2.5, 2.0, 2.5)) {
                if (e instanceof LivingEntity le && !le.equals(attacker) && !le.equals(target)) {
                    le.damage(4, attacker);
                }
            }
        } finally {
            detonating = false;
        }
        at.getWorld().spawnParticle(Particle.EXPLOSION, at.clone().add(0, 1, 0), 3, 0.4, 0.4, 0.4, 0);
        at.getWorld().spawnParticle(Particle.ENCHANT, at.clone().add(0, 1, 0), 20, 0.6, 0.6, 0.6, 0.8);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.6f);
    }

    private void explosionFx(Location loc, Particle particle) {
        loc.getWorld().spawnParticle(particle, loc.clone().add(0, 1, 0), 30, 1.5, 0.8, 1.5, 0.05);
    }

    private boolean isBeneficial(PotionEffectType type) {
        return type == PotionEffectType.STRENGTH || type == PotionEffectType.SPEED
                || type == PotionEffectType.RESISTANCE || type == PotionEffectType.REGENERATION
                || type == PotionEffectType.ABSORPTION || type == PotionEffectType.HASTE
                || type == PotionEffectType.JUMP_BOOST || type == PotionEffectType.FIRE_RESISTANCE
                || type == PotionEffectType.LUCK
                || type == PotionEffectType.INVISIBILITY || type == PotionEffectType.NIGHT_VISION;
    }

    private void announce(Player player, String msg, Sound sound, float pitch) {
        player.sendActionBar(Text.c(msg));
        player.playSound(player.getLocation(), sound, 0.9f, pitch);
    }

    private void effect(Player player, PotionEffectType type, int seconds, int amplifier) {
        player.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier));
    }
}
