package net.azisaba.buildingboard.task;

import net.azisaba.buildingboard.BuildingBoard;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public final class DeadlineCheckTask implements Runnable {
    private final @NotNull BuildingBoard plugin;

    public DeadlineCheckTask(final @NotNull BuildingBoard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            this.plugin.getJobCompletionService().updateExpiredJobStatuses(System.currentTimeMillis());
        } catch (SQLException | IllegalStateException e) {
            this.plugin.getLogger().warning("Failed to run deadline check task: " + e.getMessage());
        }
    }
}
