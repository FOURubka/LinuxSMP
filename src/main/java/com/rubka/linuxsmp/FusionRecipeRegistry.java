package com.rubka.linuxsmp;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public class FusionRecipeRegistry implements Listener {

    private static final String KEY_PREFIX = "fusion_";

    private final LinuxSMPPlugin plugin;
    private final List<NamespacedKey> registered = new ArrayList<>();

    public FusionRecipeRegistry(LinuxSMPPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        unregister();
        DistroManager distros = plugin.getDistroManager();
        int index = 0;
        for (DistroManager.Fusion fusion : distros.fusions()) {
            Distro a = distros.get(fusion.inputA());
            Distro b = distros.get(fusion.inputB());
            Distro result = distros.get(fusion.result());
            NamespacedKey key = new NamespacedKey(plugin, KEY_PREFIX + fusion.result() + "_" + index++);
            ShapedRecipe recipe = new ShapedRecipe(key, plugin.getItemFactory().token(result));
            recipe.shape("AB");
            recipe.setIngredient('A', new RecipeChoice.ExactChoice(plugin.getItemFactory().token(a)));
            recipe.setIngredient('B', new RecipeChoice.ExactChoice(plugin.getItemFactory().token(b)));
            plugin.getServer().addRecipe(recipe);
            registered.add(key);
        }
        plugin.getLogger().info("Registered " + registered.size() + " fusion recipes.");
    }

    public void unregister() {
        for (NamespacedKey key : registered) {
            plugin.getServer().removeRecipe(key);
        }
        registered.clear();
    }

    /** Fusion crafting runs a skill check - on fail, one input token is destroyed. */
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getRecipe() instanceof Keyed keyed)) {
            return;
        }
        NamespacedKey key = keyed.getKey();
        if (!key.getNamespace().equals(plugin.getName().toLowerCase())
                || !key.getKey().startsWith(KEY_PREFIX)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String resultId = plugin.getItemFactory().tokenDistroId(event.getRecipe().getResult());
        Distro result = plugin.getDistroManager().get(resultId);
        if (result == null) {
            return;
        }
        PlayerDistroData data = plugin.getPlayerDataManager().get(player);
        SkillCheckService checks = plugin.getSkillCheckService();
        if (checks.check(data, result, SkillCheckService.CheckType.FUSION)) {
            player.sendMessage(Text.c("&aFusion successful: " + result.displayName() + " &fToken&a!"));
            return;
        }
        // Failed fusion: no result, one input token burns.
        event.setCancelled(true);
        ItemStack[] matrix = event.getInventory().getMatrix();
        for (ItemStack item : matrix) {
            if (plugin.getItemFactory().tokenDistroId(item) != null) {
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }
        checks.kernelPanic(player, "&cFusion failed - an input token was destroyed.");
    }
}
