package dev.felnull.pointed.database.api;

import dev.felnull.pointed.data.RankRow;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PointService {

    //subjectType = "PLAYER"とか"TEAM"とか
    //subjectKey = player.uuid とか？ 赤とか
    // 取得
    long getNowPoint(String subjectType, String subjectKey, String scope);
    long getTotalPoint(String subjectType, String subjectKey, String scope);
    long[] getNowAndTotal(String subjectType, String subjectKey, String scope); // [0]=now, [1]=total

    // 操作
    long[] add(String subjectType, String subjectKey, String scope, long amount);     // amount > 0
    boolean subtract(String subjectType, String subjectKey, String scope, long amount);
    long[] set(String subjectType, String subjectKey, String scope, long newNow);

    // 初回準備
    void ensureAccount(String subjectType, String subjectKey, String scope, String name);

    // ランキング
    java.util.List<RankRow> getDailyTop(String subjectType, String scope, java.time.LocalDate day, int limit);
    java.util.List<RankRow> getWeeklyTop(String subjectType, String scope, java.time.LocalDate startInclusive, java.time.LocalDate endInclusive, int limit);
    java.util.List<RankRow> getGlobalTop(String subjectType, String scope, int limit);
}