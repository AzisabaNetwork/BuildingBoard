package net.azisaba.buildingboard.gui;

import net.azisaba.buildingboard.BuildingBoard;
import net.azisaba.buildingboard.gui.draft.JobDraft;
import net.azisaba.buildingboard.gui.draft.JobDraftField;
import net.azisaba.buildingboard.gui.draft.JobDraftMode;
import net.azisaba.buildingboard.gui.draft.PendingDraftInput;
import net.azisaba.buildingboard.model.job.Job;
import net.azisaba.buildingboard.model.job.JobDetails;
import net.azisaba.buildingboard.model.job.JobMember;
import net.azisaba.buildingboard.model.job.JobMemberStatus;
import net.azisaba.buildingboard.model.job.JobRegion;
import net.azisaba.buildingboard.model.job.JobStatus;
import net.azisaba.buildingboard.model.notification.JobNotification;
import net.azisaba.buildingboard.util.MathUtil;
import net.azisaba.buildingboard.util.WorldEditSelectionUtil;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BuildingBoardGuiService {
    private final @NotNull BuildingBoard plugin;
    private final @NotNull Map<UUID, JobDraft> drafts = new HashMap<>();
    private final @NotNull Map<UUID, PendingDraftInput> pendingInputs = new HashMap<>();

    public BuildingBoardGuiService(final @NotNull BuildingBoard plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(final @NotNull Player player) {
        final GuiHolder holder = new GuiHolder(27, ChatColor.DARK_GREEN + "BuildingBoard");
        final Inventory inventory = holder.getInventory();
        inventory.setItem(10, ItemStackFactory.create(Material.ANVIL, "依頼を作成", List.of("依頼作成GUIを開きます。")));
        inventory.setItem(12, ItemStackFactory.create(Material.MAP, "募集中の依頼", List.of("募集中の依頼を表示します。")));
        inventory.setItem(14, ItemStackFactory.create(Material.BOOK, "自分の依頼", List.of("自分が発注した依頼を表示します。")));
        inventory.setItem(16, ItemStackFactory.create(Material.BELL, "通知", List.of("未読通知を確認します。")));
        inventory.setItem(18, ItemStackFactory.create(Material.CHEST_MINECART, "すべての依頼", List.of("新しい順ですべての依頼を表示します。")));
        inventory.setItem(22, ItemStackFactory.create(Material.DIAMOND_PICKAXE, "受注中の依頼", List.of("自分が施工中の依頼を表示します。")));
        holder.setAction(10, (viewer, clickType) -> this.openCreateDraft(viewer));
        holder.setAction(12, (viewer, clickType) -> this.openOpenJobs(viewer, 0));
        holder.setAction(14, (viewer, clickType) -> this.openMyJobs(viewer, 0));
        holder.setAction(16, (viewer, clickType) -> this.openNotifications(viewer, true, 0));
        holder.setAction(18, (viewer, clickType) -> this.openAllJobs(viewer, 0));
        holder.setAction(22, (viewer, clickType) -> this.openCurrentContractorJobs(viewer, 0));
        player.openInventory(inventory);
    }

    public void openCreateDraft(final @NotNull Player player) {
        final JobDraft draft = new JobDraft(
                JobDraftMode.CREATE,
                0L,
                "新しい依頼",
                Collections.singletonList("建築内容を記入してください"),
                1000L,
                this.plugin.getDefaultSettings().getRecruitmentDeadlineDays(),
                this.plugin.getDefaultSettings().getWorkDeadlineDays(),
                null
        );
        this.drafts.put(player.getUniqueId(), draft);
        this.openDraftEditor(player);
    }

    public void openEditDraft(final @NotNull Player player, final long jobId) {
        this.runPlayerAction(player, () -> {
            final JobDetails details = this.plugin.getJobService().getJobDetails(jobId);
            final long now = System.currentTimeMillis();
            final int recruitmentDays = Math.max(1, (int) Math.ceil((details.getJob().getRecruitmentDeadlineAt() - now) / (double) Duration.ofDays(1).toMillis()));
            final int workDays = Math.max(1, (int) Math.ceil((details.getJob().getWorkDeadlineAt() - now) / (double) Duration.ofDays(1).toMillis()));
            this.drafts.put(player.getUniqueId(), new JobDraft(
                    JobDraftMode.EDIT,
                    jobId,
                    details.getJob().getTitle(),
                    this.splitDescription(details.getJob().getDescription()),
                    details.getJob().getTotalReward(),
                    recruitmentDays,
                    workDays,
                    details.getRegion()
            ));
            this.openDraftEditor(player);
        });
    }

    public void openOpenJobs(final @NotNull Player player, final int offset) {
        try {
            final List<Job> jobs = this.plugin.getJobService().getBrowsableOpenJobs(System.currentTimeMillis(), 28, offset);
            final GuiHolder holder = new GuiHolder(45, ChatColor.GREEN + "募集中の依頼");
            final Inventory inventory = holder.getInventory();
            this.fillJobList(inventory, holder, player, jobs, false);
            this.decoratePaging(inventory, holder, offset, jobs.size(), ViewKind.OPEN_JOBS);
            inventory.setItem(40, ItemStackFactory.create(Material.COMPASS, "メインメニュー", List.of("メインメニューに戻ります。")));
            holder.setAction(40, (viewer, clickType) -> this.openMainMenu(viewer));
            player.openInventory(inventory);
        } catch (SQLException e) {
            this.handleError(player, e);
        }
    }

    public void openMyJobs(final @NotNull Player player, final int offset) {
        try {
            final List<Job> jobs = this.plugin.getJobRepository().findByRequester(player.getUniqueId());
            final int toIndex = Math.min(offset + 28, jobs.size());
            final List<Job> page = offset >= jobs.size() ? Collections.emptyList() : jobs.subList(offset, toIndex);
            final GuiHolder holder = new GuiHolder(45, ChatColor.AQUA + "自分の依頼");
            final Inventory inventory = holder.getInventory();
            this.fillJobList(inventory, holder, player, page, true);
            this.decoratePaging(inventory, holder, offset, page.size(), ViewKind.MY_JOBS);
            inventory.setItem(40, ItemStackFactory.create(Material.COMPASS, "メインメニュー", List.of("メインメニューに戻ります。")));
            holder.setAction(40, (viewer, clickType) -> this.openMainMenu(viewer));
            player.openInventory(inventory);
        } catch (SQLException e) {
            this.handleError(player, e);
        }
    }

    public void openCurrentContractorJobs(final @NotNull Player player, final int offset) {
        try {
            final List<Job> jobs = this.plugin.getJobService().getCurrentContractorJobs(player);
            final int toIndex = Math.min(offset + 28, jobs.size());
            final List<Job> page = offset >= jobs.size() ? Collections.emptyList() : jobs.subList(offset, toIndex);
            final GuiHolder holder = new GuiHolder(45, ChatColor.BLUE + "受注中の依頼");
            final Inventory inventory = holder.getInventory();
            this.fillJobList(inventory, holder, player, page, false);
            this.decoratePaging(inventory, holder, offset, page.size(), ViewKind.CURRENT_CONTRACTOR);
            inventory.setItem(40, ItemStackFactory.create(Material.COMPASS, "メインメニュー", List.of("メインメニューに戻ります。")));
            holder.setAction(40, (viewer, clickType) -> this.openMainMenu(viewer));
            player.openInventory(inventory);
        } catch (SQLException e) {
            this.handleError(player, e);
        }
    }

    public void openAllJobs(final @NotNull Player player, final int offset) {
        try {
            final List<Job> jobs = this.plugin.getJobService().getAllJobs(28, offset);
            final GuiHolder holder = new GuiHolder(45, ChatColor.GOLD + "すべての依頼");
            final Inventory inventory = holder.getInventory();
            this.fillJobList(inventory, holder, player, jobs, false);
            this.decoratePaging(inventory, holder, offset, jobs.size(), ViewKind.ALL_JOBS);
            inventory.setItem(40, ItemStackFactory.create(Material.COMPASS, "メインメニュー", List.of("メインメニューに戻ります。")));
            holder.setAction(40, (viewer, clickType) -> this.openMainMenu(viewer));
            player.openInventory(inventory);
        } catch (SQLException e) {
            this.handleError(player, e);
        }
    }

    public void openJobDetails(final @NotNull Player player, final long jobId, final boolean fromMyJobs) {
        try {
            final JobDetails details = this.plugin.getJobService().getJobDetails(jobId);
            final GuiHolder holder = new GuiHolder(54, ChatColor.YELLOW + "依頼 #" + jobId);
            final Inventory inventory = holder.getInventory();
            final Job job = details.getJob();
            inventory.setItem(4, ItemStackFactory.create(Material.WRITABLE_BOOK, job.getTitle(), List.of(
                    "状態: " + job.getStatus().getDisplayName(),
                    "報酬総額: " + MathUtil.toFullPriceString(job.getTotalReward()),
                    "1人あたり: " + MathUtil.toFullPriceString(details.getRewardPerConfirmedMember()),
                    "端数返金: " + MathUtil.toFullPriceString(details.getRewardRemainder()),
                    "募集期限まで: " + this.formatTime(job.getRecruitmentDeadlineAt()),
                    "作業期限まで: " + this.formatTime(job.getWorkDeadlineAt())
            )));
            inventory.setItem(13, ItemStackFactory.create(Material.GRASS_BLOCK, "範囲", List.of(
                    details.getRegion().getWorldName(),
                    details.getRegion().getMinX() + ", " + details.getRegion().getMinY() + ", " + details.getRegion().getMinZ(),
                    details.getRegion().getMaxX() + ", " + details.getRegion().getMaxY() + ", " + details.getRegion().getMaxZ()
            )));
            inventory.setItem(21, ItemStackFactory.create(Material.BOOK, "依頼の説明", this.createDescriptionLore(job.getDescription(), 6)));
            inventory.setItem(22, ItemStackFactory.create(Material.GOLD_INGOT, "報酬分配", List.of(
                    "確定施工者数: " + details.getConfirmedCount(),
                    "1人あたり: " + MathUtil.toFullPriceString(details.getRewardPerConfirmedMember()),
                    "端数返金: " + MathUtil.toFullPriceString(details.getRewardRemainder())
            )));

            if (this.canApply(player, details)) {
                inventory.setItem(30, ItemStackFactory.create(Material.EMERALD, "応募", List.of("この依頼へ応募します。", "確認画面を開きます。")));
                holder.setAction(30, (viewer, clickType) -> this.openApplyConfirm(viewer, jobId, fromMyJobs));
            }
            if (this.isRequesterOrAdmin(player, job)) {
                inventory.setItem(31, ItemStackFactory.create(Material.CHEST, "応募者一覧", List.of("応募者を確認します。")));
                holder.setAction(31, (viewer, clickType) -> this.openApplicantList(viewer, jobId, fromMyJobs));
                inventory.setItem(29, ItemStackFactory.create(Material.ANVIL, "依頼を編集", List.of("タイトル、説明、期限を編集します。")));
                holder.setAction(29, (viewer, clickType) -> this.openEditDraft(viewer, jobId));
                inventory.setItem(32, ItemStackFactory.create(Material.BARRIER, "依頼を取り消す", List.of("依頼を取り消します。")));
                holder.setAction(32, (viewer, clickType) -> this.runPlayerAction(viewer, () -> {
                    this.plugin.getJobService().cancelJob(jobId, viewer);
                    viewer.closeInventory();
                    viewer.sendMessage(ChatColor.GREEN + "依頼 #" + jobId + " を取り消しました。");
                }));
                if (job.getStatus() == JobStatus.IN_PROGRESS || job.getStatus() == JobStatus.WORK_DEADLINE_PASSED) {
                    inventory.setItem(33, ItemStackFactory.create(Material.DIAMOND_BLOCK, "依頼を完了", List.of("依頼を完了状態にします。")));
                    holder.setAction(33, (viewer, clickType) -> this.runPlayerAction(viewer, () -> {
                        this.plugin.getJobCompletionService().complete(jobId, viewer);
                        viewer.closeInventory();
                        viewer.sendMessage(ChatColor.GREEN + "依頼 #" + jobId + " を完了にしました。");
                    }));
                }
            } else if (this.isConfirmedMember(player, details)) {
                inventory.setItem(31, ItemStackFactory.create(Material.REDSTONE, "辞退する", List.of("この依頼から辞退します。")));
                holder.setAction(31, (viewer, clickType) -> this.openWithdrawConfirm(viewer, jobId, fromMyJobs));
            }

            inventory.setItem(49, ItemStackFactory.create(Material.ARROW, "戻る", List.of("前の画面に戻ります。")));
            holder.setAction(49, (viewer, clickType) -> {
                if (fromMyJobs) {
                    this.openMyJobs(viewer, 0);
                } else {
                    this.openOpenJobs(viewer, 0);
                }
            });
            player.openInventory(inventory);
        } catch (SQLException e) {
            this.handleError(player, e);
        }
    }

    public void openApplyConfirm(final @NotNull Player player, final long jobId, final boolean fromMyJobs) {
        final GuiHolder holder = new GuiHolder(27, ChatColor.GREEN + "応募確認");
        final Inventory inventory = holder.getInventory();
        inventory.setItem(11, ItemStackFactory.create(Material.EMERALD_BLOCK, "応募する", List.of("発注者へ通知が送られます。")));
        inventory.setItem(15, ItemStackFactory.create(Material.BARRIER, "戻る", List.of("前の画面に戻ります。")));
        holder.setAction(11, (viewer, clickType) -> {
            try {
                this.plugin.getJobApplicationService().apply(jobId, viewer);
                viewer.closeInventory();
                viewer.sendMessage(ChatColor.GREEN + "依頼 #" + jobId + " に応募しました。");
            } catch (SQLException e) {
                this.handleError(viewer, e);
            }
        });
        holder.setAction(15, (viewer, clickType) -> this.openJobDetails(viewer, jobId, fromMyJobs));
        player.openInventory(inventory);
    }

    public void openWithdrawConfirm(final @NotNull Player player, final long jobId, final boolean fromMyJobs) {
        final GuiHolder holder = new GuiHolder(27, ChatColor.RED + "辞退確認");
        final Inventory inventory = holder.getInventory();
        inventory.setItem(11, ItemStackFactory.create(Material.REDSTONE_BLOCK, "辞退する", List.of(
                "この依頼から辞退します。",
                "辞退すると報酬は受け取れなくなります。"
        )));
        inventory.setItem(15, ItemStackFactory.create(Material.BARRIER, "戻る", List.of("前の画面に戻ります。")));
        holder.setAction(11, (viewer, clickType) -> this.runPlayerAction(viewer, () -> {
            this.plugin.getJobApplicationService().withdraw(jobId, viewer);
            viewer.closeInventory();
            viewer.sendMessage(ChatColor.GREEN + "依頼 #" + jobId + " から辞退しました。");
        }));
        holder.setAction(15, (viewer, clickType) -> this.openJobDetails(viewer, jobId, fromMyJobs));
        player.openInventory(inventory);
    }

    public void openApplicantList(final @NotNull Player player, final long jobId, final boolean fromMyJobs) {
        try {
            final JobDetails details = this.plugin.getJobService().getJobDetails(jobId);
            final GuiHolder holder = new GuiHolder(54, ChatColor.LIGHT_PURPLE + "応募者一覧 #" + jobId);
            final Inventory inventory = holder.getInventory();
            final List<JobMember> applicants = details.getMembers().stream()
                    .filter(member -> member.getStatus() == JobMemberStatus.APPLIED || member.getStatus() == JobMemberStatus.CONFIRMED)
                    .toList();
            int slot = 0;
            for (JobMember member : applicants) {
                if (slot >= 45) {
                    break;
                }
                final boolean applied = member.getStatus() == JobMemberStatus.APPLIED;
                inventory.setItem(slot, ItemStackFactory.create(
                        applied ? Material.PAPER : Material.LIME_WOOL,
                        member.getPlayerName(),
                        applied
                                ? List.of("状態: 応募中", "", ChatColor.AQUA + "✔ 左クリックで承認", ChatColor.RED + "✘ 右クリックで却下")
                                : List.of("状態: 確定済み", "", ChatColor.RED + "✘ 右クリックで解除(解任)")
                ));
                final JobMember target = member;
                holder.setAction(slot, (viewer, clickType) -> this.handleApplicantAction(viewer, jobId, target, clickType));
                slot++;
            }
            if (this.isRequesterOrAdmin(player, details.getJob()) && System.currentTimeMillis() > details.getJob().getRecruitmentDeadlineAt() && this.canManageMembers(details.getJob())) {
                inventory.setItem(45, ItemStackFactory.create(Material.PLAYER_HEAD, "施工者を追加", List.of(
                        "募集期限後の施工者を追加します。",
                        "",
                        ChatColor.AQUA + "➡ クリックで候補一覧を開く"
                )));
                holder.setAction(45, (viewer, clickType) -> this.openManualAddList(viewer, jobId, fromMyJobs, 0));
            }
            inventory.setItem(49, ItemStackFactory.create(Material.ARROW, "戻る", List.of("依頼詳細へ戻ります。")));
            holder.setAction(49, (viewer, clickType) -> this.openJobDetails(viewer, jobId, fromMyJobs));
            player.openInventory(inventory);
        } catch (SQLException e) {
            this.handleError(player, e);
        }
    }

    public void openManualAddList(final @NotNull Player player, final long jobId, final boolean fromMyJobs, final int offset) {
        this.runPlayerAction(player, () -> {
            final JobDetails details = this.plugin.getJobService().getJobDetails(jobId);
            if (!this.isRequesterOrAdmin(player, details.getJob())) {
                throw new IllegalStateException("施工者を追加できるのは発注者または管理者のみです。");
            }
            if (System.currentTimeMillis() <= details.getJob().getRecruitmentDeadlineAt()) {
                throw new IllegalStateException("手動追加は募集期限後のみ行えます。");
            }
            if (!this.canManageMembers(details.getJob())) {
                throw new IllegalStateException("この依頼ではもう施工者の変更はできません。");
            }

            final List<UUID> existingMembers = details.getMembers().stream()
                    .map(JobMember::getPlayerUuid)
                    .toList();
            final List<OfflinePlayer> candidates = Arrays.stream(Bukkit.getOfflinePlayers())
                    .filter(candidate -> candidate.getName() != null)
                    .filter(candidate -> !candidate.getUniqueId().equals(details.getJob().getRequesterUuid()))
                    .filter(candidate -> !existingMembers.contains(candidate.getUniqueId()))
                    .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                    .toList();

            final int toIndex = Math.min(offset + 28, candidates.size());
            final List<OfflinePlayer> page = offset >= candidates.size() ? Collections.emptyList() : candidates.subList(offset, toIndex);
            final GuiHolder holder = new GuiHolder(45, ChatColor.DARK_AQUA + "施工者追加 #" + jobId);
            final Inventory inventory = holder.getInventory();
            int slot = 0;
            for (OfflinePlayer candidate : page) {
                inventory.setItem(slot, ItemStackFactory.create(Material.PLAYER_HEAD, candidate.getName(), List.of(
                        "クリックで施工者として追加します。",
                        "追加後に対象プレイヤーへ通知します。"
                )));
                final OfflinePlayer target = candidate;
                holder.setAction(slot, (viewer, clickType) -> this.runPlayerAction(viewer, () -> {
                    this.plugin.getJobApplicationService().addConfirmedMemberAfterDeadline(jobId, viewer, target);
                    viewer.sendMessage(ChatColor.GREEN + target.getName() + " を依頼 #" + jobId + " の施工者に追加しました。");
                    this.openManualAddList(viewer, jobId, fromMyJobs, offset);
                }));
                slot++;
            }
            if (offset > 0) {
                inventory.setItem(36, ItemStackFactory.create(Material.ARROW, "前へ", List.of("前の候補一覧")));
                holder.setAction(36, (viewer, clickType) -> this.openManualAddList(viewer, jobId, fromMyJobs, Math.max(0, offset - 28)));
            }
            if (toIndex < candidates.size()) {
                inventory.setItem(44, ItemStackFactory.create(Material.ARROW, "次へ", List.of("次の候補一覧")));
                holder.setAction(44, (viewer, clickType) -> this.openManualAddList(viewer, jobId, fromMyJobs, offset + 28));
            }
            inventory.setItem(40, ItemStackFactory.create(Material.BARRIER, "戻る", List.of("応募者一覧に戻ります。")));
            holder.setAction(40, (viewer, clickType) -> this.openApplicantList(viewer, jobId, fromMyJobs));
            player.openInventory(inventory);
        });
    }

    public void openDraftEditor(final @NotNull Player player) {
        final JobDraft draft = this.drafts.get(player.getUniqueId());
        if (draft == null) {
            player.sendMessage(ChatColor.RED + "編集中のドラフトがありません。");
            return;
        }
        final GuiHolder holder = new GuiHolder(54, (draft.getMode() == JobDraftMode.CREATE ? ChatColor.GREEN + "依頼作成" : ChatColor.GOLD + "依頼編集"));
        final Inventory inventory = holder.getInventory();
        inventory.setItem(10, ItemStackFactory.create(Material.NAME_TAG, "タイトル", List.of(
                draft.getTitle(),
                "クリックしてチャットで編集"
        )));
        inventory.setItem(12, ItemStackFactory.create(Material.WRITABLE_BOOK, "依頼の説明", List.of(
                "行数: " + draft.getDescriptionLines().size(),
                this.getDescriptionPreview(draft),
                "この画面を一旦閉じて /bb lines ... を使うと編集できます (上には1行目だけが表示されます)"
        )));
        inventory.setItem(14, ItemStackFactory.create(Material.GOLD_INGOT, "報酬総額", List.of(
                MathUtil.toFullPriceString(draft.getReward()),
                "クリックしてチャットで編集",
                draft.getMode() == JobDraftMode.CREATE
                        ? "依頼作成時にこの金額が即時引き落とされます。"
                        : "報酬変更時は差額が即時精算されます。"
        )));
        inventory.setItem(16, ItemStackFactory.create(Material.CLOCK, "募集日数", List.of(
                draft.getRecruitmentDays() + "日",
                "クリックしてチャットで編集"
        )));
        inventory.setItem(28, ItemStackFactory.create(Material.CLOCK, "作業日数", List.of(
                draft.getWorkDays() + "日",
                "クリックしてチャットで編集"
        )));
        inventory.setItem(30, this.createRegionItem(draft));
        inventory.setItem(32, ItemStackFactory.create(Material.EMERALD_BLOCK, draft.getMode() == JobDraftMode.CREATE ? "依頼を作成" : "変更を保存", List.of(
                "クリックしてこの内容で保存します。",
                draft.getMode() == JobDraftMode.CREATE
                        ? "保存時に報酬総額が即時引き落とされます。"
                        : "保存時に報酬変更分の差額が即時精算されます。"
        )));
        inventory.setItem(34, ItemStackFactory.create(Material.BARRIER, "破棄する", List.of("ドラフトを破棄して戻ります。")));

        holder.setAction(10, (viewer, clickType) -> this.beginDraftInput(viewer, JobDraftField.TITLE));
        holder.setAction(12, (viewer, clickType) -> viewer.sendMessage(ChatColor.YELLOW + "/bb lines list|add|set|remove|clear で依頼の説明を編集できます。"));
        holder.setAction(14, (viewer, clickType) -> this.beginDraftInput(viewer, JobDraftField.REWARD));
        holder.setAction(16, (viewer, clickType) -> this.beginDraftInput(viewer, JobDraftField.RECRUITMENT_DAYS));
        holder.setAction(28, (viewer, clickType) -> this.beginDraftInput(viewer, JobDraftField.WORK_DAYS));
        holder.setAction(30, (viewer, clickType) -> this.captureRegion(viewer));
        holder.setAction(32, (viewer, clickType) -> this.submitDraft(viewer));
        holder.setAction(34, (viewer, clickType) -> {
            this.drafts.remove(viewer.getUniqueId());
            this.pendingInputs.remove(viewer.getUniqueId());
            this.openMainMenu(viewer);
        });
        player.openInventory(inventory);
    }

    public void openNotifications(final @NotNull Player player, final boolean unreadOnly, final int offset) {
        try {
            final List<JobNotification> notifications = this.plugin.getNotificationService().getNotifications(player.getUniqueId(), unreadOnly, 28, offset);
            final GuiHolder holder = new GuiHolder(45, ChatColor.BLUE + "通知");
            final Inventory inventory = holder.getInventory();
            int slot = 0;
            for (JobNotification notification : notifications) {
                if (slot >= 28) {
                    break;
                }
                inventory.setItem(slot, ItemStackFactory.create(
                        notification.isRead() ? Material.BOOK : Material.ENCHANTED_BOOK,
                        notification.getTitle(),
                        List.of(notification.getBody(), "", ChatColor.AQUA + "✔ クリックで既読にする")
                ));
                final JobNotification target = notification;
                holder.setAction(slot, (viewer, clickType) -> {
                    try {
                        this.plugin.getNotificationService().markAsRead(target.getId(), System.currentTimeMillis());
                        this.openNotifications(viewer, unreadOnly, offset);
                    } catch (SQLException e) {
                        this.handleError(viewer, e);
                    }
                });
                slot++;
            }
            inventory.setItem(36, ItemStackFactory.create(Material.BOOKSHELF, unreadOnly ? "すべて表示" : "未読のみ表示", List.of("表示対象を切り替えます。")));
            holder.setAction(36, (viewer, clickType) -> this.openNotifications(viewer, !unreadOnly, 0));
            inventory.setItem(40, ItemStackFactory.create(Material.PAPER, "すべて既読にする", List.of("通知を一括既読にします。")));
            holder.setAction(40, (viewer, clickType) -> {
                try {
                    this.plugin.getNotificationService().markAllAsRead(viewer.getUniqueId(), System.currentTimeMillis());
                    this.openNotifications(viewer, unreadOnly, 0);
                } catch (SQLException e) {
                    this.handleError(viewer, e);
                }
            });
            inventory.setItem(44, ItemStackFactory.create(Material.COMPASS, "メインメニュー", List.of("戻ります。")));
            holder.setAction(44, (viewer, clickType) -> this.openMainMenu(viewer));
            player.openInventory(inventory);
        } catch (SQLException e) {
            this.handleError(player, e);
        }
    }

    public boolean hasPendingDraftInput(final @NotNull UUID playerUuid) {
        return this.pendingInputs.containsKey(playerUuid);
    }

    public void clearDraftState(final @NotNull UUID playerUuid) {
        this.drafts.remove(playerUuid);
        this.pendingInputs.remove(playerUuid);
    }

    public void handleDraftChatInput(final @NotNull Player player, final @NotNull String message) {
        final PendingDraftInput pendingInput = this.pendingInputs.remove(player.getUniqueId());
        final JobDraft draft = this.drafts.get(player.getUniqueId());
        if (pendingInput == null || draft == null) {
            return;
        }
        if ("cancel".equalsIgnoreCase(message)) {
            player.sendMessage(ChatColor.YELLOW + "入力をキャンセルしました。");
            this.openDraftEditor(player);
            return;
        }
        try {
            switch (pendingInput.getField()) {
                case TITLE:
                    draft.setTitle(this.requireText(message, "Title"));
                    break;
                case REWARD:
                    draft.setReward(this.parseLong(message, "Reward"));
                    break;
                case RECRUITMENT_DAYS:
                    draft.setRecruitmentDays((int) this.parseLong(message, "Recruitment days"));
                    break;
                case WORK_DAYS:
                    draft.setWorkDays((int) this.parseLong(message, "Work days"));
                    break;
                default:
                    throw new IllegalStateException("Unknown draft field");
            }
            player.sendMessage(ChatColor.GREEN + "入力内容を更新しました。");
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
        }
        this.openDraftEditor(player);
    }

    private void fillJobList(final @NotNull Inventory inventory, final @NotNull GuiHolder holder, final @NotNull Player player, final @NotNull List<Job> jobs, final boolean fromMyJobs) {
        int slot = 0;
        for (Job job : jobs) {
            if (slot >= 28) {
                break;
            }
            inventory.setItem(slot, ItemStackFactory.create(Material.OAK_SIGN, job.getTitle(), List.of(
                    "ID: " + job.getId(),
                    "状態: " + job.getStatus().getDisplayName(),
                    "報酬総額: " + MathUtil.toFullPriceString(job.getTotalReward()),
                    "説明: " + this.getDescriptionSingleLinePreview(job.getDescription()),
                    "",
                    ChatColor.AQUA + "➡ クリックで詳細を表示"
            )));
            final long jobId = job.getId();
            holder.setAction(slot, (viewer, clickType) -> this.openJobDetails(viewer, jobId, fromMyJobs));
            slot++;
        }
    }

    private void decoratePaging(final @NotNull Inventory inventory, final @NotNull GuiHolder holder, final int offset, final int shown, final @NotNull ViewKind viewKind) {
        if (offset > 0) {
            inventory.setItem(36, ItemStackFactory.create(Material.ARROW, "前へ", List.of("前のページ")));
            holder.setAction(36, (viewer, clickType) -> this.openViewPage(viewer, Math.max(0, offset - 28), viewKind));
        }
        if (shown >= 28) {
            inventory.setItem(44, ItemStackFactory.create(Material.ARROW, "次へ", List.of("次のページ")));
            holder.setAction(44, (viewer, clickType) -> this.openViewPage(viewer, offset + 28, viewKind));
        }
    }

    private void openViewPage(final @NotNull Player player, final int offset, final @NotNull ViewKind viewKind) {
        switch (viewKind) {
            case OPEN_JOBS -> this.openOpenJobs(player, offset);
            case MY_JOBS -> this.openMyJobs(player, offset);
            case CURRENT_CONTRACTOR -> this.openCurrentContractorJobs(player, offset);
            case ALL_JOBS -> this.openAllJobs(player, offset);
        }
    }

    private void handleApplicantAction(final @NotNull Player viewer, final long jobId, final @NotNull JobMember member, final @NotNull ClickType clickType) {
        try {
            final OfflinePlayer target = Bukkit.getOfflinePlayer(member.getPlayerUuid());
            if (member.getStatus() == JobMemberStatus.APPLIED) {
                if (clickType.isLeftClick()) {
                    this.plugin.getJobApplicationService().approve(jobId, viewer, target);
                } else if (clickType.isRightClick()) {
                    this.plugin.getJobApplicationService().decline(jobId, viewer, target);
                }
            } else if (member.getStatus() == JobMemberStatus.CONFIRMED && clickType.isRightClick()) {
                this.plugin.getJobApplicationService().removeConfirmedMember(jobId, viewer, target);
            }
            this.openApplicantList(viewer, jobId, true);
        } catch (SQLException e) {
            this.handleError(viewer, e);
        }
    }

    private void beginDraftInput(final @NotNull Player player, final @NotNull JobDraftField field) {
        this.pendingInputs.put(player.getUniqueId(), new PendingDraftInput(field));
        player.closeInventory();
        switch (field) {
            case TITLE ->
                    player.sendMessage(ChatColor.YELLOW + "タイトルをチャットで入力してください。中止する場合は cancel と入力してください。");
            case REWARD ->
                    player.sendMessage(ChatColor.YELLOW + "報酬総額を正の数で入力してください。中止する場合は cancel と入力してください。");
            case RECRUITMENT_DAYS ->
                    player.sendMessage(ChatColor.YELLOW + "今から何日後を募集期限にするか入力してください。中止する場合は cancel と入力してください。");
            case WORK_DAYS ->
                    player.sendMessage(ChatColor.YELLOW + "今から何日後を作業期限にするか入力してください。中止する場合は cancel と入力してください。");
        }
    }

    private void captureRegion(final @NotNull Player player) {
        final JobDraft draft = this.drafts.get(player.getUniqueId());
        if (draft == null) {
            player.sendMessage(ChatColor.RED + "編集中のドラフトがありません。");
            return;
        }
        if (draft.getMode() == JobDraftMode.EDIT) {
            player.sendMessage(ChatColor.RED + "依頼作成後は範囲を変更できません。");
            return;
        }
        try {
            draft.setRegion(WorldEditSelectionUtil.requireSelectedCuboid(this.plugin.getWorldEditPlugin(), player));
            player.sendMessage(ChatColor.GREEN + "現在のWorldEdit選択範囲を取り込みました。");
            this.openDraftEditor(player);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "先にWorldEditで直方体(Cuboid)範囲を選択してください。");
        }
    }

    private void submitDraft(final @NotNull Player player) {
        final JobDraft draft = this.drafts.get(player.getUniqueId());
        if (draft == null) {
            player.sendMessage(ChatColor.RED + "編集中のドラフトがありません。");
            return;
        }
        this.runPlayerAction(player, () -> {
            if (draft.getMode() == JobDraftMode.CREATE) {
                final JobRegion region = draft.getRegion();
                if (region == null) {
                    throw new IllegalStateException("依頼を作成する前にWorldEdit範囲を取り込んでください。");
                }
                final long now = System.currentTimeMillis();
                final Job job = this.plugin.getJobService().createJob(
                        player,
                        draft.getTitle(),
                        draft.joinDescription(),
                        draft.getReward(),
                        now + Duration.ofDays(draft.getRecruitmentDays()).toMillis(),
                        now + Duration.ofDays(draft.getWorkDays()).toMillis(),
                        region
                );
                this.drafts.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "依頼 #" + job.getId() + " を作成しました。");
                return;
            }
            final long now = System.currentTimeMillis();
            this.plugin.getJobService().updateJobDetails(
                    draft.getJobId(),
                    player,
                    draft.getTitle(),
                    draft.joinDescription(),
                    draft.getReward(),
                    now + Duration.ofDays(draft.getRecruitmentDays()).toMillis(),
                    now + Duration.ofDays(draft.getWorkDays()).toMillis()
            );
            this.drafts.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "依頼 #" + draft.getJobId() + " を更新しました。");
        });
    }

    private boolean canApply(final @NotNull Player player, final @NotNull JobDetails details) {
        if (details.getJob().getStatus() != JobStatus.OPEN) {
            return false;
        }
        if (details.getJob().getRequesterUuid().equals(player.getUniqueId())) {
            return false;
        }
        if (System.currentTimeMillis() > details.getJob().getRecruitmentDeadlineAt()) {
            return false;
        }
        return details.getMembers().stream().noneMatch(member -> member.getPlayerUuid().equals(player.getUniqueId()));
    }

    private boolean isRequesterOrAdmin(final @NotNull Player player, final @NotNull Job job) {
        return player.isOp() || job.getRequesterUuid().equals(player.getUniqueId());
    }

    private boolean canManageMembers(final @NotNull Job job) {
        return job.getStatus() == JobStatus.OPEN || job.getStatus() == JobStatus.IN_PROGRESS || job.getStatus() == JobStatus.WORK_DEADLINE_PASSED;
    }

    private boolean isConfirmedMember(final @NotNull Player player, final @NotNull JobDetails details) {
        return details.getMembers().stream().anyMatch(member ->
                member.getPlayerUuid().equals(player.getUniqueId()) && member.getStatus() == JobMemberStatus.CONFIRMED
        );
    }

    private @NotNull String formatTime(final long timestamp) {
        final long remainingSeconds = Math.max(0L, (timestamp - System.currentTimeMillis()) / 1000L);
        final long days = remainingSeconds / 86400L;
        final long hours = (remainingSeconds % 86400L) / 3600L;
        return days + "d " + hours + "h";
    }

    private @NotNull org.bukkit.inventory.ItemStack createRegionItem(final @NotNull JobDraft draft) {
        final List<String> lore = new ArrayList<>();
        lore.add(draft.getMode() == JobDraftMode.CREATE ? "クリックで現在のWorldEdit範囲を取り込みます。" : "範囲は作成後に変更できません。");
        if (draft.getRegion() == null) {
            lore.add("まだ範囲が設定されていません。");
        } else {
            lore.add(draft.getRegion().getWorldName());
            lore.add(draft.getRegion().getMinX() + ", " + draft.getRegion().getMinY() + ", " + draft.getRegion().getMinZ());
            lore.add(draft.getRegion().getMaxX() + ", " + draft.getRegion().getMaxY() + ", " + draft.getRegion().getMaxZ());
        }
        return ItemStackFactory.create(Material.GRASS_BLOCK, "範囲", lore);
    }

    private @NotNull String requireText(final @NotNull String text, final @NotNull String fieldName) {
        final String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " は空にできません。");
        }
        return trimmed;
    }

    private long parseLong(final @NotNull String raw, final @NotNull String fieldName) {
        final long value;
        try {
            value = MathUtil.fromFriendlyString(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " は正の数で入力してください。");
        }
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " は正の数で入力してください。");
        }
        return value;
    }

    public boolean hasActiveDraft(final @NotNull UUID playerUuid) {
        return this.drafts.containsKey(playerUuid);
    }

    public @NotNull JobDraft getActiveDraft(final @NotNull UUID playerUuid) {
        final JobDraft draft = this.drafts.get(playerUuid);
        if (draft == null) {
            throw new IllegalStateException("編集中の依頼ドラフトがありません。先に /bb gui create または /bb gui edit <id> を開いてください。");
        }
        return draft;
    }

    public void reopenDraftEditor(final @NotNull Player player) {
        this.openDraftEditor(player);
    }

    private @NotNull List<String> splitDescription(final @NotNull String description) {
        if (description.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(List.of(description.split("\n", -1)));
    }

    private @NotNull String getDescriptionPreview(final @NotNull JobDraft draft) {
        if (draft.getDescriptionLines().isEmpty()) {
            return "依頼の説明文は未設定です";
        }
        final String first = draft.getDescriptionLines().getFirst();
        return first.length() > 30 ? first.substring(0, 30) + "..." : first;
    }

    private @NotNull List<String> createDescriptionLore(final @NotNull String description, final int maxLines) {
        final List<String> lines = this.splitDescription(description);
        if (lines.isEmpty() || (lines.size() == 1 && lines.getFirst().isEmpty())) {
            return List.of("説明はありません。");
        }
        final List<String> lore = new ArrayList<>();
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            lore.add(lines.get(i));
        }
        if (lines.size() > maxLines) {
            lore.add("...");
        }
        return lore;
    }

    private @NotNull String getDescriptionSingleLinePreview(final @NotNull String description) {
        final List<String> lines = this.splitDescription(description);
        if (lines.isEmpty() || (lines.size() == 1 && lines.getFirst().isEmpty())) {
            return "なし";
        }
        final String first = lines.getFirst();
        return first.length() > 24 ? first.substring(0, 24) + "..." : first;
    }

    private void handleError(final @NotNull Player player, final @NotNull SQLException e) {
        player.closeInventory();
        player.sendMessage(ChatColor.RED + "データベースエラー: " + e.getMessage());
        this.plugin.getLogger().warning("GUI error: " + e.getMessage());
    }

    private void runPlayerAction(final @NotNull Player player, final @NotNull SqlAction action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            player.sendMessage(ChatColor.RED + e.getMessage());
        } catch (SQLException e) {
            this.handleError(player, e);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private enum ViewKind {
        OPEN_JOBS,
        MY_JOBS,
        CURRENT_CONTRACTOR,
        ALL_JOBS
    }
}
