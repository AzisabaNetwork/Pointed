package dev.felnull.pointed.teams.manager.reward.data;

public final class RankBucket {
    public final int fromRank; // 含む（1始まり）
    public final int toRank;   // 含む
    public final int rewardId;

    public RankBucket(int fromRank, int toRank, int rewardId) {
        this.fromRank = fromRank;
        this.toRank = toRank;
        this.rewardId = rewardId;
    }
}