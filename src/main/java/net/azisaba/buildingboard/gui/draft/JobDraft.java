package net.azisaba.buildingboard.gui.draft;

import net.azisaba.buildingboard.model.job.JobRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JobDraft {
    private final @NotNull JobDraftMode mode;
    private final long jobId;
    private @NotNull String title;
    private final @NotNull List<String> descriptionLines;
    private long reward;
    private int recruitmentDays;
    private int workDays;
    private @Nullable JobRegion region;

    public JobDraft(
            final @NotNull JobDraftMode mode,
            final long jobId,
            final @NotNull String title,
            final @NotNull List<String> descriptionLines,
            final long reward,
            final int recruitmentDays,
            final int workDays,
            final @Nullable JobRegion region
    ) {
        this.mode = mode;
        this.jobId = jobId;
        this.title = title;
        this.descriptionLines = new ArrayList<>(descriptionLines);
        this.reward = reward;
        this.recruitmentDays = recruitmentDays;
        this.workDays = workDays;
        this.region = region;
    }

    public @NotNull JobDraftMode getMode() {
        return this.mode;
    }

    public long getJobId() {
        return this.jobId;
    }

    public @NotNull String getTitle() {
        return this.title;
    }

    public void setTitle(final @NotNull String title) {
        this.title = title;
    }

    public @NotNull List<String> getDescriptionLines() {
        return Collections.unmodifiableList(this.descriptionLines);
    }

    public void addDescriptionLine(final @NotNull String line) {
        this.descriptionLines.add(line);
    }

    public void setDescriptionLine(final int index, final @NotNull String line) {
        this.descriptionLines.set(index, line);
    }

    public void removeDescriptionLine(final int index) {
        this.descriptionLines.remove(index);
    }

    public void clearDescriptionLines() {
        this.descriptionLines.clear();
    }

    public @NotNull String joinDescription() {
        return String.join("\n", this.descriptionLines);
    }

    public long getReward() {
        return this.reward;
    }

    public void setReward(final long reward) {
        this.reward = reward;
    }

    public int getRecruitmentDays() {
        return this.recruitmentDays;
    }

    public void setRecruitmentDays(final int recruitmentDays) {
        this.recruitmentDays = recruitmentDays;
    }

    public int getWorkDays() {
        return this.workDays;
    }

    public void setWorkDays(final int workDays) {
        this.workDays = workDays;
    }

    public @Nullable JobRegion getRegion() {
        return this.region;
    }

    public void setRegion(final @Nullable JobRegion region) {
        this.region = region;
    }
}
