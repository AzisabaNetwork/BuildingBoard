package net.azisaba.buildingboard.repository.impl;

import net.azisaba.buildingboard.model.job.JobRegion;
import net.azisaba.buildingboard.model.job.JobStatus;
import net.azisaba.buildingboard.repository.JobRegionRepository;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcJobRegionRepository extends JdbcRepositorySupport implements JobRegionRepository {
    public JdbcJobRegionRepository(final @NotNull DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void insert(final @NotNull JobRegion region) throws SQLException {
        final String sql = "INSERT INTO job_regions (job_id, world_name, min_x, min_y, min_z, max_x, max_y, max_z) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, region.getJobId());
            statement.setString(2, region.getWorldName());
            statement.setInt(3, region.getMinX());
            statement.setInt(4, region.getMinY());
            statement.setInt(5, region.getMinZ());
            statement.setInt(6, region.getMaxX());
            statement.setInt(7, region.getMaxY());
            statement.setInt(8, region.getMaxZ());
            statement.executeUpdate();
        }
    }

    @Override
    public @NotNull Optional<JobRegion> findByJobId(final long jobId) throws SQLException {
        final String sql = "SELECT * FROM job_regions WHERE job_id = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(this.map(resultSet));
            }
        }
    }

    @Override
    public @NotNull Optional<JobRegion> findActiveContaining(final @NotNull String worldName, final int x, final int y, final int z) throws SQLException {
        final String sql = "SELECT r.* FROM job_regions r INNER JOIN jobs j ON j.id = r.job_id"
                + " WHERE r.world_name = ? AND j.status IN (?, ?, ?)"
                + " AND r.min_x <= ? AND r.max_x >= ?"
                + " AND r.min_y <= ? AND r.max_y >= ?"
                + " AND r.min_z <= ? AND r.max_z >= ?"
                + " LIMIT 1";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            statement.setString(2, JobStatus.OPEN.name());
            statement.setString(3, JobStatus.IN_PROGRESS.name());
            statement.setString(4, JobStatus.WORK_DEADLINE_PASSED.name());
            statement.setInt(5, x);
            statement.setInt(6, x);
            statement.setInt(7, y);
            statement.setInt(8, y);
            statement.setInt(9, z);
            statement.setInt(10, z);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(this.map(resultSet));
            }
        }
    }

    @Override
    public boolean existsOverlappingRegion(final @NotNull String worldName, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM job_regions r INNER JOIN jobs j ON j.id = r.job_id"
                + " WHERE r.world_name = ? AND j.status IN (?, ?, ?)"
                + " AND NOT (r.max_x < ? OR r.min_x > ? OR r.max_y < ? OR r.min_y > ? OR r.max_z < ? OR r.min_z > ?)";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, worldName);
            statement.setString(2, JobStatus.OPEN.name());
            statement.setString(3, JobStatus.IN_PROGRESS.name());
            statement.setString(4, JobStatus.WORK_DEADLINE_PASSED.name());
            statement.setInt(5, minX);
            statement.setInt(6, maxX);
            statement.setInt(7, minY);
            statement.setInt(8, maxY);
            statement.setInt(9, minZ);
            statement.setInt(10, maxZ);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private @NotNull JobRegion map(final @NotNull ResultSet resultSet) throws SQLException {
        return new JobRegion(
                resultSet.getLong("job_id"),
                resultSet.getString("world_name"),
                resultSet.getInt("min_x"),
                resultSet.getInt("min_y"),
                resultSet.getInt("min_z"),
                resultSet.getInt("max_x"),
                resultSet.getInt("max_y"),
                resultSet.getInt("max_z")
        );
    }
}
