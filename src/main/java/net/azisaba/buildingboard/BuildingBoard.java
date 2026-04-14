package net.azisaba.buildingboard;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.azisaba.buildingboard.command.BuildingBoardCommand;
import net.azisaba.buildingboard.config.DatabaseSettings;
import net.azisaba.buildingboard.config.DefaultSettings;
import net.azisaba.buildingboard.gui.BuildingBoardDraftListener;
import net.azisaba.buildingboard.gui.BuildingBoardGuiListener;
import net.azisaba.buildingboard.gui.BuildingBoardGuiService;
import net.azisaba.buildingboard.listener.PlayerSessionListener;
import net.azisaba.buildingboard.listener.RegionProtectionListener;
import net.azisaba.buildingboard.repository.JobMemberRepository;
import net.azisaba.buildingboard.repository.JobNotificationRepository;
import net.azisaba.buildingboard.repository.JobRefundRepository;
import net.azisaba.buildingboard.repository.JobRegionRepository;
import net.azisaba.buildingboard.repository.JobRepository;
import net.azisaba.buildingboard.repository.impl.JdbcJobMemberRepository;
import net.azisaba.buildingboard.repository.impl.JdbcJobNotificationRepository;
import net.azisaba.buildingboard.repository.impl.JdbcJobRefundRepository;
import net.azisaba.buildingboard.repository.impl.JdbcJobRegionRepository;
import net.azisaba.buildingboard.repository.impl.JdbcJobRepository;
import net.azisaba.buildingboard.service.JobApplicationService;
import net.azisaba.buildingboard.service.JobCompletionService;
import net.azisaba.buildingboard.service.JobService;
import net.azisaba.buildingboard.service.NotificationService;
import net.azisaba.buildingboard.service.RegionAccessService;
import net.azisaba.buildingboard.service.RefundService;
import net.azisaba.buildingboard.service.impl.SimpleJobApplicationService;
import net.azisaba.buildingboard.service.impl.SimpleJobCompletionService;
import net.azisaba.buildingboard.service.impl.SimpleJobService;
import net.azisaba.buildingboard.service.impl.SimpleNotificationService;
import net.azisaba.buildingboard.service.impl.SimpleRegionAccessService;
import net.azisaba.buildingboard.service.impl.SimpleRefundService;
import net.azisaba.buildingboard.storage.DatabaseManager;
import net.azisaba.buildingboard.storage.SchemaInitializer;
import net.azisaba.buildingboard.task.DeadlineCheckTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

public class BuildingBoard extends JavaPlugin {
    private DatabaseManager databaseManager;
    private DefaultSettings defaultSettings;
    private NotificationService notificationService;
    private RefundService refundService;
    private JobService jobService;
    private JobApplicationService jobApplicationService;
    private JobCompletionService jobCompletionService;
    private RegionAccessService regionAccessService;
    private JobRepository jobRepository;
    private BuildingBoardGuiService guiService;
    private int deadlineTaskId = -1;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.defaultSettings = DefaultSettings.fromConfig(this.getConfig());

        try {
            final WorldEditPlugin worldEditPlugin = this.findWorldEdit();
            this.databaseManager = new DatabaseManager(DatabaseSettings.fromConfig(this.getConfig()));
            new SchemaInitializer(this.databaseManager.getDataSource()).initialize();
            this.jobRepository = new JdbcJobRepository(this.databaseManager.getDataSource());
            final JobRegionRepository regionRepository = new JdbcJobRegionRepository(this.databaseManager.getDataSource());
            final JobMemberRepository memberRepository = new JdbcJobMemberRepository(this.databaseManager.getDataSource());
            final JobNotificationRepository notificationRepository = new JdbcJobNotificationRepository(this.databaseManager.getDataSource());
            final JobRefundRepository refundRepository = new JdbcJobRefundRepository(this.databaseManager.getDataSource());
            final Economy economy = this.findEconomy();
            this.notificationService = new SimpleNotificationService(notificationRepository);
            this.refundService = new SimpleRefundService(refundRepository, this.notificationService, economy);
            this.jobService = new SimpleJobService(
                    this.jobRepository,
                    regionRepository,
                    memberRepository,
                    this.notificationService,
                    this.refundService,
                    economy,
                    this.defaultSettings.getForceCompleteExtraDays() * 24L * 60L * 60L * 1000L
            );
            this.jobApplicationService = new SimpleJobApplicationService(this.jobRepository, memberRepository, this.notificationService);
            this.jobCompletionService = new SimpleJobCompletionService(this.jobRepository, memberRepository, this.notificationService, this.refundService, economy);
            this.regionAccessService = new SimpleRegionAccessService(this.jobRepository, regionRepository, memberRepository);
            this.guiService = new BuildingBoardGuiService(this);
            final BuildingBoardCommand command = new BuildingBoardCommand(this, worldEditPlugin, this.guiService);
            this.getCommand("buildingboard").setExecutor(command);
            this.getCommand("buildingboard").setTabCompleter(command);
            this.getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
            this.getServer().getPluginManager().registerEvents(new RegionProtectionListener(this, this.regionAccessService), this);
            this.getServer().getPluginManager().registerEvents(new BuildingBoardGuiListener(), this);
            this.getServer().getPluginManager().registerEvents(new BuildingBoardDraftListener(this), this);
            this.startDeadlineTask();
        } catch (SQLException | RuntimeException e) {
            this.getLogger().severe("Failed to initialize database: " + e.getMessage());
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.deadlineTaskId != -1) {
            this.getServer().getScheduler().cancelTask(this.deadlineTaskId);
            this.deadlineTaskId = -1;
        }
        if (this.databaseManager != null) {
            this.databaseManager.close();
            this.databaseManager = null;
        }
    }

    public static @NotNull BuildingBoard getInstance() {
        return JavaPlugin.getPlugin(BuildingBoard.class);
    }

    public @NotNull DatabaseManager getDatabaseManager() {
        if (this.databaseManager == null) {
            throw new IllegalStateException("DatabaseManager is not initialized");
        }
        return this.databaseManager;
    }

    public @NotNull DefaultSettings getDefaultSettings() {
        return this.defaultSettings;
    }

    public @NotNull NotificationService getNotificationService() {
        return this.notificationService;
    }

    public @NotNull RefundService getRefundService() {
        return this.refundService;
    }

    public @NotNull JobService getJobService() {
        return this.jobService;
    }

    public @NotNull JobApplicationService getJobApplicationService() {
        return this.jobApplicationService;
    }

    public @NotNull JobCompletionService getJobCompletionService() {
        return this.jobCompletionService;
    }

    public @NotNull JobRepository getJobRepository() {
        return this.jobRepository;
    }

    public @NotNull RegionAccessService getRegionAccessService() {
        return this.regionAccessService;
    }

    public @NotNull BuildingBoardGuiService getGuiService() {
        return this.guiService;
    }

    public @NotNull WorldEditPlugin getWorldEditPlugin() {
        return this.findWorldEdit();
    }

    private @Nullable Economy findEconomy() {
        final RegisteredServiceProvider<Economy> provider = this.getServer().getServicesManager().getRegistration(Economy.class);
        return provider == null ? null : provider.getProvider();
    }

    private @NotNull WorldEditPlugin findWorldEdit() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("WorldEdit");
        if (!(plugin instanceof WorldEditPlugin)) {
            throw new IllegalStateException("WorldEdit is required");
        }
        return (WorldEditPlugin) plugin;
    }

    private void startDeadlineTask() {
        final long intervalTicks = Math.max(20L, this.defaultSettings.getDeadlineCheckIntervalSeconds() * 20L);
        this.deadlineTaskId = this.getServer().getScheduler().scheduleSyncRepeatingTask(
                this,
                new DeadlineCheckTask(this),
                intervalTicks,
                intervalTicks
        );
    }
}
