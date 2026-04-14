package net.azisaba.buildingboard.service;

import net.azisaba.buildingboard.model.notification.JobNotification;
import net.azisaba.buildingboard.model.notification.NotificationCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    @NotNull JobNotification notify(@NotNull UUID playerUuid, @NotNull NotificationCategory category, @Nullable Long jobId, @NotNull String title, @NotNull String body) throws SQLException;
    @NotNull List<JobNotification> getNotifications(@NotNull UUID playerUuid, boolean unreadOnly, int limit, int offset) throws SQLException;
    int countUnread(@NotNull UUID playerUuid) throws SQLException;
    void markAsRead(long notificationId, long readAt) throws SQLException;
    void markAllAsRead(@NotNull UUID playerUuid, long readAt) throws SQLException;
}
