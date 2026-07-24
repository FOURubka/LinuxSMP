package com.rubka.linuxsmp;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Read-only page listing every fusion recipe: input A + input B = result. */
public class RecipesGUI implements InventoryHolder, LinuxGui {

    private final LinuxSMPPlugin plugin;
    private final Inventory inventory;

    public RecipesGUI(LinuxSMPPlugin plugin, Player player) {
        this.plugin = plugin;
        this.inventory = plugin.getServer().createInventory(this, 54,
                Text.mm("<gradient:#7f5af0:#2cb67d><bold>Fusion Recipes</bold></gradient>"));
        build(player);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    private void build(Player player) {
        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, border);
        }

        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        List<DistroManager.Fusion> fusions = plugin.getDistroManager().fusions();
        int row = 0;
        for (DistroManager.Fusion fusion : fusions) {
            if (row >= 6) {
                break;
            }
            Distro a = plugin.getDistroManager().get(fusion.inputA());
            Distro b = plugin.getDistroManager().get(fusion.inputB());
            Distro result = plugin.getDistroManager().get(fusion.result());
            if (a == null || b == null || result == null) {
                continue;
            }
            int base = row * 9;
            inventory.setItem(base + 1, inputToken(a, data));
            inventory.setItem(base + 2, glyph(Material.NETHER_STAR, "&7+"));
            inventory.setItem(base + 3, inputToken(b, data));
            inventory.setItem(base + 4, glyph(Material.ARROW, "&7="));
            inventory.setItem(base + 5, resultToken(result, data));
            row++;
        }

        if (row == 0) {
            inventory.setItem(22, glyph(Material.BARRIER, "&cNo fusion recipes configured."));
        }
    }

    private ItemStack inputToken(Distro distro, PlayerDistroData data) {
        ItemStack item = plugin.getItemFactory().token(distro);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Text.c(distro.rarity().color + distro.rarity().name()));
        lore.add(Text.c(data.unlocked.contains(distro.id())
                ? "&aYou own this" : "&8Not owned yet"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack resultToken(Distro distro, PlayerDistroData data) {
        int pct = 100 - (int) Math.round(plugin.getSkillCheckService()
                .failChance(data, distro, SkillCheckService.CheckType.FUSION) * 100);
        ItemStack item = plugin.getItemFactory().token(distro);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(Text.c(distro.rarity().color + distro.rarity().name()));
        lore.add(Text.c("&6Ability: &f" + Abilities.of(distro.ability()).name()));
        lore.add(Text.c("&7Your success chance: " + pctColor(pct) + pct + "%"));
        lore.add(Text.c("&8On failure one input token is lost."));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String pctColor(int pct) {
        return pct >= 75 ? "&a" : pct >= 45 ? "&e" : "&c";
    }

    private ItemStack glyph(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c(name));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }
}
