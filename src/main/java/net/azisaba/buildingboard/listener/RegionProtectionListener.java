package net.azisaba.buildingboard.listener;

import net.azisaba.buildingboard.BuildingBoard;
import net.azisaba.buildingboard.service.RegionAccessService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class RegionProtectionListener implements Listener {
    private final @NotNull BuildingBoard plugin;
    private final @NotNull RegionAccessService regionAccessService;

    public RegionProtectionListener(final @NotNull BuildingBoard plugin, final @NotNull RegionAccessService regionAccessService) {
        this.plugin = plugin;
        this.regionAccessService = regionAccessService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBlockPlace(final @NotNull BlockPlaceEvent event) {
        this.handlePlayerBlockEdit(event.getPlayer(), event.getBlockPlaced(), event::setCancelled);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBlockBreak(final @NotNull BlockBreakEvent event) {
        this.handlePlayerBlockEdit(event.getPlayer(), event.getBlock(), event::setCancelled);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBucketEmpty(final @NotNull PlayerBucketEmptyEvent event) {
        final Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        this.handlePlayerBlockEdit(event.getPlayer(), target, event::setCancelled);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBucketFill(final @NotNull PlayerBucketFillEvent event) {
        this.handlePlayerBlockEdit(event.getPlayer(), event.getBlockClicked(), event::setCancelled);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerInteract(final @NotNull PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (!event.getClickedBlock().getType().isInteractable()) {
            return;
        }
        try {
            if (!this.regionAccessService.canUse(event.getPlayer(), event.getClickedBlock())) {
                event.setCancelled(true);
                this.sendDenied(event.getPlayer());
            }
        } catch (SQLException e) {
            this.logSqlError("interact", e);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPistonExtend(final @NotNull BlockPistonExtendEvent event) {
        try {
            for (Block movedBlock : event.getBlocks()) {
                final Block destination = movedBlock.getRelative(event.getDirection());
                if (this.regionAccessService.isProtected(movedBlock) || this.regionAccessService.isProtected(destination)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } catch (SQLException e) {
            this.logSqlError("piston extend", e);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPistonRetract(final @NotNull BlockPistonRetractEvent event) {
        try {
            for (Block movedBlock : event.getBlocks()) {
                if (this.regionAccessService.isProtected(movedBlock) || this.regionAccessService.isProtected(event.getBlock())) {
                    event.setCancelled(true);
                    return;
                }
            }
        } catch (SQLException e) {
            this.logSqlError("piston retract", e);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onLiquidFlow(final @NotNull BlockFromToEvent event) {
        final Material type = event.getBlock().getType();
        if (type != Material.WATER && type != Material.LAVA) {
            return;
        }
        try {
            final boolean sourceProtected = this.regionAccessService.isProtected(event.getBlock());
            final boolean targetProtected = this.regionAccessService.isProtected(event.getToBlock());
            if (sourceProtected && !targetProtected) {
                event.setCancelled(true);
                return;
            }
            if (sourceProtected && targetProtected && !this.regionAccessService.isSameProtectedJob(event.getBlock(), event.getToBlock())) {
                event.setCancelled(true);
            }
        } catch (SQLException e) {
            this.logSqlError("liquid flow", e);
        }
    }

    private void handlePlayerBlockEdit(final @NotNull Player player, final @NotNull Block block, final @NotNull CancelHandler cancelHandler) {
        try {
            if (!this.regionAccessService.canEdit(player, block)) {
                cancelHandler.cancel(true);
                this.sendDenied(player);
            }
        } catch (SQLException e) {
            this.logSqlError("block edit", e);
        }
    }

    private void sendDenied(final @NotNull Player player) {
        player.sendMessage(ChatColor.RED + "この保護された建築範囲では操作できません。");
    }

    private void logSqlError(final @NotNull String action, final @NotNull SQLException e) {
        this.plugin.getLogger().warning("Failed to evaluate region protection for " + action + ": " + e.getMessage());
    }

    @FunctionalInterface
    private interface CancelHandler {
        void cancel(boolean cancelled);
    }
}
