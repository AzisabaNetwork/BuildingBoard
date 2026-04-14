package net.azisaba.buildingboard.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public final class DatabaseSettings {
    private final @NotNull String host;
    private final int port;
    private final @NotNull String databaseName;
    private final @NotNull String username;
    private final @NotNull String password;
    private final boolean useSsl;
    private final @NotNull String connectionParameters;

    public DatabaseSettings(
            final @NotNull String host,
            final int port,
            final @NotNull String databaseName,
            final @NotNull String username,
            final @NotNull String password,
            final boolean useSsl,
            final @NotNull String connectionParameters
    ) {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.useSsl = useSsl;
        this.connectionParameters = connectionParameters;
    }

    @SuppressWarnings("DataFlowIssue") // definitely non-null
    public static @NotNull DatabaseSettings fromConfig(final @NotNull FileConfiguration config) {
        return new DatabaseSettings(
                config.getString("database.host", "127.0.0.1"),
                config.getInt("database.port", 3306),
                config.getString("database.name", "buildingboard"),
                config.getString("database.username", "root"),
                config.getString("database.password", "password"),
                config.getBoolean("database.use-ssl", false),
                config.getString("database.connection-parameters", "?characterEncoding=utf8&useUnicode=true&serverTimezone=UTC")
        );
    }

    public @NotNull String getJdbcUrl() {
        final String sslMode = this.useSsl ? "true" : "false";
        final String parameters = this.connectionParameters.startsWith("?")
                ? this.connectionParameters
                : "?" + this.connectionParameters;
        return "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.databaseName
                + parameters + "&useSSL=" + sslMode;
    }

    public @NotNull String getUsername() {
        return this.username;
    }

    public @NotNull String getPassword() {
        return this.password;
    }
}
