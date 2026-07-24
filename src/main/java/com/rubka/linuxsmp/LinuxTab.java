package com.rubka.linuxsmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tab completion for every LinuxSMP command. */
public class LinuxTab implements TabCompleter {

    private final LinuxSMPPlugin plugin;

    public LinuxTab(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> pool = switch (name) {
            case "distroroll" -> args.length == 1 ? List.of("premium") : List.of();
            case "distroswap" -> args.length == 1 ? unlockedDistros(sender) : List.of();
            case "distrorepair" -> args.length == 1 ? brickedDistros(sender) : List.of();
            case "distrotoken" -> args.length == 1 ? unlockedDistros(sender) : List.of();
            case "distroduel" -> duel(args);
            case "linuxsmp" -> admin(args);
            default -> List.of();
        };
        return filter(pool, args);
    }

    private List<String> duel(String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>(List.of("accept", "deny"));
            Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            return out;
        }
        return List.of();
    }

    private List<String> admin(String[] args) {
        if (args.length == 1) {
            return List.of("reload", "give", "iso", "token", "exchange");
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("give") || sub.equals("iso") || sub.equals("token"))) {
            return onlineNames();
        }
        if (sub.equals("give") && args.length == 3) {
            return List.of("packages", "repotokens", "dotfiles");
        }
        if (sub.equals("token") && args.length == 3) {
            return allDistros();
        }
        if (sub.equals("exchange")) {
            if (args.length == 2) return List.of("buy", "sell");
            if (args.length == 3) return List.of("packages", "repotokens");
        }
        return List.of();
    }

    private List<String> allDistros() {
        return new ArrayList<>(plugin.getDistroManager().all().keySet());
    }

    private List<String> unlockedDistros(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return allDistros();
        }
        return new ArrayList<>(plugin.getPlayerDataManager().get(player).unlocked);
    }

    private List<String> brickedDistros(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return allDistros();
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        List<String> out = new ArrayList<>(data.permaBricked);
        data.tempBrickedUntil.keySet().stream()
                .filter(data::isTempBricked)
                .forEach(out::add);
        return out;
    }

    private List<String> onlineNames() {
        List<String> out = new ArrayList<>();
        Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        return out;
    }

    private List<String> filter(List<String> pool, String[] args) {
        if (pool.isEmpty()) {
            return pool;
        }
        List<String> out = new ArrayList<>();
        StringUtil.copyPartialMatches(args[args.length - 1], pool, out);
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }
}
