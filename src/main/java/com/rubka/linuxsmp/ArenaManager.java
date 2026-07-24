package com.rubka.linuxsmp;

import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ArenaManager implements Listener {

    private record Challenge(UUID challenger, long expiresAt) {
    }

    private final LinuxSMPPlugin plugin;
    private final Map<UUID, Challenge> pending = new HashMap<>();
    private final Map<UUID, UUID> duels = new HashMap<>();

    public ArenaManager(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void challenge(Player challenger, Player target) {
        if (challenger.equals(target)) {
            challenger.sendMessage(Text.c("&cYou can't duel yourself."));
            return;
        }
        if (isDueling(challenger) || isDueling(target)) {
            challenger.sendMessage(Text.c("&cOne of you is already in a duel."));
            return;
        }
        Distro own = activeDistro(challenger);
        Distro theirs = activeDistro(target);
        if (own == null) {
            challenger.sendMessage(Text.c("&cYou need an active distro to duel."));
            return;
        }
        if (theirs == null) {
            challenger.sendMessage(Text.c("&cYour opponent has no active distro."));
            return;
        }
        if (plugin.getConfig().getBoolean("duel.same-rarity-only", false) && own.rarity() != theirs.rarity()) {
            challenger.sendMessage(Text.c("&cSame-rarity duels only: yours is " + own.rarity().color
                    + own.rarity() + "&c, theirs is " + theirs.rarity().color + theirs.rarity() + "&c."));
            return;
        }
        pending.put(target.getUniqueId(),
                new Challenge(challenger.getUniqueId(), System.currentTimeMillis() + 30_000));
        challenger.sendMessage(Text.c("&7Duel request sent to &f" + target.getName() + "&7."));
        target.sendMessage(Text.c("&6" + challenger.getName() + " &7challenges you to a distro duel! "
                + "&f/distroduel accept &7(30s)"));
    }

    public void accept(Player target) {
        Challenge challenge = pending.remove(target.getUniqueId());
        if (challenge == null || challenge.expiresAt() < System.currentTimeMillis()) {
            target.sendMessage(Text.c("&cNo pending duel request."));
            return;
        }
        Player challenger = Bukkit.getPlayer(challenge.challenger());
        if (challenger == null || !challenger.isOnline()) {
            target.sendMessage(Text.c("&cChallenger is offline."));
            return;
        }
        duels.put(challenger.getUniqueId(), target.getUniqueId());
        duels.put(target.getUniqueId(), challenger.getUniqueId());
        for (Player player : new Player[]{challenger, target}) {
            player.showTitle(Title.title(
                    Text.c("&6DISTRO DUEL"),
                    Text.c("&7" + challenger.getName() + " &fvs &7" + target.getName()),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
            ));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
        }
    }

    public void deny(Player target) {
        if (pending.remove(target.getUniqueId()) != null) {
            target.sendMessage(Text.c("&7Duel request denied."));
        } else {
            target.sendMessage(Text.c("&cNo pending duel request."));
        }
    }

    public boolean isDueling(Player player) {
        return duels.containsKey(player.getUniqueId());
    }

    private Distro activeDistro(Player player) {
        return plugin.getDistroManager().get(plugin.getPlayerDataManager().get(player).active);
    }

    private void finish(UUID loserId, UUID winnerId) {
        duels.remove(loserId);
        duels.remove(winnerId);
        long winReward = plugin.getConfig().getLong("duel.win-packages", 150);
        long loseReward = plugin.getConfig().getLong("duel.lose-packages", 25);
        plugin.getPlayerDataManager().get(winnerId).packages += winReward;
        plugin.getPlayerDataManager().get(loserId).packages += loseReward;
        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);
        if (winner != null) {
            winner.sendMessage(Text.c("&aDuel won! &e+" + winReward + " Packages"));
        }
        if (loser != null) {
            loser.sendMessage(Text.c("&7Duel lost. &e+" + loseReward + " Packages &7for the effort."));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID loser = event.getPlayer().getUniqueId();
        UUID winner = duels.get(loser);
        if (winner != null) {
            finish(loser, winner);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID quitter = event.getPlayer().getUniqueId();
        pending.remove(quitter);
        UUID opponent = duels.get(quitter);
        if (opponent != null) {
            finish(quitter, opponent);
        }
    }
}
