package net.azisaba.buildingboard.repository.impl;

import net.azisaba.buildingboard.model.job.JobMember;
import net.azisaba.buildingboard.model.job.JobMemberStatus;
import net.azisaba.buildingboard.repository.JobMemberRepository;
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

public final class JdbcJobMemberRepository extends JdbcRepositorySupport implements JobMemberRepository {
    public JdbcJobMemberRepository(final @NotNull DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public @NotNull JobMember insert(final @NotNull JobMember member) throws SQLException {
        final String sql = "INSERT INTO job_members (job_id, player_uuid, player_name, role_status, joined_at, decided_at, left_at, decided_by_uuid, note)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, member.getJobId());
            statement.setString(2, member.getPlayerUuid().toString());
            statement.setString(3, member.getPlayerName());
            statement.setString(4, member.getStatus().name());
            statement.setLong(5, member.getJoinedAt());
            this.setNullableLong(statement, 6, member.getDecidedAt());
            this.setNullableLong(statement, 7, member.getLeftAt());
            this.setNullableUuid(statement, 8, member.getDecidedByUuid());
            this.setNullableString(statement, 9, member.getNote());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to obtain generated job member id");
                }
                return new JobMember(
                        keys.getLong(1),
                        member.getJobId(),
                        member.getPlayerUuid(),
                        member.getPlayerName(),
                        member.getStatus(),
                        member.getJoinedAt(),
                        member.getDecidedAt(),
                        member.getLeftAt(),
                        member.getDecidedByUuid(),
                        member.getNote()
                );
            }
        }
    }

    @Override
    public @NotNull Optional<JobMember> findByJobIdAndPlayerUuid(final long jobId, final @NotNull UUID playerUuid) throws SQLException {
        final String sql = "SELECT * FROM job_members WHERE job_id = ? AND player_uuid = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, jobId);
            statement.setString(2, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(this.map(resultSet));
            }
        }
    }

    @Override
    public @NotNull List<JobMember> findByJobId(final long jobId) throws SQLException {
        return this.findMany("SELECT * FROM job_members WHERE job_id = ? ORDER BY joined_at ASC", statement -> statement.setLong(1, jobId));
    }

    @Override
    public @NotNull List<JobMember> findByJobIdAndStatus(final long jobId, final @NotNull JobMemberStatus status) throws SQLException {
        return this.findMany("SELECT * FROM job_members WHERE job_id = ? AND role_status = ? ORDER BY joined_at ASC", statement -> {
            statement.setLong(1, jobId);
            statement.setString(2, status.name());
        });
    }

    @Override
    public int countByJobIdAndStatus(final long jobId, final @NotNull JobMemberStatus status) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM job_members WHERE job_id = ? AND role_status = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, jobId);
            statement.setString(2, status.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    @Override
    public void updateStatus(final long id, final @NotNull JobMemberStatus status, final @Nullable Long decidedAt, final @Nullable Long leftAt, final @Nullable UUID decidedByUuid, final @Nullable String note) throws SQLException {
        final String sql = "UPDATE job_members SET role_status = ?, decided_at = ?, left_at = ?, decided_by_uuid = ?, note = ? WHERE id = ?";
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.name());
            this.setNullableLong(statement, 2, decidedAt);
            this.setNullableLong(statement, 3, leftAt);
            this.setNullableUuid(statement, 4, decidedByUuid);
            this.setNullableString(statement, 5, note);
            statement.setLong(6, id);
            statement.executeUpdate();
        }
    }

    private @NotNull List<JobMember> findMany(final @NotNull String sql, final @NotNull StatementConfigurer configurer) throws SQLException {
        try (java.sql.Connection connection = this.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            configurer.configure(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                final List<JobMember> members = new ArrayList<>();
                while (resultSet.next()) {
                    members.add(this.map(resultSet));
                }
                return members;
            }
        }
    }

    private @NotNull JobMember map(final @NotNull ResultSet resultSet) throws SQLException {
        final String decidedBy = resultSet.getString("decided_by_uuid");
        return new JobMember(
                resultSet.getLong("id"),
                resultSet.getLong("job_id"),
                this.getUuid(resultSet, "player_uuid"),
                resultSet.getString("player_name"),
                JobMemberStatus.valueOf(resultSet.getString("role_status")),
                resultSet.getLong("joined_at"),
                this.getNullableLong(resultSet, "decided_at"),
                this.getNullableLong(resultSet, "left_at"),
                decidedBy == null ? null : UUID.fromString(decidedBy),
                resultSet.getString("note")
        );
    }

    @FunctionalInterface
    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws SQLException;
    }
}
