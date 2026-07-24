package com.rubka.linuxsmp;

import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.concurrent.ThreadLocalRandom;

public class LootListener implements Listener {

    private final LinuxSMPPlugin plugin;

    public LootListener(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        var config = plugin.getConfig();
        var random = ThreadLocalRandom.current();
        PlayerDistroData data = plugin.getPlayerDataManager().get(killer);

        if (random.nextDouble() < config.getDouble("loot.packages-chance", 0.30)) {
            int min = config.getInt("loot.packages-min", 1);
            int max = config.getInt("loot.packages-max", 3);
            int amount = random.nextInt(min, max + 1);
            data.packages += amount;
            killer.sendActionBar(Text.c("&e+" + amount + " Packages"));
        }
        if (random.nextDouble() < config.getDouble("loot.repotoken-chance", 0.02)) {
            data.repoTokens++;
            killer.sendActionBar(Text.c("&b+1 RepoToken"));
        }
        if (random.nextDouble() < config.getDouble("loot.dotfile-chance", 0.05)) {
            data.dotfiles++;
            killer.sendActionBar(Text.c("&d+1 Dotfile"));
        }
        if (random.nextDouble() < config.getDouble("loot.recovery-iso-chance", 0.005)) {
            event.getDrops().add(plugin.getItemFactory().recoveryIso());
        }
    }
}
