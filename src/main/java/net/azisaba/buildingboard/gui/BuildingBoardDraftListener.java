package net.azisaba.buildingboard.gui;

import net.azisaba.buildingboard.BuildingBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public final class BuildingBoardDraftListener implements Listener {
    private final @NotNull BuildingBoard plugin;

    public BuildingBoardDraftListener(final @NotNull BuildingBoard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAsyncChat(final @NotNull AsyncPlayerChatEvent event) {
        if (!this.plugin.getGuiService().hasPendingDraftInput(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        final Player player = event.getPlayer();
        final String message = event.getMessage();
        Bukkit.getScheduler().runTask(this.plugin, () -> this.plugin.getGuiService().handleDraftChatInput(player, message));
    }

    @EventHandler
    public void onQuit(final @NotNull PlayerQuitEvent event) {
        this.plugin.getGuiService().clearDraftState(event.getPlayer().getUniqueId());
    }
}
