package net.azisaba.buildingboard.repository.impl;

import net.azisaba.buildingboard.model.notification.JobNotification;
import net.azisaba.buildingboard.model.notification.NotificationCategory;
import net.azisaba.buildingboard.repository.JobNotificationRepository;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcJobNotificationRepository extends JdbcRepositorySupport implements JobNotificationRepository {
    public JdbcJobNotificationRepository(final @NotNull DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public @NotNull JobNotification insert(final @NotNull JobNotification notification) throws SQLException {
        final String sql = "INSERT INTO job_notifications (player_uuid, category, job_id, title, body, is_read, created_at, read_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, notification.getPlayerUuid().toString());
            statement.setString(2, notification.getCategory().name());
            this.setNullableLong(statement, 3, notification.getJobId());
            statement.setString(4, notification.getTitle());
            statement.setString(5, notification.getBody());
            statement.setBoolean(6, notification.isRead());
            statement.setLong(7, notification.getCreatedAt());
            this.setNullableLong(statement, 8, notification.getReadAt());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to obtain generated notification id");
                }
                return new JobNotification(
                        keys.getLong(1),
                        notification.getPlayerUuid(),
                        notification.getCategory(),
                        notification.getJobId(),
                        notification.getTitle(),
                        notification.getBody(),
                        notification.isRead(),
                        notification.getCreatedAt(),
                        notification.getReadAt()
                );
            }
        }
    }

    @Override
    public @NotNull List<JobNotification> findByPlayerUuid(final @NotNull UUID playerUuid, final boolean unreadOnly, final int limit, final int offset) throws SQLException {
        final String sql = unreadOnly
                ? "SELECT * FROM job_notifications WHERE player_uuid = ? AND is_read = FALSE ORDER BY created_at DESC LIMIT ? OFFSET ?"
                : "SELECT * FROM job_notifications WHERE player_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<JobNotification> notifications = new ArrayList<>();
                while (resultSet.next()) {
                    notifications.add(this.map(resultSet));
                }
                return notifications;
            }
        }
    }

    @Override
    public int countUnread(final @NotNull UUID playerUuid) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM job_notifications WHERE player_uuid = ? AND is_read = FALSE";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    @Override
    public void markAsRead(final long notificationId, final long readAt) throws SQLException {
        final String sql = "UPDATE job_notifications SET is_read = TRUE, read_at = ? WHERE id = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, readAt);
            statement.setLong(2, notificationId);
            statement.executeUpdate();
        }
    }

    @Override
    public void markAllAsRead(final @NotNull UUID playerUuid, final long readAt) throws SQLException {
        final String sql = "UPDATE job_notifications SET is_read = TRUE, read_at = ? WHERE player_uuid = ? AND is_read = FALSE";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, readAt);
            statement.setString(2, playerUuid.toString());
            statement.executeUpdate();
        }
    }

    private @NotNull JobNotification map(final @NotNull ResultSet resultSet) throws SQLException {
        return new JobNotification(
                resultSet.getLong("id"),
                this.getUuid(resultSet, "player_uuid"),
                NotificationCategory.valueOf(resultSet.getString("category")),
                this.getNullableLong(resultSet, "job_id"),
                resultSet.getString("title"),
                resultSet.getString("body"),
                resultSet.getBoolean("is_read"),
                resultSet.getLong("created_at"),
                this.getNullableLong(resultSet, "read_at")
        );
    }
}
