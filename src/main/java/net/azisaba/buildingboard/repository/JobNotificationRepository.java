package net.azisaba.buildingboard.repository;

import net.azisaba.buildingboard.model.notification.JobNotification;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface JobNotificationRepository {
    @NotNull JobNotification insert(@NotNull JobNotification notification) throws SQLException;
    @NotNull List<JobNotification> findByPlayerUuid(@NotNull UUID playerUuid, boolean unreadOnly, int limit, int offset) throws SQLException;
    int countUnread(@NotNull UUID playerUuid) throws SQLException;
    void markAsRead(long notificationId, long readAt) throws SQLException;
    void markAllAsRead(@NotNull UUID playerUuid, long readAt) throws SQLException;
}
