package net.azisaba.buildingboard.repository.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

abstract class JdbcRepositorySupport {
    private final @NotNull DataSource dataSource;

    JdbcRepositorySupport(final @NotNull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected @NotNull DataSource getDataSource() {
        return this.dataSource;
    }

    protected @NotNull UUID getUuid(final @NotNull ResultSet resultSet, final @NotNull String column) throws SQLException {
        return UUID.fromString(resultSet.getString(column));
    }

    protected @Nullable Long getNullableLong(final @NotNull ResultSet resultSet, final @NotNull String column) throws SQLException {
        final long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    protected void setNullableLong(final @NotNull PreparedStatement statement, final int index, final @Nullable Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value);
    }

    protected void setNullableString(final @NotNull PreparedStatement statement, final int index, final @Nullable String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
            return;
        }
        statement.setString(index, value);
    }

    protected void setNullableUuid(final @NotNull PreparedStatement statement, final int index, final @Nullable UUID value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
            return;
        }
        statement.setString(index, value.toString());
    }
}
