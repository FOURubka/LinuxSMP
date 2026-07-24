package com.rubka.linuxsmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class AdminCommand implements CommandExecutor {

    private final LinuxSMPPlugin plugin;

    public AdminCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("linuxsmp.admin")) {
                    sender.sendMessage(Text.c("&cNo permission."));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(Text.c("&aLinuxSMP reloaded."));
            }
            case "give" -> {
                if (!sender.hasPermission("linuxsmp.admin")) {
                    sender.sendMessage(Text.c("&cNo permission."));
                    return true;
                }
                if (args.length != 4) {
                    sender.sendMessage(Text.c("&7Usage: &f/linuxsmp give <player> <packages|repotokens|dotfiles> <amount>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Text.c("&cPlayer not found."));
                    return true;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Text.c("&cInvalid amount."));
                    return true;
                }
                PlayerDistroData data = plugin.getPlayerDataManager().get(target);
                switch (args[2].toLowerCase(Locale.ROOT)) {
                    case "packages" -> data.packages += amount;
                    case "repotokens" -> data.repoTokens += amount;
                    case "dotfiles" -> data.dotfiles += amount;
                    default -> {
                        sender.sendMessage(Text.c("&cUnknown currency."));
                        return true;
                    }
                }
                sender.sendMessage(Text.c("&aGave &f" + amount + " " + args[2] + " &ato " + target.getName() + "."));
            }
            case "iso" -> {
                if (!sender.hasPermission("linuxsmp.admin")) {
                    sender.sendMessage(Text.c("&cNo permission."));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(Text.c("&7Usage: &f/linuxsmp iso <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Text.c("&cPlayer not found."));
                    return true;
                }
                target.getInventory().addItem(plugin.getItemFactory().recoveryIso());
                sender.sendMessage(Text.c("&aGave a Recovery ISO to " + target.getName() + "."));
            }
            case "token" -> {
                if (!sender.hasPermission("linuxsmp.admin")) {
                    sender.sendMessage(Text.c("&cNo permission."));
                    return true;
                }
                if (args.length != 3) {
                    sender.sendMessage(Text.c("&7Usage: &f/linuxsmp token <player> <distro>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                Distro distro = plugin.getDistroManager().get(args[2].toLowerCase(Locale.ROOT));
                if (target == null || distro == null) {
                    sender.sendMessage(Text.c("&cPlayer or distro not found."));
                    return true;
                }
                target.getInventory().addItem(plugin.getItemFactory().token(distro));
                sender.sendMessage(Text.c("&aGave a " + distro.displayName() + " &atoken to " + target.getName() + "."));
            }
            case "exchange" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Text.c("Players only."));
                    return true;
                }
                if (!plugin.getEconomyHook().isAvailable()) {
                    player.sendMessage(Text.c("&cCurrency exchange is disabled (Vault not found)."));
                    return true;
                }
                if (args.length != 4) {
                    player.sendMessage(Text.c("&7Usage: &f/linuxsmp exchange <buy|sell> <packages|repotokens> <amount>"));
                    return true;
                }
                long amount;
                try {
                    amount = Long.parseLong(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Text.c("&cInvalid amount."));
                    return true;
                }
                if (amount <= 0) {
                    player.sendMessage(Text.c("&cAmount must be positive."));
                    return true;
                }
                PlayerDistroData data = plugin.getPlayerDataManager().get(player);
                EconomyHook economy = plugin.getEconomyHook();
                boolean buy = args[1].equalsIgnoreCase("buy");
                String what = args[2].toLowerCase(Locale.ROOT);
                if (buy && what.equals("packages")) {
                    economy.buyPackages(player, data, amount);
                } else if (buy && what.equals("repotokens")) {
                    economy.buyRepoTokens(player, data, amount);
                } else if (!buy && what.equals("packages")) {
                    economy.sellPackages(player, data, amount);
                } else {
                    player.sendMessage(Text.c("&cOnly: buy packages, buy repotokens, sell packages."));
                }
            }
            default -> usage(sender);
        }
        return true;
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(Text.c("&7/linuxsmp reload &8| &7give <player> <currency> <amount> &8| "
                + "&7iso <player> &8| &7token <player> <distro> &8| &7exchange <buy|sell> <what> <amount>"));
    }
}
