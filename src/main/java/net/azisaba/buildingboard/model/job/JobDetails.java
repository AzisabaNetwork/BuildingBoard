package net.azisaba.buildingboard.model.job;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class JobDetails {
    private final @NotNull Job job;
    private final @NotNull JobRegion region;
    private final @NotNull List<JobMember> members;
    private final int appliedCount;
    private final int confirmedCount;
    private final long rewardPerConfirmedMember;
    private final long rewardRemainder;

    public JobDetails(
            final @NotNull Job job,
            final @NotNull JobRegion region,
            final @NotNull List<JobMember> members,
            final int appliedCount,
            final int confirmedCount,
            final long rewardPerConfirmedMember,
            final long rewardRemainder
    ) {
        this.job = Objects.requireNonNull(job, "job");
        this.region = Objects.requireNonNull(region, "region");
        this.members = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(members, "members")));
        this.appliedCount = appliedCount;
        this.confirmedCount = confirmedCount;
        this.rewardPerConfirmedMember = rewardPerConfirmedMember;
        this.rewardRemainder = rewardRemainder;
    }

    public @NotNull Job getJob() { return this.job; }
    public @NotNull JobRegion getRegion() { return this.region; }
    public @NotNull List<JobMember> getMembers() { return this.members; }
    public int getAppliedCount() { return this.appliedCount; }
    public int getConfirmedCount() { return this.confirmedCount; }
    public long getRewardPerConfirmedMember() { return this.rewardPerConfirmedMember; }
    public long getRewardRemainder() { return this.rewardRemainder; }
}
