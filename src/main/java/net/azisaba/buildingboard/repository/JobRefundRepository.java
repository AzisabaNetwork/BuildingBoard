package net.azisaba.buildingboard.repository;

import net.azisaba.buildingboard.model.refund.JobRefund;
import net.azisaba.buildingboard.model.refund.RefundStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface JobRefundRepository {
    @NotNull JobRefund insert(@NotNull JobRefund refund) throws SQLException;
    @NotNull List<JobRefund> findPendingByRequesterUuid(@NotNull UUID requesterUuid) throws SQLException;
    void updateStatus(long id, @NotNull RefundStatus status, @Nullable Long processedAt, @Nullable String failureReason) throws SQLException;
}
