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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * Visual fusion map: clickable distro nodes on the left; selecting one highlights
 * what it is made from and what it fuses into. Data-driven from the recipe list.
 */
public class FusionTreeGUI implements InventoryHolder, LinuxGui {

    private static final int BACK = 8;
    private static final int[] NODE_SLOTS = {0, 9, 18, 27, 36, 45, 1, 10, 19, 28, 37, 46};
    // Recipe rows in the detail area, each laid out A + B = R across cols 3..7.
    private static final int[] MADE_ROWS = {9, 18};
    private static final int[] INTO_ROWS = {36, 45};

    private final LinuxSMPPlugin plugin;
    private final Inventory inventory;
    private final Map<Integer, String> clickable = new HashMap<>();
    private String selected;

    public FusionTreeGUI(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
        this.inventory = plugin.getServer().createInventory(this, 54,
                Text.mm("<gradient:#7f5af0:#2cb67d><bold>Fusion Map</bold></gradient>"));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        build(player);
        player.openInventory(inventory);
    }

    public void handleClick(Player player, int rawSlot) {
        if (rawSlot == BACK) {
            new FusionGUI(plugin).open(player);
            return;
        }
        String id = clickable.get(rawSlot);
        if (id != null && !id.equals(selected)) {
            selected = id;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
            build(player);
        }
    }

    private void build(Player player) {
        clickable.clear();
        ItemStack border = pane();
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }
        inventory.setItem(BACK, glyph(Material.ARROW, "&e< Back to fusion"));

        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        List<DistroManager.Fusion> fusions = plugin.getDistroManager().fusions();

        // Nodes: every distro that appears in any recipe, stable insertion order.
        Set<String> nodes = new LinkedHashSet<>();
        for (DistroManager.Fusion f : fusions) {
            nodes.add(f.inputA());
            nodes.add(f.inputB());
            nodes.add(f.result());
        }
        int i = 0;
        for (String id : nodes) {
            if (i >= NODE_SLOTS.length) {
                break;
            }
            Distro d = plugin.getDistroManager().get(id);
            if (d == null) {
                continue;
            }
            int slot = NODE_SLOTS[i++];
            inventory.setItem(slot, node(d, data, id.equals(selected)));
            clickable.put(slot, id);
        }

        if (selected == null) {
            inventory.setItem(22, glyph(Material.KNOWLEDGE_BOOK,
                    "&e&lClick a distro node", "&7See what it is made from", "&7and what it fuses into."));
            return;
        }

        inventory.setItem(3, glyph(Material.CRAFTING_TABLE, "&aMade from"));
        int madeRow = 0;
        for (DistroManager.Fusion f : fusions) {
            if (!f.result().equals(selected) || madeRow >= MADE_ROWS.length) {
                continue;
            }
            recipeRow(data, MADE_ROWS[madeRow++], f);
        }
        if (madeRow == 0) {
            inventory.setItem(MADE_ROWS[0] + 3, glyph(Material.BARRIER, "&8Base distro - not fused."));
        }

        inventory.setItem(30, glyph(Material.FURNACE, "&bFuses into"));
        int intoRow = 0;
        for (DistroManager.Fusion f : fusions) {
            boolean isInput = f.inputA().equals(selected) || f.inputB().equals(selected);
            if (!isInput || intoRow >= INTO_ROWS.length) {
                continue;
            }
            recipeRow(data, INTO_ROWS[intoRow++], f);
        }
        if (intoRow == 0) {
            inventory.setItem(INTO_ROWS[0] + 3, glyph(Material.BARRIER, "&8Does not fuse into anything."));
        }
    }

    /** Lays A [+] B [=] R across cols 3..7 of the given row, glinting the selected node. */
    private void recipeRow(PlayerDistroData data, int base, DistroManager.Fusion f) {
        Distro a = plugin.getDistroManager().get(f.inputA());
        Distro b = plugin.getDistroManager().get(f.inputB());
        Distro r = plugin.getDistroManager().get(f.result());
        if (a == null || b == null || r == null) {
            return;
        }
        inventory.setItem(base + 3, node(a, data, a.id().equals(selected)));
        inventory.setItem(base + 4, glyph(Material.NETHER_STAR, "&7+"));
        inventory.setItem(base + 5, node(b, data, b.id().equals(selected)));
        inventory.setItem(base + 6, glyph(Material.ARROW, "&7="));
        inventory.setItem(base + 7, result(r, data, r.id().equals(selected)));
        clickable.put(base + 3, a.id());
        clickable.put(base + 5, b.id());
        clickable.put(base + 7, r.id());
    }

    private ItemStack node(Distro distro, PlayerDistroData data, boolean glow) {
        ItemStack item = plugin.getItemFactory().token(distro);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Text.c(distro.rarity().color + distro.rarity().name()));
        lore.add(Text.c(data.unlocked.contains(distro.id()) ? "&aYou own this" : "&8Not owned yet"));
        lore.add(Text.c("&8Click to focus"));
        meta.lore(lore);
        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack result(Distro distro, PlayerDistroData data, boolean glow) {
        int pct = 100 - (int) Math.round(plugin.getSkillCheckService()
                .failChance(data, distro, SkillCheckService.CheckType.FUSION) * 100);
        ItemStack item = plugin.getItemFactory().token(distro);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Text.c(distro.rarity().color + distro.rarity().name()));
        lore.add(Text.c("&6Ability: &f" + Abilities.of(distro.ability()).name()));
        lore.add(Text.c("&7Your success: " + pctColor(pct) + pct + "%"));
        lore.add(Text.c("&8On failure one input token is lost."));
        meta.lore(lore);
        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private String pctColor(int pct) {
        return pct >= 75 ? "&a" : pct >= 45 ? "&e" : "&c";
    }

    private ItemStack glyph(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c(name));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) {
                lines.add(Text.c(l));
            }
            meta.lore(lines);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }
}
