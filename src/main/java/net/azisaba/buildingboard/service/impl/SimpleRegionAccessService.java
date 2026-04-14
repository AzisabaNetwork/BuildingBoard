package net.azisaba.buildingboard.service.impl;

import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobMemberStatus;
import net.azisaba.buildingboard.model.job.JobRegion;
import net.azisaba.buildingboard.repository.JobMemberRepository;
import net.azisaba.buildingboard.repository.JobRegionRepository;
import net.azisaba.buildingboard.repository.JobRepository;
import net.azisaba.buildingboard.service.RegionAccessService;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class SimpleRegionAccessService implements RegionAccessService {
    private final @NotNull JobRepository jobRepository;
    private final @NotNull JobRegionRepository regionRepository;
    private final @NotNull JobMemberRepository memberRepository;

    public SimpleRegionAccessService(
            final @NotNull JobRepository jobRepository,
            final @NotNull JobRegionRepository regionRepository,
            final @NotNull JobMemberRepository memberRepository
    ) {
        this.jobRepository = jobRepository;
        this.regionRepository = regionRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public @NotNull Optional<Job> findProtectedJobAt(final @NotNull Location location) throws SQLException {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        final Optional<JobRegion> region = this.regionRepository.findActiveContaining(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
        if (!region.isPresent()) {
            return Optional.empty();
        }
        return this.jobRepository.findById(region.get().getJobId());
    }

    @Override
    public boolean canEdit(final @NotNull OfflinePlayer player, final @NotNull Block block) throws SQLException {
        if (this.hasAdminBypass(player)) {
            return true;
        }
        final Optional<Job> job = this.findProtectedJobAt(block.getLocation());
        return !job.isPresent() || this.isAllowedEditor(player.getUniqueId(), job.get());
    }

    @Override
    public boolean canUse(final @NotNull OfflinePlayer player, final @NotNull Block block) throws SQLException {
        return this.canEdit(player, block);
    }

    @Override
    public boolean isProtected(final @NotNull Block block) throws SQLException {
        return this.findProtectedJobAt(block.getLocation()).isPresent();
    }

    @Override
    public boolean isSameProtectedJob(final @NotNull Block first, final @NotNull Block second) throws SQLException {
        final Optional<Job> firstJob = this.findProtectedJobAt(first.getLocation());
        final Optional<Job> secondJob = this.findProtectedJobAt(second.getLocation());
        if (!firstJob.isPresent() || !secondJob.isPresent()) {
            return false;
        }
        return firstJob.get().getId() == secondJob.get().getId();
    }

    @Override
    public boolean isAllowedEditor(final @NotNull UUID playerUuid, final @NotNull Job job) throws SQLException {
        if (job.getRequesterUuid().equals(playerUuid)) {
            return true;
        }
        return this.memberRepository.findByJobIdAndPlayerUuid(job.getId(), playerUuid)
                .map(member -> member.getStatus() == JobMemberStatus.CONFIRMED)
                .orElse(false);
    }

    private boolean hasAdminBypass(final @NotNull OfflinePlayer player) {
        return player instanceof Player && ((Player) player).hasPermission("buildingboard.admin");
    }
}
