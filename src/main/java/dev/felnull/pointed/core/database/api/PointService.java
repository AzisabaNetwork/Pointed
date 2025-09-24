package dev.felnull.pointed.core.database.api;

import dev.felnull.pointed.core.database.data.RankRow;

import java.util.List;

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

    //指定 scope にある subject の口座とポイント履歴を削除（subjects は残す）
    boolean deleteSubjectInScope(String subjectType, String subjectKey, String scope);

    //すべての scope で subject を完全削除（accounts/履歴を消し、subjects も削除
    boolean deleteSubjectEverywhere(String subjectType, String subjectKey);

    List<String> listScopes(String subjectType, String subjectKey);
    List<String> listAllScopes();
}