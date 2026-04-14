package net.azisaba.buildingboard.service;

import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobDetails;
import net.azisaba.buildingboard.model.job.JobRegion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

public interface JobService {
    @NotNull Job createJob(@NotNull OfflinePlayer requester, @NotNull String title, @NotNull String description, long totalReward, long recruitmentDeadlineAt, long workDeadlineAt, @NotNull JobRegion region) throws SQLException;
    void updateJobDetails(long jobId, @NotNull OfflinePlayer actor, @NotNull String title, @NotNull String description, long totalReward, long recruitmentDeadlineAt, long workDeadlineAt) throws SQLException;
    void cancelJob(long jobId, @NotNull OfflinePlayer actor) throws SQLException;
    @NotNull JobDetails getJobDetails(long jobId) throws SQLException;
    @NotNull List<Job> getAllJobs(int limit, int offset) throws SQLException;
    @NotNull List<Job> getBrowsableOpenJobs(long now, int limit, int offset) throws SQLException;
    @NotNull List<Job> getCurrentContractorJobs(@NotNull OfflinePlayer player) throws SQLException;
}
