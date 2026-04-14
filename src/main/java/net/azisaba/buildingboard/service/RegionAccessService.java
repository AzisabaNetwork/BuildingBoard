package net.azisaba.buildingboard.service;

import net.azisaba.buildingboard.model.job.Job;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public interface RegionAccessService {
    @NotNull Optional<Job> findProtectedJobAt(@NotNull Location location) throws SQLException;
    boolean canEdit(@NotNull OfflinePlayer player, @NotNull Block block) throws SQLException;
    boolean canUse(@NotNull OfflinePlayer player, @NotNull Block block) throws SQLException;
    boolean isProtected(@NotNull Block block) throws SQLException;
    boolean isSameProtectedJob(@NotNull Block first, @NotNull Block second) throws SQLException;
    boolean isAllowedEditor(@NotNull UUID playerUuid, @NotNull Job job) throws SQLException;
}
