package net.azisaba.buildingboard.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ItemStackFactory {
    private ItemStackFactory() {
    }

    public static @NotNull ItemStack create(final @NotNull Material material, final @NotNull String name, final @NotNull List<String> lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.AQUA + name);
        final List<String> formattedLore = new ArrayList<>();
        for (String line : lore) {
            formattedLore.add(ChatColor.GRAY + line);
        }
        meta.setLore(formattedLore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
