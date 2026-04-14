package net.azisaba.buildingboard.repository.impl;

import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobStatus;
import net.azisaba.buildingboard.repository.JobRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcJobRepository extends JdbcRepositorySupport implements JobRepository {
    public JdbcJobRepository(final @NotNull DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public @NotNull Job insert(final @NotNull Job job) throws SQLException {
        final String sql = "INSERT INTO jobs (requester_uuid, requester_name, title, description, status, total_reward,"
                + " recruitment_deadline_at, work_deadline_at, force_complete_at, created_at, updated_at, completed_at, cancelled_at, completed_by_force)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, job.getRequesterUuid().toString());
            statement.setString(2, job.getRequesterName());
            statement.setString(3, job.getTitle());
            statement.setString(4, job.getDescription());
            statement.setString(5, job.getStatus().name());
            statement.setLong(6, job.getTotalReward());
            statement.setLong(7, job.getRecruitmentDeadlineAt());
            statement.setLong(8, job.getWorkDeadlineAt());
            statement.setLong(9, job.getForceCompleteAt());
            statement.setLong(10, job.getCreatedAt());
            statement.setLong(11, job.getUpdatedAt());
            this.setNullableLong(statement, 12, job.getCompletedAt());
            this.setNullableLong(statement, 13, job.getCancelledAt());
            statement.setBoolean(14, job.isCompletedByForce());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to obtain generated job id");
                }
                return new Job(
                        keys.getLong(1),
                        job.getRequesterUuid(),
                        job.getRequesterName(),
                        job.getTitle(),
                        job.getDescription(),
                        job.getStatus(),
                        job.getTotalReward(),
                        job.getRecruitmentDeadlineAt(),
                        job.getWorkDeadlineAt(),
                        job.getForceCompleteAt(),
                        job.getCreatedAt(),
                        job.getUpdatedAt(),
                        job.getCompletedAt(),
                        job.getCancelledAt(),
                        job.isCompletedByForce()
                );
            }
        }
    }

    @Override
    public @NotNull Optional<Job> findById(final long id) throws SQLException {
        final String sql = "SELECT * FROM jobs WHERE id = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(this.map(resultSet));
            }
        }
    }

    @Override
    public @NotNull List<Job> findAll(final int limit, final int offset) throws SQLException {
        final String sql = "SELECT * FROM jobs ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return this.findMany(sql, statement -> {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
        });
    }

    @Override
    public @NotNull List<Job> findByRequester(final @NotNull UUID requesterUuid) throws SQLException {
        return this.findMany("SELECT * FROM jobs WHERE requester_uuid = ? ORDER BY created_at DESC", statement -> statement.setString(1, requesterUuid.toString()));
    }

    @Override
    public @NotNull List<Job> findByConfirmedMember(final @NotNull UUID playerUuid) throws SQLException {
        final String sql = "SELECT j.* FROM jobs j INNER JOIN job_members m ON m.job_id = j.id"
                + " WHERE m.player_uuid = ? AND m.role_status = ? AND j.status IN (?, ?, ?)"
                + " ORDER BY j.created_at DESC";
        return this.findMany(sql, statement -> {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, "CONFIRMED");
            statement.setString(3, JobStatus.OPEN.name());
            statement.setString(4, JobStatus.IN_PROGRESS.name());
            statement.setString(5, JobStatus.WORK_DEADLINE_PASSED.name());
        });
    }

    @Override
    public @NotNull List<Job> findBrowsableOpenJobs(final long now, final int limit, final int offset) throws SQLException {
        final String sql = "SELECT * FROM jobs WHERE status = ? AND recruitment_deadline_at >= ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return this.findMany(sql, statement -> {
            statement.setString(1, JobStatus.OPEN.name());
            statement.setLong(2, now);
            statement.setInt(3, limit);
            statement.setInt(4, offset);
        });
    }

    @Override
    public @NotNull List<Job> findRecruitmentDeadlinePassed(final long now) throws SQLException {
        final String sql = "SELECT * FROM jobs WHERE status = ? AND recruitment_deadline_at < ?";
        return this.findMany(sql, statement -> {
            statement.setString(1, JobStatus.OPEN.name());
            statement.setLong(2, now);
        });
    }

    @Override
    public @NotNull List<Job> findWorkDeadlinePassed(final long now) throws SQLException {
        final String sql = "SELECT * FROM jobs WHERE status IN (?, ?) AND work_deadline_at <= ?";
        return this.findMany(sql, statement -> {
            statement.setString(1, JobStatus.OPEN.name());
            statement.setString(2, JobStatus.IN_PROGRESS.name());
            statement.setLong(3, now);
        });
    }

    @Override
    public @NotNull List<Job> findForceCompletable(final long now) throws SQLException {
        final String sql = "SELECT * FROM jobs WHERE status IN (?, ?) AND force_complete_at <= ?";
        return this.findMany(sql, statement -> {
            statement.setString(1, JobStatus.IN_PROGRESS.name());
            statement.setString(2, JobStatus.WORK_DEADLINE_PASSED.name());
            statement.setLong(3, now);
        });
    }

    @Override
    public void updateDetails(final long jobId, final @NotNull String title, final @NotNull String description, final long totalReward, final long recruitmentDeadlineAt, final long workDeadlineAt, final long forceCompleteAt, final long updatedAt) throws SQLException {
        final String sql = "UPDATE jobs SET title = ?, description = ?, total_reward = ?, recruitment_deadline_at = ?, work_deadline_at = ?, force_complete_at = ?, updated_at = ? WHERE id = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, description);
            statement.setLong(3, totalReward);
            statement.setLong(4, recruitmentDeadlineAt);
            statement.setLong(5, workDeadlineAt);
            statement.setLong(6, forceCompleteAt);
            statement.setLong(7, updatedAt);
            statement.setLong(8, jobId);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateStatus(final long jobId, final @NotNull JobStatus status, final long updatedAt, final @Nullable Long completedAt, final @Nullable Long cancelledAt, final boolean completedByForce) throws SQLException {
        final String sql = "UPDATE jobs SET status = ?, updated_at = ?, completed_at = ?, cancelled_at = ?, completed_by_force = ? WHERE id = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setLong(2, updatedAt);
            this.setNullableLong(statement, 3, completedAt);
            this.setNullableLong(statement, 4, cancelledAt);
            statement.setBoolean(5, completedByForce);
            statement.setLong(6, jobId);
            statement.executeUpdate();
        }
    }

    private @NotNull List<Job> findMany(final @NotNull String sql, final @NotNull StatementConfigurer configurer) throws SQLException {
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<Job> jobs = new ArrayList<>();
                while (resultSet.next()) {
                    jobs.add(this.map(resultSet));
                }
                return jobs;
            }
        }
    }

    private @NotNull Job map(final @NotNull ResultSet resultSet) throws SQLException {
        return new Job(
                resultSet.getLong("id"),
                this.getUuid(resultSet, "requester_uuid"),
                resultSet.getString("requester_name"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                JobStatus.valueOf(resultSet.getString("status")),
                resultSet.getLong("total_reward"),
                resultSet.getLong("recruitment_deadline_at"),
                resultSet.getLong("work_deadline_at"),
                resultSet.getLong("force_complete_at"),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at"),
                this.getNullableLong(resultSet, "completed_at"),
                this.getNullableLong(resultSet, "cancelled_at"),
                resultSet.getBoolean("completed_by_force")
        );
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }
}
