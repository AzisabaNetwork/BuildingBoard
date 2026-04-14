package net.azisaba.buildingboard.util;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import net.azisaba.buildingboard.model.job.JobRegion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WorldEditSelectionUtil {
    private WorldEditSelectionUtil() {
    }

    public static @NotNull JobRegion requireSelectedCuboid(
            final @NotNull WorldEditPlugin worldEditPlugin,
            final @NotNull Player player
    ) throws IncompleteRegionException {
        final Region region = worldEditPlugin.getSession(player).getSelection(BukkitAdapter.adapt(player.getWorld()));
        if (!(region instanceof CuboidRegion)) {
            throw new IllegalStateException("選択範囲は直方体である必要があります。");
        }
        return new JobRegion(
                0L,
                player.getWorld().getName(),
                region.getMinimumPoint().getBlockX(),
                region.getMinimumPoint().getBlockY(),
                region.getMinimumPoint().getBlockZ(),
                region.getMaximumPoint().getBlockX(),
                region.getMaximumPoint().getBlockY(),
                region.getMaximumPoint().getBlockZ()
        );
    }
}
