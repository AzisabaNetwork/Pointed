package dev.felnull.pointed.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SubjectPointData {
    private final long subjectId;
    private final SubjectType type;
    private final UUID playerUuid;   // PLAYER時のみ
    private final String teamKey;    // TEAM時のみ

    private final Map<String, Integer> pointMap = new HashMap<>();
    private final Map<String, Integer> totalPointMap = new HashMap<>();
    private final Map<Integer, Integer> rewardObtained = new HashMap<>();

    public SubjectPointData(long subjectId, SubjectType type, UUID playerUuid, String teamKey) {
        this.subjectId = subjectId;
        this.type = type;
        this.playerUuid = playerUuid;
        this.teamKey = teamKey;
    }

    public int addPoint(String pointName, int add) {
        pointMap.compute(pointName, (k, v) -> (v == null ? 0 : v) + add);
        totalPointMap.compute(pointName, (k, v) -> (v == null ? 0 : v) + add);
        return pointMap.get(pointName);
    }

    public int subtractPoint(String pointName, int sub) {
        pointMap.compute(pointName, (k, v) -> (v == null ? 0 : v) - sub);
        return pointMap.get(pointName);
    }

    public int getPoint(String pointName) { return pointMap.getOrDefault(pointName, 0); }
    public int getTotalPoint(String pointName) { return totalPointMap.getOrDefault(pointName, 0); }

    public int getObtained(int rewardId) { return rewardObtained.getOrDefault(rewardId, 0); }
    public void setObtained(int rewardId, int n) { rewardObtained.put(rewardId, n); }

    public Set<String> getPointMapKeys() { return pointMap.keySet(); }

    public long getSubjectId() { return subjectId; }
    public SubjectType getType() { return type; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getTeamKey() { return teamKey; }
}

