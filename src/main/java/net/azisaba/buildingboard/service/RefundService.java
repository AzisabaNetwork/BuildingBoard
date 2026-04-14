package net.azisaba.buildingboard.service;

import net.azisaba.buildingboard.model.refund.JobRefund;
import net.azisaba.buildingboard.model.refund.RefundReason;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface RefundService {
    @NotNull JobRefund enqueueRefund(long jobId, @NotNull UUID requesterUuid, long amount, @NotNull RefundReason reason) throws SQLException;
    @NotNull List<JobRefund> getPendingRefunds(@NotNull UUID requesterUuid) throws SQLException;
    void processPendingRefunds(@NotNull OfflinePlayer player) throws SQLException;
}
