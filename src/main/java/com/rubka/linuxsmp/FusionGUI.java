package com.rubka.linuxsmp;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Drag two distro tokens in, preview the result and success chance, then fuse. */
public class FusionGUI implements InventoryHolder, LinuxGui {

    static final int INPUT_A = 11;
    static final int INPUT_B = 15;
    private static final int RESULT = 13;
    private static final int CONFIRM = 22;
    private static final int RECIPES = 26;

    private final LinuxSMPPlugin plugin;
    private final Inventory inventory;

    public FusionGUI(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
        this.inventory = plugin.getServer().createInventory(this, 27,
                Text.mm("<gradient:#7f5af0:#2cb67d><bold>Distro Fusion</bold></gradient>"));
        build();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    private void build() {
        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, border);
        }
        inventory.setItem(INPUT_A, null);
        inventory.setItem(INPUT_B, null);
        inventory.setItem(10, hint(Material.LIME_STAINED_GLASS_PANE, "&aInput A", "&7Click a token to place it"));
        inventory.setItem(16, hint(Material.LIME_STAINED_GLASS_PANE, "&aInput B", "&7Click a token to place it"));
        inventory.setItem(RECIPES, hint(Material.KNOWLEDGE_BOOK, "&e&lFusion recipes", "&7See which fusions exist"));
        refresh();
    }

    /** Handles all clicks (the event is already cancelled by GuiListener). */
    public void handleClick(Player player, InventoryClickEvent event) {
        int raw = event.getRawSlot();
        boolean inThis = event.getClickedInventory() == inventory;

        if (inThis) {
            if (raw == INPUT_A || raw == INPUT_B) {
                returnSlot(player, raw);
                refresh();
            } else if (raw == CONFIRM) {
                fuse(player);
            } else if (raw == RECIPES) {
                new FusionTreeGUI(plugin).open(player);
            }
            return;
        }

        // Clicked in the player inventory: move one token into a free input slot.
        ItemStack clicked = event.getCurrentItem();
        if (plugin.getItemFactory().tokenDistroId(clicked) == null) {
            return;
        }
        int target = inventory.getItem(INPUT_A) == null ? INPUT_A
                : inventory.getItem(INPUT_B) == null ? INPUT_B : -1;
        if (target == -1) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f);
            return;
        }
        ItemStack one = clicked.clone();
        one.setAmount(1);
        inventory.setItem(target, one);
        clicked.setAmount(clicked.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        refresh();
    }

    /** On close, hand any staged input tokens back to the player. */
    public void returnInputs(Player player) {
        for (int slot : new int[]{INPUT_A, INPUT_B}) {
            ItemStack item = inventory.getItem(slot);
            if (item != null) {
                inventory.setItem(slot, null);
                giveBack(player, item);
            }
        }
    }

    private void returnSlot(Player player, int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) {
            return;
        }
        inventory.setItem(slot, null);
        giveBack(player, item);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
    }

    private void fuse(Player player) {
        String idA = plugin.getItemFactory().tokenDistroId(inventory.getItem(INPUT_A));
        String idB = plugin.getItemFactory().tokenDistroId(inventory.getItem(INPUT_B));
        Distro result = findResult(idA, idB);
        if (result == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            Text.msg(player, "&cThose two tokens do not fuse into anything.");
            return;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        SkillCheckService checks = plugin.getSkillCheckService();
        // Consume both inputs regardless of outcome.
        inventory.setItem(INPUT_A, null);
        inventory.setItem(INPUT_B, null);

        if (checks.check(data, result, SkillCheckService.CheckType.FUSION)) {
            giveBack(player, plugin.getItemFactory().token(result));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.4f);
            Fx.burst(player, result.rarity());
            Text.msg(player, "&aFusion successful: " + result.displayName() + " &fToken&a!");
        } else {
            // One input survives, the other is lost.
            giveBack(player, plugin.getItemFactory().token(plugin.getDistroManager().get(idB)));
            checks.kernelPanic(player, "&cFusion failed - the " + plugin.getDistroManager().get(idA).displayName()
                    + " &ctoken was destroyed.");
        }
        refresh();
    }

    /** Updates the centre preview slot from the current inputs. */
    private void refresh() {
        String idA = plugin.getItemFactory().tokenDistroId(inventory.getItem(INPUT_A));
        String idB = plugin.getItemFactory().tokenDistroId(inventory.getItem(INPUT_B));

        if (idA == null || idB == null) {
            inventory.setItem(RESULT, info(Material.GRAY_DYE, "&7Waiting for two tokens",
                    List.of("&8Place a token in each slot")));
            inventory.setItem(CONFIRM, info(Material.BARRIER, "&8Fuse", List.of("&7Not ready")));
            return;
        }
        Distro result = findResult(idA, idB);
        if (result == null) {
            inventory.setItem(RESULT, info(Material.BARRIER, "&cNo recipe",
                    List.of("&7These two do not fuse.")));
            inventory.setItem(CONFIRM, info(Material.BARRIER, "&8Fuse", List.of("&7No recipe")));
            return;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(getViewer());
        int pct = 100 - (int) Math.round(plugin.getSkillCheckService()
                .failChance(data, result, SkillCheckService.CheckType.FUSION) * 100);

        ItemStack preview = plugin.getItemFactory().token(result);
        ItemMeta meta = preview.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Result: " + result.rarity().color + result.displayName());
        lore.add("&7Success chance: " + pctColor(pct) + pct + "%");
        lore.add("&8On failure one token is lost.");
        meta.lore(lore.stream().map(Text::c).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        preview.setItemMeta(meta);
        inventory.setItem(RESULT, preview);

        inventory.setItem(CONFIRM, info(Material.LIME_STAINED_GLASS_PANE, "&a&lFUSE",
                List.of("&7Click to attempt", "&7Success: " + pctColor(pct) + pct + "%")));
    }

    private Distro findResult(String idA, String idB) {
        if (idA == null || idB == null) {
            return null;
        }
        for (DistroManager.Fusion f : plugin.getDistroManager().fusions()) {
            if ((f.inputA().equals(idA) && f.inputB().equals(idB))
                    || (f.inputA().equals(idB) && f.inputB().equals(idA))) {
                return plugin.getDistroManager().get(f.result());
            }
        }
        return null;
    }

    private Player getViewer() {
        return (Player) inventory.getViewers().get(0);
    }

    private void giveBack(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    private String pctColor(int pct) {
        return pct >= 75 ? "&a" : pct >= 45 ? "&e" : "&c";
    }

    private ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack hint(Material material, String name, String... lore) {
        return info(material, name, List.of(lore));
    }

    private ItemStack info(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c(name));
        meta.lore(loreLines.stream().map(Text::c).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
