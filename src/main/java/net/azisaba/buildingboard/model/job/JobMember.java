package net.azisaba.buildingboard.model.job;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class JobMember {
    private final long id;
    private final long jobId;
    private final @NotNull UUID playerUuid;
    private final @NotNull String playerName;
    private final @NotNull JobMemberStatus status;
    private final long joinedAt;
    private final @Nullable Long decidedAt;
    private final @Nullable Long leftAt;
    private final @Nullable UUID decidedByUuid;
    private final @Nullable String note;

    public JobMember(
            final long id,
            final long jobId,
            final @NotNull UUID playerUuid,
            final @NotNull String playerName,
            final @NotNull JobMemberStatus status,
            final long joinedAt,
            final @Nullable Long decidedAt,
            final @Nullable Long leftAt,
            final @Nullable UUID decidedByUuid,
            final @Nullable String note
    ) {
        this.id = id;
        this.jobId = jobId;
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.status = Objects.requireNonNull(status, "status");
        this.joinedAt = joinedAt;
        this.decidedAt = decidedAt;
        this.leftAt = leftAt;
        this.decidedByUuid = decidedByUuid;
        this.note = note;
    }

    public long getId() { return this.id; }
    public long getJobId() { return this.jobId; }
    public @NotNull UUID getPlayerUuid() { return this.playerUuid; }
    public @NotNull String getPlayerName() { return this.playerName; }
    public @NotNull JobMemberStatus getStatus() { return this.status; }
    public long getJoinedAt() { return this.joinedAt; }
    public @Nullable Long getDecidedAt() { return this.decidedAt; }
    public @Nullable Long getLeftAt() { return this.leftAt; }
    public @Nullable UUID getDecidedByUuid() { return this.decidedByUuid; }
    public @Nullable String getNote() { return this.note; }
}
