package dev.felnull.pointed.teams.manager.reward.data;

public final class RewardCommandRow {
    public final int idx;
    public final String commandText;
    public final boolean enabled;

    public RewardCommandRow(int idx, String commandText, boolean enabled) {
        this.idx = idx;
        this.commandText = commandText;
        this.enabled = enabled;
    }
}
