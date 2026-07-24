package com.rubka.linuxsmp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

public class RollCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public RollCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.c("Players only."));
            return true;
        }
        boolean premium = args.length > 0 && args[0].equalsIgnoreCase("premium");
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        var config = plugin.getConfig();

        Distro result;
        if (premium) {
            long cost = config.getLong("roll.premium-cost-repotokens", 1);
            if (data.repoTokens < cost) {
                Text.msg(player, "&cPremium roll costs &b" + cost + " RepoTokens&c. You have "
                        + data.repoTokens + ".");
                return true;
            }
            data.repoTokens -= cost;
            Map<Rarity, Integer> weights = new EnumMap<>(Rarity.class);
            var section = config.getConfigurationSection("premium-rarity-weights");
            if (section != null) {
                for (Rarity rarity : Rarity.values()) {
                    int weight = section.getInt(rarity.name(), 0);
                    if (weight > 0) {
                        weights.put(rarity, weight);
                    }
                }
            }
            result = plugin.getDistroManager().roll(weights);
        } else {
            boolean free = config.getBoolean("roll.first-roll-free", true)
                    && data.unlocked.isEmpty() && data.permaBricked.isEmpty();
            long cost = config.getLong("roll.cost-packages", 100);
            if (!free) {
                if (data.packages < cost) {
                    Text.msg(player, "&cA roll costs &e" + cost + " Packages&c. You have "
                            + data.packages + ".");
                    return true;
                }
                data.packages -= cost;
            }
            result = plugin.getDistroManager().roll();
        }

        if (result == null) {
            Text.msg(player, "&cNo distros configured.");
            return true;
        }

        boolean duplicate = data.unlocked.contains(result.id());
        long refund = 0;
        if (duplicate) {
            refund = config.getLong("roll.duplicate-refund." + result.rarity().name(), 25);
            data.packages += refund;
        } else {
            data.unlocked.add(result.id());
            data.permaBricked.remove(result.id());
            if (data.active == null) {
                data.active = result.id();
                data.lastMaintenance = System.currentTimeMillis();
                PassiveTask.onSwitch(plugin, player, data);
            }
        }

        new RollAnimation(plugin, player, result, duplicate, refund).start();
        return true;
    }
}
