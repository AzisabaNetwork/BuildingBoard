package net.azisaba.buildingboard.service.impl;

import net.azisaba.buildingboard.model.notification.NotificationCategory;
import net.azisaba.buildingboard.model.refund.JobRefund;
import net.azisaba.buildingboard.model.refund.RefundReason;
import net.azisaba.buildingboard.model.refund.RefundStatus;
import net.azisaba.buildingboard.repository.JobRefundRepository;
import net.azisaba.buildingboard.service.NotificationService;
import net.azisaba.buildingboard.service.RefundService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public final class SimpleRefundService implements RefundService {
    private final @NotNull JobRefundRepository repository;
    private final @NotNull NotificationService notificationService;
    private final @Nullable Economy economy;

    public SimpleRefundService(
            final @NotNull JobRefundRepository repository,
            final @NotNull NotificationService notificationService,
            final @Nullable Economy economy
    ) {
        this.repository = repository;
        this.notificationService = notificationService;
        this.economy = economy;
    }

    @Override
    public @NotNull JobRefund enqueueRefund(final long jobId, final @NotNull UUID requesterUuid, final long amount, final @NotNull RefundReason reason) throws SQLException {
        return this.repository.insert(new JobRefund(0L, jobId, requesterUuid, amount, reason, RefundStatus.PENDING, System.currentTimeMillis(), null, null));
    }

    @Override
    public @NotNull List<JobRefund> getPendingRefunds(final @NotNull UUID requesterUuid) throws SQLException {
        return this.repository.findPendingByRequesterUuid(requesterUuid);
    }

    @Override
    public void processPendingRefunds(final @NotNull OfflinePlayer player) throws SQLException {
        final List<JobRefund> refunds = this.repository.findPendingByRequesterUuid(player.getUniqueId());
        if (refunds.isEmpty()) {
            return;
        }
        for (JobRefund refund : refunds) {
            if (this.economy == null) {
                this.repository.updateStatus(refund.getId(), RefundStatus.PENDING, null, "Economy is unavailable");
                continue;
            }
            if (this.economy.depositPlayer(player, refund.getAmount()).transactionSuccess()) {
                final long now = System.currentTimeMillis();
                this.repository.updateStatus(refund.getId(), RefundStatus.COMPLETED, now, null);
                this.notificationService.notify(
                        player.getUniqueId(),
                        NotificationCategory.REFUND_COMPLETED,
                        refund.getJobId(),
                        "返金が完了しました",
                        "依頼 #" + refund.getJobId() + " の返金 " + refund.getAmount() + " が処理されました。"
                );
            } else {
                this.repository.updateStatus(refund.getId(), RefundStatus.PENDING, null, "Vault deposit failed");
            }
        }
    }
}
