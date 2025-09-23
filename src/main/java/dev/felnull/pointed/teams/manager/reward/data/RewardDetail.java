package dev.felnull.pointed.teams.manager.reward.data;

import java.util.List;

public final class RewardDetail {
    public final int id;
    public final String displayName;
    public final int needPoint;
    public final int needMinTotal;
    public final boolean repeatable;
    public final boolean active;
    public final java.sql.Timestamp createdAt;
    public final java.sql.Timestamp updatedAt;
    public final List<RewardCommandRow> commands; // idx順

    public RewardDetail(int id, String displayName, int needPoint, int needMinTotal,
                        boolean repeatable, boolean active, java.sql.Timestamp createdAt,
                        java.sql.Timestamp updatedAt, List<RewardCommandRow> commands) {
        this.id = id;
        this.displayName = displayName;
        this.needPoint = needPoint;
        this.needMinTotal = needMinTotal;
        this.repeatable = repeatable;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.commands = commands;
    }
}
