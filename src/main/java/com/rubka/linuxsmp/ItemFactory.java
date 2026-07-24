package com.rubka.linuxsmp;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ItemFactory {

    private final LinuxSMPPlugin plugin;
    private final NamespacedKey tokenKey;
    private final NamespacedKey isoKey;

    public ItemFactory(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
        this.tokenKey = new NamespacedKey(plugin, "distro_token");
        this.isoKey = new NamespacedKey(plugin, "recovery_iso");
    }

    public ItemStack token(Distro distro) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c(distro.displayName() + " &fToken"));
        List<String> lore = new java.util.ArrayList<>();
        lore.add(distro.rarity().color + distro.rarity().name());
        lore.add("&6Ability: &f" + Abilities.of(distro.ability()).name());
        lore.add("&7Right-click to install");
        lore.add("");
        lore.add("&7&o\"" + distro.lore() + "\"");
        for (String line : distro.description()) {
            lore.add("&8" + line);
        }
        meta.lore(lore.stream().map(Text::c).toList());
        meta.setCustomModelData(distro.customModelData());
        meta.getPersistentDataContainer().set(tokenKey, PersistentDataType.STRING, distro.id());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack recoveryIso() {
        ItemStack item = new ItemStack(Material.MUSIC_DISC_11);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Text.c("&b&lRecovery ISO"));
        meta.lore(List.of(
                Text.c("&7Restores a perma-bricked distro."),
                Text.c("&7Use with &f/distrorepair <distro>")
        ));
        meta.setCustomModelData(plugin.getConfig().getInt("custom-model-data.recovery-iso", 1010));
        meta.getPersistentDataContainer().set(isoKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** Returns the distro id stored on a token item, or null if not a token. */
    public String tokenDistroId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(tokenKey, PersistentDataType.STRING);
    }

    public boolean isRecoveryIso(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(isoKey, PersistentDataType.BYTE);
    }
}
