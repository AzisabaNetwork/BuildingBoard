package net.azisaba.buildingboard.model.notification;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public final class JobNotification {
    private final long id;
    private final @NotNull UUID playerUuid;
    private final @NotNull NotificationCategory category;
    private final @Nullable Long jobId;
    private final @NotNull String title;
    private final @NotNull String body;
    private final boolean read;
    private final long createdAt;
    private final @Nullable Long readAt;

    public JobNotification(
            final long id,
            final @NotNull UUID playerUuid,
            final @NotNull NotificationCategory category,
            final @Nullable Long jobId,
            final @NotNull String title,
            final @NotNull String body,
            final boolean read,
            final long createdAt,
            final @Nullable Long readAt
    ) {
        this.id = id;
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.category = Objects.requireNonNull(category, "category");
        this.jobId = jobId;
        this.title = Objects.requireNonNull(title, "title");
        this.body = Objects.requireNonNull(body, "body");
        this.read = read;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public long getId() { return this.id; }
    public @NotNull UUID getPlayerUuid() { return this.playerUuid; }
    public @NotNull NotificationCategory getCategory() { return this.category; }
    public @Nullable Long getJobId() { return this.jobId; }
    public @NotNull String getTitle() { return this.title; }
    public @NotNull String getBody() { return this.body; }
    public boolean isRead() { return this.read; }
    public long getCreatedAt() { return this.createdAt; }
    public @Nullable Long getReadAt() { return this.readAt; }
}
