package net.azisaba.buildingboard.listener;

import net.azisaba.buildingboard.BuildingBoard;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class PlayerSessionListener implements Listener {
    private final @NotNull BuildingBoard plugin;

    public PlayerSessionListener(final @NotNull BuildingBoard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final @NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        try {
            this.plugin.getRefundService().processPendingRefunds(player);
            final int unread = this.plugin.getNotificationService().countUnread(player.getUniqueId());
            if (unread > 0) {
                player.sendMessage(ChatColor.GOLD + "未読のBuildingBoard通知が " + unread + " 件あります。/bb notifications で確認できます。");
            }
        } catch (SQLException | IllegalStateException e) {
            this.plugin.getLogger().warning("Failed to process join actions for " + player.getName() + ": " + e.getMessage());
        }
    }
}
