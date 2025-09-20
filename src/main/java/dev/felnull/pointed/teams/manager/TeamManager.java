package dev.felnull.pointed.teams.manager;

import dev.felnull.pointed.core.database.data.RankRow;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface TeamManager {

    long getTeamNowPoint(String teamID, String scope);
    long getTeamTotalPoint(String teamID, String scope);
    long[] getTeamNowAndTotal(String teamID, String scope); // [0]=now, [1]=total

    // 操作
    long[] addTeamPt(String teamID, String scope, long amount);     // amount > 0
    boolean subtractTeamPt(String teamID, String scope, long amount);
    long[] setTeamPt(String teamID, String scope, long newNow);

    // 初回準備
    //TeamIDはCoreで言うsubjectKey
    void createTeam(String teamID, String scope, String displayName);

    // ランキング
    java.util.List<RankRow> getTeamDailyTop(String scope, java.time.LocalDate day, int limit);
    java.util.List<RankRow> getTeamWeeklyTop(String scope, java.time.LocalDate startInclusive, java.time.LocalDate endInclusive, int limit);
    java.util.List<RankRow> getTeamGlobalTop(String scope, int limit);

    // メタと表示名の管理
    void upsertTeam(String teamID, String scope, String displayName, String color, Integer sortOrder, Boolean active);
    void setTeamDisplayName(String teamID, String newName);
    void setTeamColor(String teamID, String color);
    void setTeamSortOrder(String teamID, int sortOrder);
    void setTeamActive(String teamID, boolean active);

    // 取得
    java.util.Optional<TeamData> loadTeam(String teamID);
    java.util.List<TeamData> loadAllTeamsOrdered(); // sort_order→subject_key

    // 削除
    void deleteTeamEverywhere(String teamID);      // 全スコープごと完全削除
    void deleteTeamInScope(String teamID, String scope); // スコープだけ削除（subjectsは残す）

    //util
    // 非同期にバー/凡例を生成して返す
    void sendBarAndLegendAsync(CommandSender sender,
                               String scope, int width, java.util.List<String> order);
}
