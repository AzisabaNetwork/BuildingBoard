package net.azisaba.buildingboard.model.job;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class Job {
    private final long id;
    private final @NotNull UUID requesterUuid;
    private final @NotNull String requesterName;
    private final @NotNull String title;
    private final @NotNull String description;
    private final @NotNull JobStatus status;
    private final long totalReward;
    private final long recruitmentDeadlineAt;
    private final long workDeadlineAt;
    private final long forceCompleteAt;
    private final long createdAt;
    private final long updatedAt;
    private final @Nullable Long completedAt;
    private final @Nullable Long cancelledAt;
    private final boolean completedByForce;

    public Job(
            final long id,
            final @NotNull UUID requesterUuid,
            final @NotNull String requesterName,
            final @NotNull String title,
            final @NotNull String description,
            final @NotNull JobStatus status,
            final long totalReward,
            final long recruitmentDeadlineAt,
            final long workDeadlineAt,
            final long forceCompleteAt,
            final long createdAt,
            final long updatedAt,
            final @Nullable Long completedAt,
            final @Nullable Long cancelledAt,
            final boolean completedByForce
    ) {
        this.id = id;
        this.requesterUuid = Objects.requireNonNull(requesterUuid, "requesterUuid");
        this.requesterName = Objects.requireNonNull(requesterName, "requesterName");
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.status = Objects.requireNonNull(status, "status");
        this.totalReward = totalReward;
        this.recruitmentDeadlineAt = recruitmentDeadlineAt;
        this.workDeadlineAt = workDeadlineAt;
        this.forceCompleteAt = forceCompleteAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
        this.completedByForce = completedByForce;
    }

    public long getId() { return this.id; }
    public @NotNull UUID getRequesterUuid() { return this.requesterUuid; }
    public @NotNull String getRequesterName() { return this.requesterName; }
    public @NotNull String getTitle() { return this.title; }
    public @NotNull String getDescription() { return this.description; }
    public @NotNull JobStatus getStatus() { return this.status; }
    public long getTotalReward() { return this.totalReward; }
    public long getRecruitmentDeadlineAt() { return this.recruitmentDeadlineAt; }
    public long getWorkDeadlineAt() { return this.workDeadlineAt; }
    public long getForceCompleteAt() { return this.forceCompleteAt; }
    public long getCreatedAt() { return this.createdAt; }
    public long getUpdatedAt() { return this.updatedAt; }
    public @Nullable Long getCompletedAt() { return this.completedAt; }
    public @Nullable Long getCancelledAt() { return this.cancelledAt; }
    public boolean isCompletedByForce() { return this.completedByForce; }
}
