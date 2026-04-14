package net.azisaba.buildingboard.service.impl;

import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobMember;
import net.azisaba.buildingboard.model.job.JobMemberStatus;
import net.azisaba.buildingboard.model.job.JobStatus;
import net.azisaba.buildingboard.model.notification.NotificationCategory;
import net.azisaba.buildingboard.repository.JobMemberRepository;
import net.azisaba.buildingboard.repository.JobRepository;
import net.azisaba.buildingboard.service.JobApplicationService;
import net.azisaba.buildingboard.service.NotificationService;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class SimpleJobApplicationService implements JobApplicationService {
    private final @NotNull JobRepository jobRepository;
    private final @NotNull JobMemberRepository memberRepository;
    private final @NotNull NotificationService notificationService;

    public SimpleJobApplicationService(
            final @NotNull JobRepository jobRepository,
            final @NotNull JobMemberRepository memberRepository,
            final @NotNull NotificationService notificationService
    ) {
        this.jobRepository = jobRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void apply(final long jobId, final @NotNull OfflinePlayer applicant) throws SQLException {
        final Job job = this.requireJob(jobId);
        final long now = System.currentTimeMillis();
        if (job.getStatus() != JobStatus.OPEN || now > job.getRecruitmentDeadlineAt()) {
            throw new IllegalStateException("この依頼は現在応募を受け付けていません。");
        }
        if (job.getRequesterUuid().equals(applicant.getUniqueId())) {
            throw new IllegalStateException("発注者は自分の依頼に応募できません。");
        }
        if (this.memberRepository.findByJobIdAndPlayerUuid(jobId, applicant.getUniqueId()).isPresent()) {
            throw new IllegalStateException("この依頼にはすでに参加情報があります。");
        }
        this.memberRepository.insert(new JobMember(
                0L,
                jobId,
                applicant.getUniqueId(),
                applicant.getName() == null ? applicant.getUniqueId().toString() : applicant.getName(),
                JobMemberStatus.APPLIED,
                now,
                null,
                null,
                null,
                null
        ));
        this.notificationService.notify(
                job.getRequesterUuid(),
                NotificationCategory.APPLICATION_RECEIVED,
                jobId,
                "新しい応募があります",
                (applicant.getName() == null ? applicant.getUniqueId() : applicant.getName()) + " が依頼 #" + jobId + " に応募しました。"
        );
    }

    @Override
    public void approve(final long jobId, final @NotNull OfflinePlayer actor, final @NotNull OfflinePlayer target) throws SQLException {
        final Job job = this.requireJob(jobId);
        this.ensureRequesterOrAdmin(job, actor);
        final JobMember member = this.memberRepository.findByJobIdAndPlayerUuid(jobId, target.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("Application not found"));
        if (member.getStatus() != JobMemberStatus.APPLIED) {
            throw new IllegalStateException("このプレイヤーは応募中状態ではありません。");
        }
        final long now = System.currentTimeMillis();
        this.memberRepository.updateStatus(member.getId(), JobMemberStatus.CONFIRMED, now, null, actor.getUniqueId(), "Approved by requester");
        if (job.getStatus() == JobStatus.OPEN && now > job.getRecruitmentDeadlineAt()) {
            this.jobRepository.updateStatus(jobId, JobStatus.IN_PROGRESS, now, null, null, false);
        }
        this.notificationService.notify(
                target.getUniqueId(),
                NotificationCategory.APPLICATION_ACCEPTED,
                jobId,
                "応募が承認されました",
                "依頼 #" + jobId + " の施工者として確定しました。"
        );
    }

    @Override
    public void decline(final long jobId, final @NotNull OfflinePlayer actor, final @NotNull OfflinePlayer target) throws SQLException {
        final Job job = this.requireJob(jobId);
        this.ensureRequesterOrAdmin(job, actor);
        final JobMember member = this.memberRepository.findByJobIdAndPlayerUuid(jobId, target.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("Application not found"));
        if (member.getStatus() != JobMemberStatus.APPLIED) {
            throw new IllegalStateException("このプレイヤーは応募中状態ではありません。");
        }
        final long now = System.currentTimeMillis();
        this.memberRepository.updateStatus(member.getId(), JobMemberStatus.DECLINED, now, null, actor.getUniqueId(), "Declined by requester");
        this.notificationService.notify(
                target.getUniqueId(),
                NotificationCategory.APPLICATION_DECLINED,
                jobId,
                "応募が却下されました",
                "依頼 #" + jobId + " への応募は見送られました。"
        );
    }

    @Override
    public void addConfirmedMemberAfterDeadline(final long jobId, final @NotNull OfflinePlayer actor, final @NotNull OfflinePlayer target) throws SQLException {
        final Job job = this.requireJob(jobId);
        this.ensureRequesterOrAdmin(job, actor);
        final long now = System.currentTimeMillis();
        if (now <= job.getRecruitmentDeadlineAt()) {
            throw new IllegalStateException("手動追加は募集期限後のみ行えます。");
        }
        if (job.getRequesterUuid().equals(target.getUniqueId())) {
            throw new IllegalStateException("発注者を施工者にすることはできません。");
        }
        if (this.memberRepository.findByJobIdAndPlayerUuid(jobId, target.getUniqueId()).isPresent()) {
            throw new IllegalStateException("この依頼にはすでに参加情報があります。");
        }
        this.memberRepository.insert(new JobMember(
                0L,
                jobId,
                target.getUniqueId(),
                target.getName() == null ? target.getUniqueId().toString() : target.getName(),
                JobMemberStatus.CONFIRMED,
                now,
                now,
                null,
                actor.getUniqueId(),
                "Added by requester after deadline"
        ));
        if (job.getStatus() == JobStatus.OPEN) {
            this.jobRepository.updateStatus(jobId, JobStatus.IN_PROGRESS, now, null, null, false);
        }
        this.notificationService.notify(
                target.getUniqueId(),
                NotificationCategory.APPLICATION_ACCEPTED,
                jobId,
                "施工者に追加されました",
                "依頼 #" + jobId + " の施工者として追加されました。"
        );
    }

    @Override
    public void removeConfirmedMember(final long jobId, final @NotNull OfflinePlayer actor, final @NotNull OfflinePlayer target) throws SQLException {
        final Job job = this.requireJob(jobId);
        this.ensureRequesterOrAdmin(job, actor);
        final JobMember member = this.memberRepository.findByJobIdAndPlayerUuid(jobId, target.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("Member not found"));
        if (member.getStatus() != JobMemberStatus.CONFIRMED) {
            throw new IllegalStateException("確定済みの施工者だけを解除できます。");
        }
        final long now = System.currentTimeMillis();
        this.memberRepository.updateStatus(member.getId(), JobMemberStatus.REMOVED, member.getDecidedAt(), now, actor.getUniqueId(), "Removed by requester");
        this.notificationService.notify(
                target.getUniqueId(),
                NotificationCategory.MEMBER_REMOVED,
                jobId,
                "施工者から外されました",
                "依頼 #" + jobId + " の施工者一覧から外されました。"
        );
    }

    @Override
    public void withdraw(final long jobId, final @NotNull OfflinePlayer contractor) throws SQLException {
        final JobMember member = this.memberRepository.findByJobIdAndPlayerUuid(jobId, contractor.getUniqueId())
                .orElseThrow(() -> new IllegalStateException("Member not found"));
        if (member.getStatus() != JobMemberStatus.CONFIRMED) {
            throw new IllegalStateException("確定済みの施工者だけが辞退できます。");
        }
        final long now = System.currentTimeMillis();
        this.memberRepository.updateStatus(member.getId(), JobMemberStatus.WITHDRAWN, member.getDecidedAt(), now, contractor.getUniqueId(), "Withdrawn by contractor");
    }

    private @NotNull Job requireJob(final long jobId) throws SQLException {
        return this.jobRepository.findById(jobId).orElseThrow(() -> new IllegalStateException("Unknown job #" + jobId));
    }

    private void ensureRequesterOrAdmin(final @NotNull Job job, final @NotNull OfflinePlayer actor) {
        if (!actor.isOp() && !job.getRequesterUuid().equals(actor.getUniqueId())) {
            throw new IllegalStateException("この依頼を管理する権限がありません。");
        }
    }
}
