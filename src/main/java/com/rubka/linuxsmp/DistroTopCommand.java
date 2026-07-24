package com.rubka.linuxsmp;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ranks players by collection points (rarer distros are worth more). */
public class DistroTopCommand implements CommandExecutor {

    private record Entry(UUID uuid, int count, int points) {
    }

    private final LinuxSMPPlugin plugin;

    public DistroTopCommand(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<Entry> board = new ArrayList<>();
        for (Map.Entry<UUID, List<String>> e : plugin.getPlayerDataManager().collectionSnapshot().entrySet()) {
            int count = 0;
            int points = 0;
            for (String id : e.getValue()) {
                Distro distro = plugin.getDistroManager().get(id);
                if (distro != null) {
                    count++;
                    points += distro.rarity().points;
                }
            }
            if (count > 0) {
                board.add(new Entry(e.getKey(), count, points));
            }
        }
        board.sort(Comparator.comparingInt(Entry::points).reversed()
                .thenComparing(Comparator.comparingInt(Entry::count).reversed()));

        sender.sendMessage(Text.mm("<gradient:#7f5af0:#2cb67d><bold>Distro Top</bold></gradient> "
                + "<dark_gray>- by collection points</dark_gray>"));
        if (board.isEmpty()) {
            sender.sendMessage(Text.c("&7No collections yet. Be the first: &f/distroroll"));
            return true;
        }

        int shown = Math.min(10, board.size());
        for (int i = 0; i < shown; i++) {
            Entry en = board.get(i);
            sender.sendMessage(Text.c(rankColor(i) + "#" + (i + 1) + " &f" + name(en.uuid())
                    + " &8- &e" + en.points() + " pts &7(" + en.count() + " distros)"));
        }

        // Show the sender's own rank if they are outside the top list.
        if (sender instanceof org.bukkit.entity.Player player) {
            int rank = indexOf(board, player.getUniqueId());
            if (rank >= shown) {
                Entry en = board.get(rank);
                sender.sendMessage(Text.c("&8..."));
                sender.sendMessage(Text.c("&7#" + (rank + 1) + " &f" + name(en.uuid())
                        + " &8- &e" + en.points() + " pts &7(" + en.count() + " distros)"));
            }
        }
        return true;
    }

    private int indexOf(List<Entry> board, UUID uuid) {
        for (int i = 0; i < board.size(); i++) {
            if (board.get(i).uuid().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

    private String name(UUID uuid) {
        String n = Bukkit.getOfflinePlayer(uuid).getName();
        return n != null ? n : uuid.toString().substring(0, 8);
    }

    private String rankColor(int index) {
        return switch (index) {
            case 0 -> "&6";
            case 1 -> "&7";
            case 2 -> "&c";
            default -> "&8";
        };
    }
}
