package net.azisaba.buildingboard.model.job;

public enum JobStatus {
    OPEN("募集中"),
    IN_PROGRESS("進行中"),
    WORK_DEADLINE_PASSED("作業期限切れ"),
    COMPLETED("完了"),
    CANCELLED("キャンセル"),
    EXPIRED("期限切れ"),
    ;

    private final String displayName;

    JobStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
