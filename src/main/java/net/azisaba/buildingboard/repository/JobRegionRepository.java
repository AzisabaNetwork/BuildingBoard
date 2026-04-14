package net.azisaba.buildingboard.repository;

import net.azisaba.buildingboard.model.job.JobRegion;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Optional;

public interface JobRegionRepository {
    void insert(@NotNull JobRegion region) throws SQLException;
    @NotNull Optional<JobRegion> findByJobId(long jobId) throws SQLException;
    @NotNull Optional<JobRegion> findActiveContaining(@NotNull String worldName, int x, int y, int z) throws SQLException;
    boolean existsOverlappingRegion(@NotNull String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) throws SQLException;
}
