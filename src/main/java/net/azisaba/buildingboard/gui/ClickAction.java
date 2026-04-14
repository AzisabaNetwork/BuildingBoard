package net.azisaba.buildingboard.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ClickAction {
    void handle(@NotNull Player player, @NotNull ClickType clickType);
}
