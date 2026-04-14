package net.azisaba.buildingboard.repository.impl;

import net.azisaba.buildingboard.model.refund.JobRefund;
import net.azisaba.buildingboard.model.refund.RefundReason;
import net.azisaba.buildingboard.model.refund.RefundStatus;
import net.azisaba.buildingboard.repository.JobRefundRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcJobRefundRepository extends JdbcRepositorySupport implements JobRefundRepository {
    public JdbcJobRefundRepository(final @NotNull DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public @NotNull JobRefund insert(final @NotNull JobRefund refund) throws SQLException {
        final String sql = "INSERT INTO job_refunds (job_id, requester_uuid, amount, reason, status, created_at, processed_at, failure_reason)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, refund.getJobId());
            statement.setString(2, refund.getRequesterUuid().toString());
            statement.setLong(3, refund.getAmount());
            statement.setString(4, refund.getReason().name());
            statement.setString(5, refund.getStatus().name());
            statement.setLong(6, refund.getCreatedAt());
            this.setNullableLong(statement, 7, refund.getProcessedAt());
            this.setNullableString(statement, 8, refund.getFailureReason());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to obtain generated refund id");
                }
                return new JobRefund(
                        keys.getLong(1),
                        refund.getJobId(),
                        refund.getRequesterUuid(),
                        refund.getAmount(),
                        refund.getReason(),
                        refund.getStatus(),
                        refund.getCreatedAt(),
                        refund.getProcessedAt(),
                        refund.getFailureReason()
                );
            }
        }
    }

    @Override
    public @NotNull List<JobRefund> findPendingByRequesterUuid(final @NotNull UUID requesterUuid) throws SQLException {
        final String sql = "SELECT * FROM job_refunds WHERE requester_uuid = ? AND status = ? ORDER BY created_at ASC";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requesterUuid.toString());
            statement.setString(2, RefundStatus.PENDING.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<JobRefund> refunds = new ArrayList<>();
                while (resultSet.next()) {
                    refunds.add(this.map(resultSet));
                }
                return refunds;
            }
        }
    }

    @Override
    public void updateStatus(final long id, final @NotNull RefundStatus status, final @Nullable Long processedAt, final @Nullable String failureReason) throws SQLException {
        final String sql = "UPDATE job_refunds SET status = ?, processed_at = ?, failure_reason = ? WHERE id = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            this.setNullableLong(statement, 2, processedAt);
            this.setNullableString(statement, 3, failureReason);
            statement.setLong(4, id);
            statement.executeUpdate();
        }
    }

    private @NotNull JobRefund map(final @NotNull ResultSet resultSet) throws SQLException {
        return new JobRefund(
                resultSet.getLong("id"),
                resultSet.getLong("job_id"),
                this.getUuid(resultSet, "requester_uuid"),
                resultSet.getLong("amount"),
                RefundReason.valueOf(resultSet.getString("reason")),
                RefundStatus.valueOf(resultSet.getString("status")),
                resultSet.getLong("created_at"),
                this.getNullableLong(resultSet, "processed_at"),
                resultSet.getString("failure_reason")
        );
    }
}
