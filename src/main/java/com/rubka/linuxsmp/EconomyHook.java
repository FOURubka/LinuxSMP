package com.rubka.linuxsmp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHook {

    private final LinuxSMPPlugin plugin;
    private Economy economy;

    public EconomyHook(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found - currency exchange disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> provider =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider != null) {
            economy = provider.getProvider();
            plugin.getLogger().info("Hooked into Vault economy: " + economy.getName());
        }
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean buyPackages(Player player, PlayerDistroData data, long amount) {
        double cost = amount * plugin.getConfig().getDouble("economy.package-buy-price", 10.0);
        if (!economy.has(player, cost)) {
            player.sendMessage(Text.c("&cNot enough money: costs " + economy.format(cost) + "."));
            return false;
        }
        economy.withdrawPlayer(player, cost);
        data.packages += amount;
        player.sendMessage(Text.c("&aBought &e" + amount + " Packages &afor " + economy.format(cost) + "."));
        return true;
    }

    public boolean sellPackages(Player player, PlayerDistroData data, long amount) {
        if (data.packages < amount) {
            player.sendMessage(Text.c("&cYou only have &e" + data.packages + " Packages&c."));
            return false;
        }
        double payout = amount * plugin.getConfig().getDouble("economy.package-sell-price", 5.0);
        data.packages -= amount;
        economy.depositPlayer(player, payout);
        player.sendMessage(Text.c("&aSold &e" + amount + " Packages &afor " + economy.format(payout) + "."));
        return true;
    }

    public boolean buyRepoTokens(Player player, PlayerDistroData data, long amount) {
        double cost = amount * plugin.getConfig().getDouble("economy.repotoken-buy-price", 500.0);
        if (!economy.has(player, cost)) {
            player.sendMessage(Text.c("&cNot enough money: costs " + economy.format(cost) + "."));
            return false;
        }
        economy.withdrawPlayer(player, cost);
        data.repoTokens += amount;
        player.sendMessage(Text.c("&aBought &b" + amount + " RepoTokens &afor " + economy.format(cost) + "."));
        return true;
    }
}
