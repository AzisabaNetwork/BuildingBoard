package net.azisaba.buildingboard.service.impl;

import net.azisaba.buildingboard.model.notification.JobNotification;
import net.azisaba.buildingboard.model.notification.NotificationCategory;
import net.azisaba.buildingboard.repository.JobNotificationRepository;
import net.azisaba.buildingboard.service.NotificationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public final class SimpleNotificationService implements NotificationService {
    private final @NotNull JobNotificationRepository repository;

    public SimpleNotificationService(final @NotNull JobNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public @NotNull JobNotification notify(final @NotNull UUID playerUuid, final @NotNull NotificationCategory category, final @Nullable Long jobId, final @NotNull String title, final @NotNull String body) throws SQLException {
        return this.repository.insert(new JobNotification(0L, playerUuid, category, jobId, title, body, false, System.currentTimeMillis(), null));
    }

    @Override
    public @NotNull List<JobNotification> getNotifications(final @NotNull UUID playerUuid, final boolean unreadOnly, final int limit, final int offset) throws SQLException {
        return this.repository.findByPlayerUuid(playerUuid, unreadOnly, limit, offset);
    }

    @Override
    public int countUnread(final @NotNull UUID playerUuid) throws SQLException {
        return this.repository.countUnread(playerUuid);
    }

    @Override
    public void markAsRead(final long notificationId, final long readAt) throws SQLException {
        this.repository.markAsRead(notificationId, readAt);
    }

    @Override
    public void markAllAsRead(final @NotNull UUID playerUuid, final long readAt) throws SQLException {
        this.repository.markAllAsRead(playerUuid, readAt);
    }
}
