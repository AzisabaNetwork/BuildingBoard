package net.azisaba.buildingboard.service.impl;

import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobMember;
import net.azisaba.buildingboard.model.job.JobMemberStatus;
import net.azisaba.buildingboard.model.job.JobStatus;
import net.azisaba.buildingboard.model.notification.NotificationCategory;
import net.azisaba.buildingboard.model.refund.RefundReason;
import net.azisaba.buildingboard.repository.JobMemberRepository;
import net.azisaba.buildingboard.repository.JobRepository;
import net.azisaba.buildingboard.service.JobCompletionService;
import net.azisaba.buildingboard.service.NotificationService;
import net.azisaba.buildingboard.service.RefundService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

public final class SimpleJobCompletionService implements JobCompletionService {
    private final @NotNull JobRepository jobRepository;
    private final @NotNull JobMemberRepository memberRepository;
    private final @NotNull NotificationService notificationService;
    private final @NotNull RefundService refundService;
    private final @Nullable Economy economy;

    public SimpleJobCompletionService(
            final @NotNull JobRepository jobRepository,
            final @NotNull JobMemberRepository memberRepository,
            final @NotNull NotificationService notificationService,
            final @NotNull RefundService refundService,
            final @Nullable Economy economy
    ) {
        this.jobRepository = jobRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.refundService = refundService;
        this.economy = economy;
    }

    @Override
    public void complete(final long jobId, final @NotNull OfflinePlayer actor) throws SQLException {
        final Job job = this.requireJob(jobId);
        this.ensureRequesterOrAdmin(job, actor);
        this.completeInternal(job, false);
    }

    @Override
    public void forceComplete(final long jobId) throws SQLException {
        this.completeInternal(this.requireJob(jobId), true);
    }

    @Override
    public void updateExpiredJobStatuses(final long now) throws SQLException {
        for (Job job : this.jobRepository.findRecruitmentDeadlinePassed(now)) {
            if (this.memberRepository.countByJobIdAndStatus(job.getId(), JobMemberStatus.CONFIRMED) > 0) {
                this.jobRepository.updateStatus(job.getId(), JobStatus.IN_PROGRESS, now, null, null, false);
            }
        }
        for (Job job : this.jobRepository.findWorkDeadlinePassed(now)) {
            if (job.getStatus() != JobStatus.WORK_DEADLINE_PASSED && job.getStatus() != JobStatus.COMPLETED && job.getStatus() != JobStatus.CANCELLED && job.getStatus() != JobStatus.EXPIRED) {
                this.jobRepository.updateStatus(job.getId(), JobStatus.WORK_DEADLINE_PASSED, now, null, null, false);
            }
        }
        for (Job job : this.jobRepository.findForceCompletable(now)) {
            this.completeInternal(job, true);
        }
    }

    private void completeInternal(final @NotNull Job job, final boolean force) throws SQLException {
        final List<JobMember> confirmedMembers = this.memberRepository.findByJobIdAndStatus(job.getId(), JobMemberStatus.CONFIRMED);
        final long now = System.currentTimeMillis();
        if (confirmedMembers.isEmpty()) {
            this.jobRepository.updateStatus(job.getId(), JobStatus.EXPIRED, now, null, null, false);
            this.refundService.enqueueRefund(job.getId(), job.getRequesterUuid(), job.getTotalReward(), RefundReason.NO_CONFIRMED_MEMBERS_EXPIRED);
            return;
        }
        final long rewardPerMember = job.getTotalReward() / confirmedMembers.size();
        final long remainder = job.getTotalReward() - (rewardPerMember * confirmedMembers.size());
        if (this.economy != null) {
            for (JobMember member : confirmedMembers) {
                this.economy.depositPlayer(Bukkit.getOfflinePlayer(member.getPlayerUuid()), rewardPerMember);
            }
        }
        if (remainder > 0L) {
            this.refundService.enqueueRefund(job.getId(), job.getRequesterUuid(), remainder, RefundReason.REWARD_REMAINDER);
        }
        this.jobRepository.updateStatus(job.getId(), JobStatus.COMPLETED, now, now, null, force);
        for (JobMember member : confirmedMembers) {
            this.notificationService.notify(
                    member.getPlayerUuid(),
                    force ? NotificationCategory.JOB_FORCE_COMPLETED : NotificationCategory.JOB_COMPLETED,
                    job.getId(),
                    force ? "依頼が自動完了しました" : "依頼が完了しました",
                    "依頼 #" + job.getId() + " は完了済みとして処理され、報酬の支払いも実行されました。"
            );
        }
        this.notificationService.notify(
                job.getRequesterUuid(),
                force ? NotificationCategory.JOB_FORCE_COMPLETED : NotificationCategory.JOB_COMPLETED,
                job.getId(),
                force ? "依頼が自動完了しました" : "依頼が完了しました",
                "依頼 #" + job.getId() + " は完了済みとして処理され、必要な支払いが実行されました。"
        );
    }

    private @NotNull Job requireJob(final long jobId) throws SQLException {
        return this.jobRepository.findById(jobId).orElseThrow(() -> new IllegalStateException("Unknown job #" + jobId));
    }

    private void ensureRequesterOrAdmin(final @NotNull Job job, final @NotNull OfflinePlayer actor) {
        if (!actor.isOp() && !job.getRequesterUuid().equals(actor.getUniqueId())) {
            throw new IllegalStateException("この依頼を完了できる権限がありません。");
        }
    }
}
