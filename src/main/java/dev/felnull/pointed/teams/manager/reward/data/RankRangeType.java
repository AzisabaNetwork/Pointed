package dev.felnull.pointed.teams.manager.reward.data;

public enum RankRangeType {
    ALL_TIME,   // 全期間
    WEEK,       // 週（アプリ側で [from, to) の LocalDate を用意）
    DAY,        // 単日（[d, d+1)）
    CUSTOM_DAYS // 任意レンジ（[from, to)）
}
