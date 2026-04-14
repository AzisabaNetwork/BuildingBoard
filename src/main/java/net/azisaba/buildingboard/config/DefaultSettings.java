package net.azisaba.buildingboard.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public final class DefaultSettings {
    private final int recruitmentDeadlineDays;
    private final int workDeadlineDays;
    private final int forceCompleteExtraDays;
    private final int deadlineCheckIntervalSeconds;

    public DefaultSettings(
            final int recruitmentDeadlineDays,
            final int workDeadlineDays,
            final int forceCompleteExtraDays,
            final int deadlineCheckIntervalSeconds
    ) {
        this.recruitmentDeadlineDays = recruitmentDeadlineDays;
        this.workDeadlineDays = workDeadlineDays;
        this.forceCompleteExtraDays = forceCompleteExtraDays;
        this.deadlineCheckIntervalSeconds = deadlineCheckIntervalSeconds;
    }

    public static @NotNull DefaultSettings fromConfig(final @NotNull FileConfiguration config) {
        return new DefaultSettings(
                config.getInt("defaults.recruitment-deadline-days", 7),
                config.getInt("defaults.work-deadline-days", 30),
                config.getInt("defaults.force-complete-extra-days", 60),
                config.getInt("tasks.deadline-check-interval-seconds", 300)
        );
    }

    public int getRecruitmentDeadlineDays() {
        return this.recruitmentDeadlineDays;
    }

    public int getWorkDeadlineDays() {
        return this.workDeadlineDays;
    }

    public int getForceCompleteExtraDays() {
        return this.forceCompleteExtraDays;
    }

    public int getDeadlineCheckIntervalSeconds() {
        return this.deadlineCheckIntervalSeconds;
    }
}
