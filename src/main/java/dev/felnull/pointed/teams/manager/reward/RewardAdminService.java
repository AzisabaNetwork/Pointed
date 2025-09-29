package dev.felnull.pointed.teams.manager.reward;

import dev.felnull.pointed.teams.manager.reward.data.RankBucket;
import dev.felnull.pointed.teams.manager.reward.data.RankRangeType;
import dev.felnull.pointed.teams.manager.reward.data.RewardDetail;
import dev.felnull.pointed.teams.manager.reward.data.RewardSummary;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RewardAdminService {
    // ===== rewards  =====
    int  createReward(int rewardId, String displayName, int needPoint, int needMinTotal, boolean repeatable, boolean active) throws SQLException;
    void updateReward(int rewardId, String displayName, Integer needPoint, Integer needMinTotal, Boolean repeatable, Boolean active) throws SQLException;
    void deleteReward(int rewardId, boolean force) throws SQLException;

    // ===== reward_commands 管理 =====
    //idxがnullな場合末尾に追加
    int  addCommand(int rewardId, String commandText, Integer idx) throws SQLException;
    void removeCommand(int rewardId, int idx) throws SQLException;
    void moveCommand(int rewardId, int fromIdx, int toIdx) throws SQLException;
    void setCommandEnabled(int rewardId, int idx, boolean enabled) throws SQLException;
    void updateCommandText(int rewardId, int idx, String newText) throws SQLException;
    boolean rewardExists(long rewardId);

    // ===== Reward =====
    /**
     * 利用例
     * List<RankBucket> buckets = Arrays.asList(
     *     new RankBucket(1, 1,   100), // 1位だけ
     *     new RankBucket(2, 3,   101), // 2～3位
     *     new RankBucket(4, 10,  102)  // 4～10位（10位に同点が何人いても全員）
     * );
     *
     * adminService.dispatchByBuckets(
     *     teamSubjectId, scope,
     *     RankRangeType.WEEK, monday, nextMonday,
     *     buckets
     * );
     */
    /** 配布用：有効なコマンドを idx 昇順で取得 */
    List<String> findEnabledCommandsByRewardId(int rewardId) throws SQLException;

    /** 上位N名に rewardId のコマンドを配布（重複配布は dispatch_log UNIQUE で防止） */
    void dispatchTopN(long teamSubjectId, String scope,
                      RankRangeType rangeType, LocalDate fromDate, LocalDate toDate,
                      int topN, int rewardId) throws SQLException;

    /** 順位レンジごとに別報酬を配布（n～m対応） */
    void dispatchByBuckets(long teamSubjectId, String scope,
                           RankRangeType rangeType, LocalDate fromDate, LocalDate toDate,
                           List<RankBucket> buckets) throws SQLException;

    // プレイヤー/チームを直接指定して手動配布（scope=manual固定／期間なし／繰り返しOK）
    void dispatchManual(UUID playerUuid, String playerName, int rewardId) throws SQLException;

    /**
     * 報酬一覧（ページング・検索・並び替え）。
     * @param active null=全て / true=アクティブのみ / false=非アクティブのみ
     * @param query  部分一致検索（display_name）
     *               query が null/空文字なら → 絞り込みしない（全件表示）
     *               query が "Top"なら → display_name に "Top" を含む報酬だけ出る
     * @param orderBy "updated_desc" | "updated_asc" | "name_asc" | "name_desc"
     * @param limit  件数
     * @param offset オフセット
     */
    List<RewardSummary> listRewards(Boolean active, String query, String orderBy, int limit, int offset) throws SQLException;

    /** 総件数（ページングUI用） */
    int countRewards(Boolean active, String query) throws SQLException;

    /** 詳細（コマンド付き） */
    RewardDetail getRewardDetail(int rewardId, boolean onlyEnabledCommands) throws SQLException;

    /** 過去にその報酬を配布したか */
    boolean hasEverDistributedRewardToPlayerByUuid(UUID playerUuid, int rewardId);

    // === 既存シグネチャ互換の非Tx版（内部で接続取得）===
    boolean hasEverDistributedRewardToPlayer(long teamId, int rewardId) throws SQLException;
}