package dev.felnull.pointed.teams.manager.reward.data;

public final class PlayerRank {
    public final long playerSubjectId;
    public final String playerName;
    public final long points;
    public final int rankNo;

    public PlayerRank(long playerSubjectId, String playerName, long points, int rankNo) {
        this.playerSubjectId = playerSubjectId;
        this.playerName = playerName;
        this.points = points;
        this.rankNo = rankNo;
    }
}

