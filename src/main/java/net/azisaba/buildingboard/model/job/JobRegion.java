package net.azisaba.buildingboard.model.job;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class JobRegion {
    private final long jobId;
    private final @NotNull String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public JobRegion(
            final long jobId,
            final @NotNull String worldName,
            final int minX,
            final int minY,
            final int minZ,
            final int maxX,
            final int maxY,
            final int maxZ
    ) {
        this.jobId = jobId;
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public long getJobId() { return this.jobId; }
    public @NotNull String getWorldName() { return this.worldName; }
    public int getMinX() { return this.minX; }
    public int getMinY() { return this.minY; }
    public int getMinZ() { return this.minZ; }
    public int getMaxX() { return this.maxX; }
    public int getMaxY() { return this.maxY; }
    public int getMaxZ() { return this.maxZ; }
}
