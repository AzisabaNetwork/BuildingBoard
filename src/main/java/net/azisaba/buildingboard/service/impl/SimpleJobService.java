package net.azisaba.buildingboard.service.impl;

import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobDetails;
import net.azisaba.buildingboard.model.job.JobMember;
import net.azisaba.buildingboard.model.job.JobMemberStatus;
import net.azisaba.buildingboard.model.job.JobRegion;
import net.azisaba.buildingboard.model.job.JobStatus;
import net.azisaba.buildingboard.model.notification.NotificationCategory;
import net.azisaba.buildingboard.model.refund.RefundReason;
import net.azisaba.buildingboard.repository.JobMemberRepository;
import net.azisaba.buildingboard.repository.JobRegionRepository;
import net.azisaba.buildingboard.repository.JobRepository;
import net.azisaba.buildingboard.service.JobService;
import net.azisaba.buildingboard.service.NotificationService;
import net.azisaba.buildingboard.service.RefundService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

public final class SimpleJobService implements JobService {
    private final @NotNull JobRepository jobRepository;
    private final @NotNull JobRegionRepository regionRepository;
    private final @NotNull JobMemberRepository memberRepository;
    private final @NotNull NotificationService notificationService;
    private final @NotNull RefundService refundService;
    private final @Nullable Economy economy;
    private final long forceCompleteExtraMillis;

    public SimpleJobService(
            final @NotNull JobRepository jobRepository,
            final @NotNull JobRegionRepository regionRepository,
            final @NotNull JobMemberRepository memberRepository,
            final @NotNull NotificationService notificationService,
            final @NotNull RefundService refundService,
            final @Nullable Economy economy,
            final long forceCompleteExtraMillis
    ) {
        this.jobRepository = jobRepository;
        this.regionRepository = regionRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.refundService = refundService;
        this.economy = economy;
        this.forceCompleteExtraMillis = forceCompleteExtraMillis;
    }

    @Override
    public @NotNull Job createJob(final @NotNull OfflinePlayer requester, final @NotNull String title, final @NotNull String description, final long totalReward, final long recruitmentDeadlineAt, final long workDeadlineAt, final @NotNull JobRegion region) throws SQLException {
        if (this.regionRepository.existsOverlappingRegion(region.getWorldName(), region.getMinX(), region.getMinY(), region.getMinZ(), region.getMaxX(), region.getMaxY(), region.getMaxZ())) {
            throw new IllegalStateException("選択した範囲は、すでに有効な依頼範囲と重複しています。");
        }
        this.requireEconomy();
        this.withdrawOrThrow(requester, totalReward, "依頼作成時の報酬預かりに失敗しました。");
        final long now = System.currentTimeMillis();
        final long forceCompleteAt = workDeadlineAt + this.forceCompleteExtraMillis;
        final Job inserted = this.jobRepository.insert(new Job(
                0L,
                requester.getUniqueId(),
                requester.getName() == null ? requester.getUniqueId().toString() : requester.getName(),
                title,
                description,
                JobStatus.OPEN,
                totalReward,
                recruitmentDeadlineAt,
                workDeadlineAt,
                forceCompleteAt,
                now,
                now,
                null,
                null,
                false
        ));
        this.regionRepository.insert(new JobRegion(
                inserted.getId(),
                region.getWorldName(),
                region.getMinX(),
                region.getMinY(),
                region.getMinZ(),
                region.getMaxX(),
                region.getMaxY(),
                region.getMaxZ()
        ));
        return inserted;
    }

    @Override
    public void updateJobDetails(final long jobId, final @NotNull OfflinePlayer actor, final @NotNull String title, final @NotNull String description, final long totalReward, final long recruitmentDeadlineAt, final long workDeadlineAt) throws SQLException {
        final Job job = this.requireJob(jobId);
        this.ensureRequesterOrAdmin(job, actor);
        this.requireEconomy();
        this.adjustRewardEscrow(job, totalReward);
        final long forceCompleteAt = workDeadlineAt + this.forceCompleteExtraMillis;
        this.jobRepository.updateDetails(jobId, title, description, totalReward, recruitmentDeadlineAt, workDeadlineAt, forceCompleteAt, System.currentTimeMillis());
    }

    @Override
    public void cancelJob(final long jobId, final @NotNull OfflinePlayer actor) throws SQLException {
        final Job job = this.requireJob(jobId);
        this.ensureRequesterOrAdmin(job, actor);
        this.jobRepository.updateStatus(jobId, JobStatus.CANCELLED, System.currentTimeMillis(), null, System.currentTimeMillis(), false);
        this.refundService.enqueueRefund(jobId, job.getRequesterUuid(), job.getTotalReward(), RefundReason.JOB_CANCELLED);
        for (JobMember member : this.memberRepository.findByJobIdAndStatus(jobId, JobMemberStatus.CONFIRMED)) {
            this.notificationService.notify(
                    member.getPlayerUuid(),
                    NotificationCategory.JOB_CANCELLED,
                    jobId,
                    "依頼が取り消されました",
                    "依頼 #" + jobId + " は発注者または管理者によって取り消されました。"
            );
        }
    }

    @Override
    public @NotNull JobDetails getJobDetails(final long jobId) throws SQLException {
        final Job job = this.requireJob(jobId);
        final JobRegion region = this.regionRepository.findByJobId(jobId).orElseThrow(() -> new IllegalStateException("Region missing for job #" + jobId));
        final List<JobMember> members = this.memberRepository.findByJobId(jobId);
        final int appliedCount = (int) members.stream().filter(member -> member.getStatus() == JobMemberStatus.APPLIED).count();
        final int confirmedCount = (int) members.stream().filter(member -> member.getStatus() == JobMemberStatus.CONFIRMED).count();
        final long rewardPerMember = confirmedCount == 0 ? 0L : job.getTotalReward() / confirmedCount;
        final long rewardRemainder = confirmedCount == 0 ? job.getTotalReward() : job.getTotalReward() - (rewardPerMember * confirmedCount);
        return new JobDetails(job, region, members, appliedCount, confirmedCount, rewardPerMember, rewardRemainder);
    }

    @Override
    public @NotNull List<Job> getAllJobs(final int limit, final int offset) throws SQLException {
        return this.jobRepository.findAll(limit, offset);
    }

    @Override
    public @NotNull List<Job> getBrowsableOpenJobs(final long now, final int limit, final int offset) throws SQLException {
        return this.jobRepository.findBrowsableOpenJobs(now, limit, offset);
    }

    @Override
    public @NotNull List<Job> getCurrentContractorJobs(final @NotNull OfflinePlayer player) throws SQLException {
        return this.jobRepository.findByConfirmedMember(player.getUniqueId());
    }

    private @NotNull Job requireJob(final long jobId) throws SQLException {
        return this.jobRepository.findById(jobId).orElseThrow(() -> new IllegalStateException("Unknown job #" + jobId));
    }

    private void ensureRequesterOrAdmin(final @NotNull Job job, final @NotNull OfflinePlayer actor) {
        if (!actor.isOp() && !job.getRequesterUuid().equals(actor.getUniqueId())) {
            throw new IllegalStateException("この依頼を管理する権限がありません。");
        }
    }

    private void adjustRewardEscrow(final @NotNull Job job, final long newTotalReward) throws SQLException {
        final long difference = newTotalReward - job.getTotalReward();
        if (difference == 0L) {
            return;
        }
        final OfflinePlayer requester = Bukkit.getOfflinePlayer(job.getRequesterUuid());
        if (difference > 0L) {
            this.withdrawOrThrow(requester, difference, "報酬増額分の引き落としに失敗しました。");
            return;
        }
        final long refundAmount = -difference;
        if (this.economy == null) {
            throw new IllegalStateException("経済プラグインが見つからないため差額返金できません。");
        }
        final EconomyResponse response = this.economy.depositPlayer(requester, refundAmount);
        if (!response.transactionSuccess()) {
            this.refundService.enqueueRefund(job.getId(), job.getRequesterUuid(), refundAmount, RefundReason.REWARD_REMAINDER);
        }
    }

    private void withdrawOrThrow(final @NotNull OfflinePlayer player, final long amount, final @NotNull String message) {
        if (this.economy == null) {
            throw new IllegalStateException("経済プラグインが見つからないため依頼を作成できません。");
        }
        final EconomyResponse response = this.economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            throw new IllegalStateException(message + " 所持金を確認してください。");
        }
    }

    private void requireEconomy() {
        if (this.economy == null) {
            throw new IllegalStateException("経済プラグインが見つからないため依頼を処理できません。");
        }
    }
}
