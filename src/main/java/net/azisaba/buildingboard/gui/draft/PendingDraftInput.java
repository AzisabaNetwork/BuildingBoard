package net.azisaba.buildingboard.gui.draft;

import org.jetbrains.annotations.NotNull;

public final class PendingDraftInput {
    private final @NotNull JobDraftField field;

    public PendingDraftInput(final @NotNull JobDraftField field) {
        this.field = field;
    }

    public @NotNull JobDraftField getField() {
        return this.field;
    }
}
