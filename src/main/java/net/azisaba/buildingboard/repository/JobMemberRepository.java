package net.azisaba.buildingboard.repository;

import net.azisaba.buildingboard.model.job.JobMember;
import net.azisaba.buildingboard.model.job.JobMemberStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobMemberRepository {
    @NotNull JobMember insert(@NotNull JobMember member) throws SQLException;
    @NotNull Optional<JobMember> findByJobIdAndPlayerUuid(long jobId, @NotNull UUID playerUuid) throws SQLException;
    @NotNull List<JobMember> findByJobId(long jobId) throws SQLException;
    @NotNull List<JobMember> findByJobIdAndStatus(long jobId, @NotNull JobMemberStatus status) throws SQLException;
    int countByJobIdAndStatus(long jobId, @NotNull JobMemberStatus status) throws SQLException;
    void updateStatus(long id, @NotNull JobMemberStatus status, @Nullable Long decidedAt, @Nullable Long leftAt, @Nullable UUID decidedByUuid, @Nullable String note) throws SQLException;
}
