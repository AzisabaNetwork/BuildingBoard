package net.azisaba.buildingboard.repository;

import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    @NotNull Job insert(@NotNull Job job) throws SQLException;
    @NotNull Optional<Job> findById(long id) throws SQLException;
    @NotNull List<Job> findAll(int limit, int offset) throws SQLException;
    @NotNull List<Job> findByRequester(@NotNull UUID requesterUuid) throws SQLException;
    @NotNull List<Job> findByConfirmedMember(@NotNull UUID playerUuid) throws SQLException;
    @NotNull List<Job> findBrowsableOpenJobs(long now, int limit, int offset) throws SQLException;
    @NotNull List<Job> findRecruitmentDeadlinePassed(long now) throws SQLException;
    @NotNull List<Job> findWorkDeadlinePassed(long now) throws SQLException;
    @NotNull List<Job> findForceCompletable(long now) throws SQLException;
    void updateDetails(long jobId, @NotNull String title, @NotNull String description, long totalReward, long recruitmentDeadlineAt, long workDeadlineAt, long forceCompleteAt, long updatedAt) throws SQLException;
    void updateStatus(long jobId, @NotNull JobStatus status, long updatedAt, @Nullable Long completedAt, @Nullable Long cancelledAt, boolean completedByForce) throws SQLException;
}
