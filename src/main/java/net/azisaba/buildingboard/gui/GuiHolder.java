package net.azisaba.buildingboard.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class GuiHolder implements InventoryHolder {
    private final @NotNull Inventory inventory;
    private final @NotNull Map<Integer, ClickAction> actions = new HashMap<>();

    public GuiHolder(final int size, final @NotNull String title) {
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public void setAction(final int slot, final @NotNull ClickAction action) {
        this.actions.put(slot, action);
    }

    public @Nullable ClickAction getAction(final int slot) {
        return this.actions.get(slot);
    }
}
