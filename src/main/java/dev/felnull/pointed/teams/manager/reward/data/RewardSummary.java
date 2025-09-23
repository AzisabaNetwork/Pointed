package dev.felnull.pointed.teams.manager.reward.data;

import java.util.List;

public final class RewardSummary {
    public final int id;
    public final String displayName;
    public final int needPoint;
    public final int needMinTotal;
    public final boolean repeatable;
    public final boolean active;
    public final java.sql.Timestamp updatedAt; // 並び替え・表示に便利
    public final int commandCount;             // コマンドの本数（有効/無効問わず）

    public RewardSummary(int id, String displayName, int needPoint, int needMinTotal,
                         boolean repeatable, boolean active, java.sql.Timestamp updatedAt, int commandCount) {
        this.id = id;
        this.displayName = displayName;
        this.needPoint = needPoint;
        this.needMinTotal = needMinTotal;
        this.repeatable = repeatable;
        this.active = active;
        this.updatedAt = updatedAt;
        this.commandCount = commandCount;
    }
}

