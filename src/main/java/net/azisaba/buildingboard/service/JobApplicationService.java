package net.azisaba.buildingboard.service;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public interface JobApplicationService {
    void apply(long jobId, @NotNull OfflinePlayer applicant) throws SQLException;
    void approve(long jobId, @NotNull OfflinePlayer actor, @NotNull OfflinePlayer target) throws SQLException;
    void decline(long jobId, @NotNull OfflinePlayer actor, @NotNull OfflinePlayer target) throws SQLException;
    void addConfirmedMemberAfterDeadline(long jobId, @NotNull OfflinePlayer actor, @NotNull OfflinePlayer target) throws SQLException;
    void removeConfirmedMember(long jobId, @NotNull OfflinePlayer actor, @NotNull OfflinePlayer target) throws SQLException;
    void withdraw(long jobId, @NotNull OfflinePlayer contractor) throws SQLException;
}
