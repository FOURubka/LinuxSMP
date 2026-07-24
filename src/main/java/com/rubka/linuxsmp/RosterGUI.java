package com.rubka.linuxsmp;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RosterGUI implements InventoryHolder, LinuxGui {

    private static final int[] DISTRO_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final LinuxSMPPlugin plugin;
    private final Inventory inventory;
    private final Map<Integer, String> slotToDistro = new HashMap<>();

    public RosterGUI(LinuxSMPPlugin plugin, Player player) {
        this.plugin = plugin;
        this.inventory = plugin.getServer().createInventory(this, 54,
                Text.mm("<gradient:#7f5af0:#2cb67d><bold>Distro Roster</bold></gradient>"));
        build(player);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    /** Click an unlocked, non-bricked distro to make it active. */
    public void handleClick(Player player, int slot) {
        String id = slotToDistro.get(slot);
        if (id == null) {
            return;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        Distro distro = plugin.getDistroManager().get(id);
        if (distro == null || !data.unlocked.contains(id)) {
            return;
        }
        if (data.isTempBricked(id) || data.permaBricked.contains(id)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f);
            Text.msg(player, "&c" + distro.displayName() + " &cis bricked.");
            return;
        }
        if (id.equals(data.active)) {
            return;
        }
        data.active = id;
        data.lastMaintenance = System.currentTimeMillis();
        data.mixedBuild = false;
        PassiveTask.onSwitch(plugin, player, data);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
        Fx.spawn(player, org.bukkit.Particle.HAPPY_VILLAGER, 12, 0.4, 0.1);
        Text.msg(player, "&aBooted " + distro.displayName() + "&a.");
        build(player);
    }

    private void build(Player player) {
        slotToDistro.clear();
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);

        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack accent = pane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }
        for (int i : new int[]{0, 8, 45, 53, 4, 49}) {
            inventory.setItem(i, accent);
        }

        int idx = 0;
        for (Distro distro : plugin.getDistroManager().all().values()) {
            if (idx >= DISTRO_SLOTS.length) {
                break;
            }
            int slot = DISTRO_SLOTS[idx++];
            inventory.setItem(slot, distroItem(distro, data));
            slotToDistro.put(slot, distro.id());
        }

        inventory.setItem(46, setsItem(data));
        inventory.setItem(47, info(Material.GOLD_NUGGET, "&e&lPackages", "&f" + data.packages,
                "&7Soft currency - rerolls & repairs"));
        inventory.setItem(48, info(Material.EMERALD, "&b&lRepoTokens", "&f" + data.repoTokens,
                "&7Premium rolls: &f/distroroll premium"));
        inventory.setItem(50, info(Material.NAME_TAG, "&d&lDotfiles", "&f" + data.dotfiles,
                "&7Cosmetic collectibles. Pure flex."));
        inventory.setItem(51, info(Material.EXPERIENCE_BOTTLE, "&a&lSkill Level", "&f" + data.skillLevel,
                "&7Higher skill = safer skill checks"));

        Distro active = plugin.getDistroManager().get(data.active);
        inventory.setItem(49, active == null
                ? info(Material.BARRIER, "&7No active distro", "", "&7Roll one with &f/distroroll")
                : info(Material.LIME_STAINED_GLASS_PANE, "&aActive: " + active.displayName(),
                        "&7" + Abilities.of(active.ability()).name(), "&8Click a distro below to switch"));
    }

    private ItemStack distroItem(Distro distro, PlayerDistroData data) {
        boolean unlocked = data.unlocked.contains(distro.id());
        boolean tempBricked = data.isTempBricked(distro.id());
        boolean permaBricked = data.permaBricked.contains(distro.id());
        boolean active = distro.id().equals(data.active);

        Material material = tempBricked || permaBricked ? Material.BARRIER
                : unlocked ? Material.PAPER : Material.GRAY_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c(distro.rarity().color + "&l" + strip(distro.displayName())
                + (active ? " &r&a(active)" : "")));

        List<String> lore = new ArrayList<>();
        lore.add(distro.rarity().color + "" + distro.rarity().name());
        lore.add("");
        if (permaBricked) {
            lore.add("&4&lPERMA-BRICKED &7- needs a Recovery ISO");
        } else if (tempBricked) {
            lore.add("&c&lBRICKED &7- " + data.tempBrickSecondsLeft(distro.id()) + "s left");
        } else if (!unlocked) {
            lore.add("&8Not installed - roll to unlock");
        }
        if (unlocked || tempBricked || permaBricked) {
            Abilities.Info ab = Abilities.of(distro.ability());
            lore.add("&6Ability: &f" + ab.name());
            lore.add("&7 " + ab.desc());
            lore.add("");
            lore.add("&8Stability " + bar(distro.stat("stability")) + " &7" + distro.stat("stability"));
            lore.add("&8Custom    " + bar(distro.stat("customization")) + " &7" + distro.stat("customization"));
            lore.add("&8Perf      " + bar(distro.stat("performance")) + " &7" + distro.stat("performance"));
            lore.add("&8Curve     " + bar(distro.learningCurve()) + " &7" + distro.learningCurve());
            if (distro.highMaintenance()) {
                lore.add("&6! High-maintenance");
            }
        }
        lore.add("");
        lore.add("&7&o\"" + distro.lore() + "\"");
        for (String line : distro.description()) {
            lore.add("&8" + line);
        }
        if (unlocked && !active && !tempBricked && !permaBricked) {
            lore.add("&e> Click to activate");
        }
        meta.lore(lore.stream().map(Text::c).toList());

        if (unlocked) {
            meta.setCustomModelData(distro.customModelData());
        }
        if (active) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /** Collection set bonuses, showing progress and which are active. */
    private ItemStack setsItem(PlayerDistroData data) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c("&5&lSet Bonuses"));
        List<String> lore = new ArrayList<>();
        lore.add("&7Collect every distro in a set");
        lore.add("&7for a permanent passive buff.");
        lore.add("");
        for (Sets.Bonus b : Sets.ALL) {
            int owned = Sets.owned(b, data);
            int total = b.members().size();
            boolean done = owned >= total;
            lore.add((done ? "&a✔ " : "&8✗ ") + (done ? "&a" : "&7") + b.name()
                    + " &8(" + owned + "/" + total + ")");
            lore.add((done ? "&2" : "&8") + "   " + b.description());
        }
        meta.lore(lore.stream().map(Text::c).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /** 10-segment coloured bar for a 1-10 stat. */
    private String bar(int value) {
        int v = Math.max(0, Math.min(10, value));
        String color = v >= 8 ? "&a" : v >= 5 ? "&e" : "&c";
        return color + "|".repeat(v) + "&8" + "|".repeat(10 - v);
    }

    private String strip(String legacy) {
        return legacy.replaceAll("&[0-9a-fk-or]", "");
    }

    private ItemStack info(Material material, String name, String value, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c(name + (value.isEmpty() ? "" : " &8» " + value)));
        List<String> lore = new ArrayList<>();
        for (String l : loreLines) {
            if (!l.isEmpty()) lore.add(l);
        }
        meta.lore(lore.stream().map(Text::c).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.isBlank() ? Component.empty() : Text.c(name));
        item.setItemMeta(meta);
        return item;
    }
}
