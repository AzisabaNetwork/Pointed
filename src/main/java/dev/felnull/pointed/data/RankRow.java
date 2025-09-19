package dev.felnull.pointed.data;

import java.util.UUID;

public final class RankRow {
    public final String subjectType;
    public final String subjectKey;
    public final String name;
    /**
     * gained:
     *   - daily/weekly → その期間に増加したポイント
     *   - global       → totalPoint をそのまま入れる
     */
    public final long gained;     // 日次/週次は増分, globalはtotal
    public final long nowPoint;
    public final long totalPoint;

    public RankRow(String subjectType, String subjectKey, String name,
                   long gained, long nowPoint, long totalPoint) {
        this.subjectType = subjectType;
        this.subjectKey = subjectKey;
        this.name = name;
        this.gained = gained;
        this.nowPoint = nowPoint;
        this.totalPoint = totalPoint;
    }
}