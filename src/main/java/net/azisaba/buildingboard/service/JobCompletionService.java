package net.azisaba.buildingboard.service;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public interface JobCompletionService {
    void complete(long jobId, @NotNull OfflinePlayer actor) throws SQLException;
    void forceComplete(long jobId) throws SQLException;
    void updateExpiredJobStatuses(long now) throws SQLException;
}
