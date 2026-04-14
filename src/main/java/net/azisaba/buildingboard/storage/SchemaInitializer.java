package net.azisaba.buildingboard.storage;

import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaInitializer {
    private final @NotNull DataSource dataSource;

    public SchemaInitializer(final @NotNull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() throws SQLException {
        try (Connection connection = this.dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS jobs ("
                            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                            + "requester_uuid VARCHAR(36) NOT NULL,"
                            + "requester_name VARCHAR(16) NOT NULL,"
                            + "title VARCHAR(64) NOT NULL,"
                            + "description TEXT NOT NULL,"
                            + "status VARCHAR(32) NOT NULL,"
                            + "total_reward BIGINT NOT NULL,"
                            + "recruitment_deadline_at BIGINT NOT NULL,"
                            + "work_deadline_at BIGINT NOT NULL,"
                            + "force_complete_at BIGINT NOT NULL,"
                            + "created_at BIGINT NOT NULL,"
                            + "updated_at BIGINT NOT NULL,"
                            + "completed_at BIGINT NULL,"
                            + "cancelled_at BIGINT NULL,"
                            + "completed_by_force BOOLEAN NOT NULL DEFAULT FALSE"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS job_regions ("
                            + "job_id BIGINT PRIMARY KEY,"
                            + "world_name VARCHAR(64) NOT NULL,"
                            + "min_x INT NOT NULL,"
                            + "min_y INT NOT NULL,"
                            + "min_z INT NOT NULL,"
                            + "max_x INT NOT NULL,"
                            + "max_y INT NOT NULL,"
                            + "max_z INT NOT NULL,"
                            + "CONSTRAINT fk_job_regions_job_id "
                            + "FOREIGN KEY (job_id) REFERENCES jobs(id) "
                            + "ON DELETE CASCADE"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS job_members ("
                            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                            + "job_id BIGINT NOT NULL,"
                            + "player_uuid VARCHAR(36) NOT NULL,"
                            + "player_name VARCHAR(16) NOT NULL,"
                            + "role_status VARCHAR(32) NOT NULL,"
                            + "joined_at BIGINT NOT NULL,"
                            + "decided_at BIGINT NULL,"
                            + "left_at BIGINT NULL,"
                            + "decided_by_uuid VARCHAR(36) NULL,"
                            + "note VARCHAR(255) NULL,"
                            + "UNIQUE KEY uq_job_members_job_player (job_id, player_uuid),"
                            + "CONSTRAINT fk_job_members_job_id "
                            + "FOREIGN KEY (job_id) REFERENCES jobs(id) "
                            + "ON DELETE CASCADE"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS job_notifications ("
                            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                            + "player_uuid VARCHAR(36) NOT NULL,"
                            + "category VARCHAR(32) NOT NULL,"
                            + "job_id BIGINT NULL,"
                            + "title VARCHAR(64) NOT NULL,"
                            + "body VARCHAR(255) NOT NULL,"
                            + "is_read BOOLEAN NOT NULL DEFAULT FALSE,"
                            + "created_at BIGINT NOT NULL,"
                            + "read_at BIGINT NULL"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS job_refunds ("
                            + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                            + "job_id BIGINT NOT NULL,"
                            + "requester_uuid VARCHAR(36) NOT NULL,"
                            + "amount BIGINT NOT NULL,"
                            + "reason VARCHAR(32) NOT NULL,"
                            + "status VARCHAR(32) NOT NULL,"
                            + "created_at BIGINT NOT NULL,"
                            + "processed_at BIGINT NULL,"
                            + "failure_reason VARCHAR(255) NULL,"
                            + "CONSTRAINT fk_job_refunds_job_id "
                            + "FOREIGN KEY (job_id) REFERENCES jobs(id) "
                            + "ON DELETE CASCADE"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            this.createIndexIfMissing(connection, "jobs", "idx_jobs_requester", "CREATE INDEX idx_jobs_requester ON jobs(requester_uuid)");
            this.createIndexIfMissing(connection, "jobs", "idx_jobs_status", "CREATE INDEX idx_jobs_status ON jobs(status)");
            this.createIndexIfMissing(connection, "jobs", "idx_jobs_force_complete", "CREATE INDEX idx_jobs_force_complete ON jobs(force_complete_at)");
            this.createIndexIfMissing(connection, "job_members", "idx_job_members_job", "CREATE INDEX idx_job_members_job ON job_members(job_id)");
            this.createIndexIfMissing(connection, "job_members", "idx_job_members_player", "CREATE INDEX idx_job_members_player ON job_members(player_uuid)");
            this.createIndexIfMissing(connection, "job_members", "idx_job_members_status", "CREATE INDEX idx_job_members_status ON job_members(role_status)");
            this.createIndexIfMissing(connection, "job_notifications", "idx_job_notifications_player_read", "CREATE INDEX idx_job_notifications_player_read ON job_notifications(player_uuid, is_read)");
            this.createIndexIfMissing(connection, "job_refunds", "idx_job_refunds_requester_status", "CREATE INDEX idx_job_refunds_requester_status ON job_refunds(requester_uuid, status)");
            this.createIndexIfMissing(connection, "job_regions", "idx_job_regions_world", "CREATE INDEX idx_job_regions_world ON job_regions(world_name)");
        }
    }

    private void createIndexIfMissing(
            final @NotNull Connection connection,
            final @NotNull String tableName,
            final @NotNull String indexName,
            final @NotNull String sql
    ) throws SQLException {
        final DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (resultSet.next()) {
                final String existingIndexName = resultSet.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(existingIndexName)) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
