package net.azisaba.buildingboard.command;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.azisaba.buildingboard.BuildingBoard;
import net.azisaba.buildingboard.config.DefaultSettings;
import net.azisaba.buildingboard.gui.BuildingBoardGuiService;
import net.azisaba.buildingboard.gui.draft.JobDraft;
import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobDetails;
import net.azisaba.buildingboard.model.job.JobRegion;
import net.azisaba.buildingboard.model.notification.JobNotification;
import net.azisaba.buildingboard.util.WorldEditSelectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class BuildingBoardCommand implements CommandExecutor, TabCompleter {
    private final @NotNull BuildingBoard plugin;
    private final @NotNull WorldEditPlugin worldEditPlugin;
    private final @NotNull BuildingBoardGuiService guiService;

    public BuildingBoardCommand(final @NotNull BuildingBoard plugin, final @NotNull WorldEditPlugin worldEditPlugin, final @NotNull BuildingBoardGuiService guiService) {
        this.plugin = plugin;
        this.worldEditPlugin = worldEditPlugin;
        this.guiService = guiService;
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender, label);
            return true;
        }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "create" -> this.handleCreate(sender, args);
                case "edit" -> this.handleEdit(sender, args);
                case "list" -> this.handleList(sender, args);
                case "lines" -> this.handleLines(sender, args);
                case "gui" -> this.handleGui(sender, args);
                case "my" -> this.handleMy(sender);
                case "info" -> this.handleInfo(sender, args);
                case "apply" -> this.handleApply(sender, args);
                case "approve" -> this.handleApprove(sender, args);
                case "decline" -> this.handleDecline(sender, args);
                case "add" -> this.handleAdd(sender, args);
                case "remove" -> this.handleRemove(sender, args);
                case "withdraw" -> this.handleWithdraw(sender, args);
                case "complete" -> this.handleComplete(sender, args);
                case "cancel" -> this.handleCancel(sender, args);
                case "notifications" -> this.handleNotifications(sender, args);
                case "readall" -> this.handleReadAll(sender);
                case "refunds" -> this.handleRefunds(sender);
                case "checkdeadlines" -> this.handleCheckDeadlines(sender);
                default -> this.sendHelp(sender, label);
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return true;
        } catch (SQLException e) {
            sender.sendMessage(ChatColor.RED + "データベースエラー: " + e.getMessage());
            this.plugin.getLogger().warning("Database error while handling command: " + e.getMessage());
            return true;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IllegalArgumentException || e.getCause() instanceof IllegalStateException) {
                sender.sendMessage(ChatColor.RED + e.getCause().getMessage());
                return true;
            }
            throw e;
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String alias, final @NotNull String[] args) {
        if (args.length == 1) {
            return this.filterPrefix(List.of(
                    "create", "edit", "list", "my", "info", "apply", "approve", "decline",
                    "add", "remove", "withdraw", "complete", "cancel", "notifications", "gui", "lines",
                    "readall", "refunds", "checkdeadlines"
            ), args[0]);
        }
        if (args.length == 2 && "lines".equalsIgnoreCase(args[0])) {
            return this.filterPrefix(List.of("add", "remove", "set", "list", "clear"), args[1]);
        }
        if (args.length == 3 && List.of("approve", "decline", "add", "remove").contains(args[0].toLowerCase(Locale.ROOT))) {
            return this.filterPrefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
        }
        return Collections.emptyList();
    }

    private void handleCreate(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 3) {
            throw new IllegalArgumentException("使い方: /bb create <reward> <title...>");
        }
        final long reward = this.parsePositiveLong(args[1], "reward");
        final String title = this.joinArgs(args, 2);
        final DefaultSettings defaults = this.plugin.getDefaultSettings();
        final long now = System.currentTimeMillis();
        final long recruitmentDeadlineAt = now + Duration.ofDays(defaults.getRecruitmentDeadlineDays()).toMillis();
        final long workDeadlineAt = now + Duration.ofDays(defaults.getWorkDeadlineDays()).toMillis();
        final Job created = this.plugin.getJobService().createJob(
                player,
                title,
                title,
                reward,
                recruitmentDeadlineAt,
                workDeadlineAt,
                this.readSelectedRegion(player)
        );
        sender.sendMessage(ChatColor.GREEN + "WorldEditで選択した範囲で依頼 #" + created.getId() + " を作成しました。");
    }

    private void handleEdit(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 5) {
            throw new IllegalArgumentException("使い方: /bb edit <jobId> <recruitDays> <workDays> <title...>");
        }
        final long jobId = this.parsePositiveLong(args[1], "job id");
        final long recruitDays = this.parsePositiveLong(args[2], "recruit days");
        final long workDays = this.parsePositiveLong(args[3], "work days");
        final String title = this.joinArgs(args, 4);
        final long now = System.currentTimeMillis();
        this.plugin.getJobService().updateJobDetails(
                jobId,
                player,
                title,
                title,
                this.plugin.getJobService().getJobDetails(jobId).getJob().getTotalReward(),
                now + Duration.ofDays(recruitDays).toMillis(),
                now + Duration.ofDays(workDays).toMillis()
        );
        sender.sendMessage(ChatColor.GREEN + "依頼 #" + jobId + " を更新しました。");
    }

    private void handleList(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final int page = args.length >= 2 ? (int) this.parsePositiveLong(args[1], "page") : 1;
        final int offset = (page - 1) * 10;
        final List<Job> jobs = this.plugin.getJobService().getBrowsableOpenJobs(System.currentTimeMillis(), 10, offset);
        sender.sendMessage(ChatColor.GOLD + "募集中の依頼 " + page + " ページ目:");
        if (jobs.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "表示できる依頼はありません。");
            return;
        }
        for (Job job : jobs) {
            sender.sendMessage(ChatColor.YELLOW + "#" + job.getId() + ChatColor.WHITE + " " + job.getTitle() + ChatColor.GRAY + " 報酬=" + job.getTotalReward());
        }
    }

    private void handleLines(final @NotNull CommandSender sender, final @NotNull String[] args) {
        final Player player = this.requirePlayer(sender);
        if (args.length < 2) {
            throw new IllegalArgumentException("使い方: /bb lines <add|remove|set|list|clear> ...");
        }
        final JobDraft draft = this.guiService.getActiveDraft(player.getUniqueId());
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add":
                if (args.length < 3) {
                    throw new IllegalArgumentException("使い方: /bb lines add <message>");
                }
                draft.addDescriptionLine(this.joinArgs(args, 2));
                sender.sendMessage(ChatColor.GREEN + "説明文の " + draft.getDescriptionLines().size() + " 行目を追加しました。");
                this.guiService.reopenDraftEditor(player);
                return;
            case "remove":
                if (args.length < 3) {
                    throw new IllegalArgumentException("使い方: /bb lines remove <number>");
                }
                draft.removeDescriptionLine(this.requireLineIndex(draft, args[2]));
                sender.sendMessage(ChatColor.GREEN + "説明文の行を削除しました。");
                this.guiService.reopenDraftEditor(player);
                return;
            case "set":
                if (args.length < 4) {
                    throw new IllegalArgumentException("使い方: /bb lines set <number> <message>");
                }
                draft.setDescriptionLine(this.requireLineIndex(draft, args[2]), this.joinArgs(args, 3));
                sender.sendMessage(ChatColor.GREEN + "説明文の行を更新しました。");
                this.guiService.reopenDraftEditor(player);
                return;
            case "list":
                sender.sendMessage(ChatColor.GOLD + "説明文の行一覧:");
                if (draft.getDescriptionLines().isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "説明文はまだ設定されていません。");
                }
                for (int i = 0; i < draft.getDescriptionLines().size(); i++) {
                    sender.sendMessage(ChatColor.YELLOW + String.valueOf(i + 1) + ChatColor.WHITE + ": " + draft.getDescriptionLines().get(i));
                }
                return;
            case "clear":
                draft.clearDescriptionLines();
                sender.sendMessage(ChatColor.GREEN + "説明文の行をすべて削除しました。");
                this.guiService.reopenDraftEditor(player);
                return;
            default:
                throw new IllegalArgumentException("使い方: /bb lines <add|remove|set|list|clear> ...");
        }
    }

    private void handleGui(final @NotNull CommandSender sender, final @NotNull String[] args) {
        final Player player = this.requirePlayer(sender);
        if (args.length == 1) {
            this.guiService.openMainMenu(player);
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create":
                this.guiService.openCreateDraft(player);
                return;
            case "jobs":
                this.guiService.openOpenJobs(player, 0);
                return;
            case "edit":
                if (args.length < 3) {
                    throw new IllegalArgumentException("使い方: /bb gui edit <jobId>");
                }
                this.guiService.openEditDraft(player, this.parsePositiveLong(args[2], "job id"));
                return;
            case "my":
                this.guiService.openMyJobs(player, 0);
                return;
            case "current":
                this.guiService.openCurrentContractorJobs(player, 0);
                return;
            case "notifications":
                this.guiService.openNotifications(player, true, 0);
                return;
            default:
                throw new IllegalArgumentException("使い方: /bb gui [create|jobs|edit <id>|my|current|notifications]");
        }
    }

    private void handleMy(final @NotNull CommandSender sender) throws SQLException {
        final OfflinePlayer player = this.requireOfflinePlayer(sender);
        final List<Job> jobs = this.plugin.getJobRepository().findByRequester(player.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + "自分の依頼一覧:");
        if (jobs.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "依頼はありません。");
            return;
        }
        for (Job job : jobs) {
            sender.sendMessage(ChatColor.YELLOW + "#" + job.getId() + ChatColor.WHITE + " " + job.getTitle() + ChatColor.GRAY + " 状態=" + job.getStatus());
        }
    }

    private void handleInfo(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        if (args.length < 2) {
            throw new IllegalArgumentException("使い方: /bb info <jobId>");
        }
        final JobDetails details = this.plugin.getJobService().getJobDetails(this.parsePositiveLong(args[1], "job id"));
        sender.sendMessage(ChatColor.GOLD + "依頼 #" + details.getJob().getId() + " " + details.getJob().getTitle());
        sender.sendMessage(ChatColor.GRAY + "状態: " + details.getJob().getStatus() + " 報酬総額: " + details.getJob().getTotalReward());
        sender.sendMessage(ChatColor.GRAY + "確定施工者: " + details.getConfirmedCount() + " 応募者: " + details.getAppliedCount());
        sender.sendMessage(ChatColor.GRAY + "1人あたり: " + details.getRewardPerConfirmedMember() + " 端数返金: " + details.getRewardRemainder());
        sender.sendMessage(ChatColor.GRAY + "説明:");
        final List<String> descriptionLines = Arrays.asList(details.getJob().getDescription().split("\n", -1));
        if (descriptionLines.isEmpty() || (descriptionLines.size() == 1 && descriptionLines.get(0).isEmpty())) {
            sender.sendMessage(ChatColor.DARK_GRAY + "  (説明なし)");
        } else {
            for (String line : descriptionLines) {
                sender.sendMessage(ChatColor.DARK_GRAY + "  " + line);
            }
        }
        sender.sendMessage(ChatColor.GRAY + "範囲: " + details.getRegion().getWorldName()
                + " [" + details.getRegion().getMinX() + "," + details.getRegion().getMinY() + "," + details.getRegion().getMinZ()
                + "] -> [" + details.getRegion().getMaxX() + "," + details.getRegion().getMaxY() + "," + details.getRegion().getMaxZ() + "]");
    }

    private void handleApply(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 2) {
            throw new IllegalArgumentException("使い方: /bb apply <jobId>");
        }
        this.plugin.getJobApplicationService().apply(this.parsePositiveLong(args[1], "job id"), player);
        sender.sendMessage(ChatColor.GREEN + "依頼 #" + args[1] + " に応募しました。");
    }

    private void handleApprove(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 3) {
            throw new IllegalArgumentException("使い方: /bb approve <jobId> <player>");
        }
        this.plugin.getJobApplicationService().approve(this.parsePositiveLong(args[1], "job id"), player, this.requireTarget(args[2]));
        sender.sendMessage(ChatColor.GREEN + args[2] + " を依頼 #" + args[1] + " の施工者として承認しました。");
    }

    private void handleDecline(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 3) {
            throw new IllegalArgumentException("使い方: /bb decline <jobId> <player>");
        }
        this.plugin.getJobApplicationService().decline(this.parsePositiveLong(args[1], "job id"), player, this.requireTarget(args[2]));
        sender.sendMessage(ChatColor.GREEN + args[2] + " の依頼 #" + args[1] + " への応募を却下しました。");
    }

    private void handleAdd(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 3) {
            throw new IllegalArgumentException("使い方: /bb add <jobId> <player>");
        }
        this.plugin.getJobApplicationService().addConfirmedMemberAfterDeadline(this.parsePositiveLong(args[1], "job id"), player, this.requireTarget(args[2]));
        sender.sendMessage(ChatColor.GREEN + args[2] + " を依頼 #" + args[1] + " の施工者に追加しました。");
    }

    private void handleRemove(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 3) {
            throw new IllegalArgumentException("使い方: /bb remove <jobId> <player>");
        }
        this.plugin.getJobApplicationService().removeConfirmedMember(this.parsePositiveLong(args[1], "job id"), player, this.requireTarget(args[2]));
        sender.sendMessage(ChatColor.GREEN + args[2] + " を依頼 #" + args[1] + " の施工者から外しました。");
    }

    private void handleWithdraw(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 2) {
            throw new IllegalArgumentException("使い方: /bb withdraw <jobId>");
        }
        this.plugin.getJobApplicationService().withdraw(this.parsePositiveLong(args[1], "job id"), player);
        sender.sendMessage(ChatColor.GREEN + "依頼 #" + args[1] + " から辞退しました。");
    }

    private void handleComplete(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 2) {
            throw new IllegalArgumentException("使い方: /bb complete <jobId>");
        }
        this.plugin.getJobCompletionService().complete(this.parsePositiveLong(args[1], "job id"), player);
        sender.sendMessage(ChatColor.GREEN + "依頼 #" + args[1] + " を完了にしました。");
    }

    private void handleCancel(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final Player player = this.requirePlayer(sender);
        if (args.length < 2) {
            throw new IllegalArgumentException("使い方: /bb cancel <jobId>");
        }
        this.plugin.getJobService().cancelJob(this.parsePositiveLong(args[1], "job id"), player);
        sender.sendMessage(ChatColor.GREEN + "依頼 #" + args[1] + " を取り消しました。");
    }

    private void handleNotifications(final @NotNull CommandSender sender, final @NotNull String[] args) throws SQLException {
        final OfflinePlayer player = this.requireOfflinePlayer(sender);
        final boolean unreadOnly = args.length < 2 || !"all".equalsIgnoreCase(args[1]);
        final List<JobNotification> notifications = this.plugin.getNotificationService().getNotifications(player.getUniqueId(), unreadOnly, 10, 0);
        sender.sendMessage(ChatColor.GOLD + (unreadOnly ? "未読通知:" : "最近の通知:"));
        if (notifications.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "通知はありません。");
            return;
        }
        for (JobNotification notification : notifications) {
            sender.sendMessage(ChatColor.YELLOW + "[" + notification.getId() + "] " + ChatColor.WHITE + notification.getTitle() + ChatColor.GRAY + " - " + notification.getBody());
        }
    }

    private void handleReadAll(final @NotNull CommandSender sender) throws SQLException {
        final OfflinePlayer player = this.requireOfflinePlayer(sender);
        this.plugin.getNotificationService().markAllAsRead(player.getUniqueId(), System.currentTimeMillis());
        sender.sendMessage(ChatColor.GREEN + "通知をすべて既読にしました。");
    }

    private void handleRefunds(final @NotNull CommandSender sender) throws SQLException {
        final OfflinePlayer player = this.requireOfflinePlayer(sender);
        sender.sendMessage(ChatColor.GOLD + "未処理の返金一覧:");
        if (this.plugin.getRefundService().getPendingRefunds(player.getUniqueId()).isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "未処理の返金はありません。");
            return;
        }
        this.plugin.getRefundService().getPendingRefunds(player.getUniqueId()).forEach(refund ->
                sender.sendMessage(ChatColor.YELLOW + "#" + refund.getId() + ChatColor.WHITE + " 依頼=" + refund.getJobId() + ChatColor.GRAY + " 金額=" + refund.getAmount() + " 理由=" + refund.getReason())
        );
    }

    private void handleCheckDeadlines(final @NotNull CommandSender sender) throws SQLException {
        if (!sender.isOp()) {
            throw new IllegalStateException("このコマンドはOPのみ実行できます。");
        }
        this.plugin.getJobCompletionService().updateExpiredJobStatuses(System.currentTimeMillis());
        sender.sendMessage(ChatColor.GREEN + "期限チェックを実行しました。");
    }

    private void sendHelp(final @NotNull CommandSender sender, final @NotNull String label) {
        sender.sendMessage(ChatColor.GOLD + "BuildingBoard コマンド一覧:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " gui [create|jobs|edit <id>|my|current|notifications]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " lines <add|remove|set|list|clear> ...");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " create <reward> <title...>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " edit <jobId> <recruitDays> <workDays> <title...>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list [page]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " my");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info <jobId>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " apply|approve|decline|add|remove|withdraw");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " complete|cancel");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " notifications [all] | readall | refunds");
    }

    private @NotNull Player requirePlayer(final @NotNull CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new IllegalStateException("このコマンドはプレイヤーのみ実行できます。");
        }
        return (Player) sender;
    }

    private @NotNull OfflinePlayer requireOfflinePlayer(final @NotNull CommandSender sender) {
        if (sender instanceof OfflinePlayer) {
            return (OfflinePlayer) sender;
        }
        throw new IllegalStateException("このコマンドはプレイヤーとして実行する必要があります。");
    }

    private @NotNull OfflinePlayer requireTarget(final @NotNull String name) {
        // TODO: target is always non-null
        final OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (target.getUniqueId() == null) {
            throw new IllegalStateException("プレイヤーが見つかりません: " + name);
        }
        return target;
    }

    private @NotNull String joinArgs(final @NotNull String[] args, final int startIndex) {
        final List<String> parts = new ArrayList<>(Arrays.asList(args).subList(startIndex, args.length));
        return String.join(" ", parts);
    }

    private long parsePositiveLong(final @NotNull String raw, final @NotNull String name) {
        final long value;
        try {
            value = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " の値が不正です: " + raw);
        }
        if (value <= 0L) {
            throw new IllegalArgumentException(name + " は1以上である必要があります。");
        }
        return value;
    }

    private int requireLineIndex(final @NotNull JobDraft draft, final @NotNull String raw) {
        final long number = this.parsePositiveLong(raw, "line number");
        final int index = (int) number - 1;
        if (index < 0 || index >= draft.getDescriptionLines().size()) {
            throw new IllegalArgumentException("行番号が範囲外です。");
        }
        return index;
    }

    private @NotNull JobRegion readSelectedRegion(final @NotNull Player player) {
        try {
            return WorldEditSelectionUtil.requireSelectedCuboid(this.worldEditPlugin, player);
        } catch (IncompleteRegionException e) {
            throw new IllegalStateException("先にWorldEditで直方体範囲を選択してください。");
        }
    }

    private @NotNull List<String> filterPrefix(final @NotNull List<String> values, final @NotNull String prefix) {
        final String lowered = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowered))
                .toList();
    }
}
