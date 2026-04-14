package net.azisaba.buildingboard.model.refund;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class JobRefund {
    private final long id;
    private final long jobId;
    private final @NotNull UUID requesterUuid;
    private final long amount;
    private final @NotNull RefundReason reason;
    private final @NotNull RefundStatus status;
    private final long createdAt;
    private final @Nullable Long processedAt;
    private final @Nullable String failureReason;

    public JobRefund(
            final long id,
            final long jobId,
            final @NotNull UUID requesterUuid,
            final long amount,
            final @NotNull RefundReason reason,
            final @NotNull RefundStatus status,
            final long createdAt,
            final @Nullable Long processedAt,
            final @Nullable String failureReason
    ) {
        this.id = id;
        this.jobId = jobId;
        this.requesterUuid = Objects.requireNonNull(requesterUuid, "requesterUuid");
        this.amount = amount;
        this.reason = Objects.requireNonNull(reason, "reason");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.failureReason = failureReason;
    }

    public long getId() { return this.id; }
    public long getJobId() { return this.jobId; }
    public @NotNull UUID getRequesterUuid() { return this.requesterUuid; }
    public long getAmount() { return this.amount; }
    public @NotNull RefundReason getReason() { return this.reason; }
    public @NotNull RefundStatus getStatus() { return this.status; }
    public long getCreatedAt() { return this.createdAt; }
    public @Nullable Long getProcessedAt() { return this.processedAt; }
    public @Nullable String getFailureReason() { return this.failureReason; }
}
