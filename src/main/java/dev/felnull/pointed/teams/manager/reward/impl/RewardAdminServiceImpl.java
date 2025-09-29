package dev.felnull.pointed.teams.manager.reward.impl;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.database.Names;
import dev.felnull.pointed.teams.manager.reward.RankingService;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import dev.felnull.pointed.teams.manager.reward.data.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class RewardAdminServiceImpl implements RewardAdminService {
    private final DataSource ds;
    private final RankingService rankingService;

    public RewardAdminServiceImpl(DataSource ds, RankingService rankingService) {
        this.ds = ds;
        this.rankingService = rankingService;
    }

    // ============ Repository機能の統合（読み取り） ============
    @Override
    public List<String> findEnabledCommandsByRewardId(int rewardId) throws SQLException {
        String sql = "SELECT command_text " +
                "FROM " + Names.t("reward_commands") + " " +
                "WHERE reward_id = ? AND enabled = 1 " +
                "ORDER BY idx ASC";

        List<String> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("command_text"));
            }
        }
        return list;
    }

    // ============ Dispatcher機能の統合（配布） ============
    @Override
    public void dispatchTopN(long teamSubjectId, String scope,
                             RankRangeType rangeType, LocalDate fromDate, LocalDate toDate,
                             int topN, int rewardId) throws SQLException {

        List<PlayerRank> ranks = rankingService.getTeamRanking(
                teamSubjectId, scope, rangeType, fromDate, toDate, topN, 0);
        if (ranks.isEmpty()) return;

        List<String> commands = findEnabledCommandsByRewardId(rewardId);
        if (commands.isEmpty()) return;

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT IGNORE INTO ").append(Names.t("reward_dispatch_log"))
                .append(" (team_subject_id, scope, from_date, to_date, ")
                .append("  player_subject_id, rank_no, points, reward_id, executed_commands) ")
                .append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                int dispatched = 0;
                for (PlayerRank r : ranks) {
                    if (dispatched >= topN) break;

                    String executed = buildExecutedCommands(commands, r);

                    int i = 1;
                    ps.setLong(i++, teamSubjectId);
                    ps.setString(i++, scope);
                    ps.setDate(i++, java.sql.Date.valueOf(fromDate));
                    ps.setDate(i++, java.sql.Date.valueOf(toDate)); // [from, to)
                    ps.setLong(i++, r.playerSubjectId);
                    ps.setInt(i++, r.rankNo);
                    ps.setLong(i++, r.points);
                    ps.setInt(i++, rewardId);
                    ps.setString(i++, executed);

                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        // 実コマンド実行
                        for (String raw : commands) {
                            String cmd = substitute(raw, r);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        }
                        dispatched++;
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private static String buildExecutedCommands(List<String> commands, PlayerRank r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commands.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(substitute(commands.get(i), r));
        }
        return sb.toString();
    }

    private static String substitute(String template, PlayerRank r) {
        String s = template;
        s = s.replace("{playerId}", Long.toString(r.playerSubjectId));
        s = s.replace("{player}", r.playerName == null ? "" : r.playerName);
        s = s.replace("{rank}", Integer.toString(r.rankNo));
        s = s.replace("{points}", Long.toString(r.points));
        return s;
    }


    // ================== 以下、既出のAdmin（CRUD）もここに集約 ==================
    @Override
    public int createReward(
            int rewardId,            // ← 追加
            String displayName,
            int needPoint,
            int needMinTotal,
            boolean repeatable,
            boolean active) throws SQLException {

        String sql = "INSERT INTO " + Names.t("rewards") +
                " (id, display_name, need_point, need_min_total, repeatable, active) " +
                " VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setInt(i++, rewardId);
            ps.setString(i++, displayName);
            ps.setInt(i++, needPoint);
            ps.setInt(i++, needMinTotal);
            ps.setBoolean(i++, repeatable);
            ps.setBoolean(i++, active);
            ps.executeUpdate();
            return rewardId; // そのまま返す
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new SQLException("Reward ID が重複しています: " + rewardId, dup);
        }
    }

    @Override
    public void updateReward(int rewardId, String displayName, Integer needPoint, Integer needMinTotal,
                             Boolean repeatable, Boolean active) throws SQLException {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        sql.append("UPDATE ").append(Names.t("rewards")).append(" SET ");
        boolean first = true;
        if (displayName != null) { sql.append(first?"":" ,").append("display_name=?"); params.add(displayName); first=false; }
        if (needPoint   != null) { sql.append(first?"":" ,").append("need_point=?");   params.add(needPoint);   first=false; }
        if (needMinTotal!= null) { sql.append(first?"":" ,").append("need_min_total=?");params.add(needMinTotal);first=false; }
        if (repeatable  != null) { sql.append(first?"":" ,").append("repeatable=?");   params.add(repeatable);  first=false; }
        if (active      != null) { sql.append(first?"":" ,").append("active=?");       params.add(active);      first=false; }
        if (first) return;

        sql.append(" WHERE id=?");
        params.add(rewardId);

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object v = params.get(i);
                int idx = i + 1;
                if (v instanceof String)  ps.setString(idx, (String)v);
                else if (v instanceof Integer) ps.setInt(idx, (Integer) v);
                else if (v instanceof Boolean) ps.setBoolean(idx, (Boolean) v);
                else ps.setObject(idx, v);
            }
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteReward(int rewardId, boolean force) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (!force) {
                    if (exists(c, Names.t("subject_rewards"), "reward_id", rewardId))
                        throw new SQLException("subject_rewards 参照あり: force=true で強制削除してください。");
                    if (exists(c, Names.t("reward_prerequisites"), "prereq_reward_id", rewardId))
                        throw new SQLException("reward_prerequisites 参照あり: force=true で強制削除してください。");
                } else {
                    deleteRows(c, Names.t("subject_rewards"), "reward_id", rewardId);
                    deleteRows(c, Names.t("reward_prerequisites"), "prereq_reward_id", rewardId);
                    deleteRows(c, Names.t("reward_prerequisites"), "reward_id", rewardId);
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + Names.t("rewards") + " WHERE id=?")) {
                    ps.setInt(1, rewardId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    @Override
    public int addCommand(int rewardId, String commandText, Integer idx) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                int insertIdx = (idx != null) ? idx : (maxIdx(c, rewardId) + 1);
                shiftRightFrom(c, rewardId, insertIdx);

                String sql = "INSERT INTO " + Names.t("reward_commands") +
                        " (reward_id, idx, command_text, enabled) VALUES (?, ?, ?, 1)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, rewardId);
                    ps.setInt(2, insertIdx);
                    ps.setString(3, commandText);
                    ps.executeUpdate();
                }

                resequenceCommandsTx(c, rewardId);

                c.commit();
                return insertIdx;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    @Override
    public void removeCommand(int rewardId, int idx) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                String del = "DELETE FROM " + Names.t("reward_commands") +
                        " WHERE reward_id=? AND idx=?";
                try (PreparedStatement ps = c.prepareStatement(del)) {
                    ps.setInt(1, rewardId);
                    ps.setInt(2, idx);
                    ps.executeUpdate();
                }
                shiftLeftAfter(c, rewardId, idx);

                resequenceCommandsTx(c, rewardId);

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    @Override
    public void moveCommand(int rewardId, int fromIdx, int toIdx) throws SQLException {
        if (fromIdx == toIdx) return;
        if (toIdx < 0) throw new SQLException("toIdx must be >= 0");

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                // 存在チェック（fromが無いのは異常）
                if (!existsByIdx(c, rewardId, fromIdx)) {
                    throw new SQLException("source idx not found: " + fromIdx);
                }
                boolean destExists = existsByIdx(c, rewardId, toIdx);

                // 1) 移動元を一時退避 (-1)
                bumpToTemp(c, rewardId, fromIdx); // SET idx = -1 WHERE reward_id=? AND idx=fromIdx

                if (destExists) {
                    // === swap パス ===
                    // 2) 移動先を別の一時退避 (-2)
                    String tmp2 = "UPDATE " + Names.t("reward_commands")
                            + " SET idx=-2 WHERE reward_id=? AND idx=?";
                    try (PreparedStatement ps = c.prepareStatement(tmp2)) {
                        ps.setInt(1, rewardId);
                        ps.setInt(2, toIdx);
                        int n = ps.executeUpdate();
                        if (n == 0) throw new SQLException("dest idx vanished during move: " + toIdx);
                    }

                    // 3) 元(-1)を toIdx へ
                    String setTo = "UPDATE " + Names.t("reward_commands")
                            + " SET idx=? WHERE reward_id=? AND idx=-1";
                    try (PreparedStatement ps = c.prepareStatement(setTo)) {
                        ps.setInt(1, toIdx);
                        ps.setInt(2, rewardId);
                        ps.executeUpdate();
                    }

                    // 4) 退避(-2)を fromIdx へ
                    String setFrom = "UPDATE " + Names.t("reward_commands")
                            + " SET idx=? WHERE reward_id=? AND idx=-2";
                    try (PreparedStatement ps = c.prepareStatement(setFrom)) {
                        ps.setInt(1, fromIdx);
                        ps.setInt(2, rewardId);
                        ps.executeUpdate();
                    }

                } else {
                    // === 空きスロットへ移動（シフトなし） ===
                    String setTo = "UPDATE " + Names.t("reward_commands")
                            + " SET idx=? WHERE reward_id=? AND idx=-1";
                    try (PreparedStatement ps = c.prepareStatement(setTo)) {
                        ps.setInt(1, toIdx);
                        ps.setInt(2, rewardId);
                        ps.executeUpdate();
                    }
                }

                // 最後に 0..N へ整形（安全＆見た目を常に綺麗に）
                resequenceCommandsTx(c, rewardId);

                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private static boolean existsByIdx(Connection c, int rewardId, int idx) throws SQLException {
        String sql = "SELECT 1 FROM " + Names.t("reward_commands") +
                " WHERE reward_id=? AND idx=? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rewardId);
            ps.setInt(2, idx);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public void setCommandEnabled(int rewardId, int idx, boolean enabled) throws SQLException {
        String sql = "UPDATE " + Names.t("reward_commands") +
                " SET enabled=? WHERE reward_id=? AND idx=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setInt(2, rewardId);
            ps.setInt(3, idx);
            ps.executeUpdate();
        }
    }

    @Override
    public void updateCommandText(int rewardId, int idx, String newText) throws SQLException {
        String sql = "UPDATE " + Names.t("reward_commands") +
                " SET command_text=? WHERE reward_id=? AND idx=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newText);
            ps.setInt(2, rewardId);
            ps.setInt(3, idx);
            ps.executeUpdate();
        }
    }

    // ===== 内部ユーティリティ =====

    private static boolean exists(Connection c, String table, String col, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM " + table + " WHERE " + col + "=? LIMIT 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static void deleteRows(Connection c, String table, String col, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE " + col + "=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static int maxIdx(Connection c, int rewardId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(idx), -1) FROM " + Names.t("reward_commands") +
                " WHERE reward_id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    private static void shiftRightFrom(Connection c, int rewardId, int fromIdx) throws SQLException {
        String sql = "UPDATE " + Names.t("reward_commands") +
                " SET idx = idx + 1 WHERE reward_id=? AND idx >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rewardId);
            ps.setInt(2, fromIdx);
            ps.executeUpdate();
        }
    }

    private static void shiftLeftAfter(Connection c, int rewardId, int afterIdx) throws SQLException {
        String sql = "UPDATE " + Names.t("reward_commands") +
                " SET idx = idx - 1 WHERE reward_id=? AND idx > ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rewardId);
            ps.setInt(2, afterIdx);
            ps.executeUpdate();
        }
    }

    private static void bumpToTemp(Connection c, int rewardId, int fromIdx) throws SQLException {
        String sql = "UPDATE " + Names.t("reward_commands") +
                " SET idx=-1 WHERE reward_id=? AND idx=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rewardId);
            ps.setInt(2, fromIdx);
            int n = ps.executeUpdate();
            if (n == 0) throw new SQLException("指定の idx が見つかりません: " + fromIdx);
        }
    }

    @Override
    public void dispatchByBuckets(long teamSubjectId, String scope,
                                  RankRangeType rangeType, LocalDate fromDate, LocalDate toDate,
                                  List<RankBucket> buckets) throws SQLException {
        if (buckets == null || buckets.isEmpty()) return;

        // 1) ランキングを全件取得（境界同点を漏らさない）
        List<PlayerRank> ranks = rankingService.getTeamRankingAll(
                teamSubjectId, scope, rangeType, fromDate, toDate);
        if (ranks.isEmpty()) return;

        // 2) バケット順に先勝ち割当（重複配布はしない）
        //    「同点は全員」→ rankNo が範囲に入っていれば全員対象
        Map<Integer, List<String>> rewardCmds = new HashMap<>();
        for (RankBucket b : buckets) {
            if (!rewardCmds.containsKey(b.rewardId)) {
                rewardCmds.put(b.rewardId, findEnabledCommandsByRewardId(b.rewardId));
            }
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT IGNORE INTO ").append(Names.t("reward_dispatch_log"))
                .append(" (team_subject_id, scope, from_date, to_date, ")
                .append("  player_subject_id, rank_no, points, reward_id, executed_commands) ")
                .append(" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");

        // 既にこの配布バッチで別バケットに割当済みの player_subject_id を記録（先勝ち1回）
        Set<Long> assigned = new HashSet<>();

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (PlayerRank r : ranks) {
                    if (assigned.contains(r.playerSubjectId)) continue;

                    for (RankBucket b : buckets) {
                        if (r.rankNo < b.fromRank || r.rankNo > b.toRank) continue;

                        List<String> cmds = rewardCmds.get(b.rewardId);
                        if (cmds == null || cmds.isEmpty()) break; // コマンド未設定ならスキップ

                        String executed = buildExecutedCommands(cmds, r);

                        int i = 1;
                        ps.setLong(i++, teamSubjectId);
                        ps.setString(i++, scope);
                        ps.setDate(i++, java.sql.Date.valueOf(fromDate));
                        ps.setDate(i++, java.sql.Date.valueOf(toDate));
                        ps.setLong(i++, r.playerSubjectId);
                        ps.setInt(i++, r.rankNo);
                        ps.setLong(i++, r.points);
                        ps.setInt(i++, b.rewardId);
                        ps.setString(i++, executed);

                        int affected = ps.executeUpdate();
                        if (affected > 0) {
                            for (String raw : cmds) {
                                String cmd = substitute(raw, r);
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            }
                            assigned.add(r.playerSubjectId);
                        }
                        break; // 先勝ちで1バケットにだけ配布
                    }
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    @Override
    public List<RewardSummary> listRewards(Boolean active, String query, String orderBy, int limit, int offset) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.id, r.display_name, r.need_point, r.need_min_total, r.repeatable, r.active, r.updated_at, ")
                .append("       COALESCE(rc.cnt, 0) AS command_count ")
                .append("FROM ").append(Names.t("rewards")).append(" r ")
                .append("LEFT JOIN ( ")
                .append("  SELECT reward_id, COUNT(*) AS cnt ")
                .append("  FROM ").append(Names.t("reward_commands"))
                .append("  GROUP BY reward_id ")
                .append(") rc ON rc.reward_id = r.id ");

        List<Object> params = new ArrayList<Object>();
        boolean whereOpen = false;

        if (active != null) {
            sql.append(whereOpen ? " AND " : " WHERE ");
            whereOpen = true;
            sql.append("r.active = ? ");
            params.add(active);
        }
        if (query != null && !query.isEmpty()) {
            sql.append(whereOpen ? " AND " : " WHERE ");
            whereOpen = true;
            sql.append("r.display_name LIKE ? ");
            params.add("%" + query + "%");
        }

        // 並び順
        if ("name_asc".equalsIgnoreCase(orderBy)) {
            sql.append("ORDER BY r.display_name ASC ");
        } else if ("name_desc".equalsIgnoreCase(orderBy)) {
            sql.append("ORDER BY r.display_name DESC ");
        } else if ("updated_asc".equalsIgnoreCase(orderBy)) {
            sql.append("ORDER BY r.updated_at ASC, r.id ASC ");
        } else { // 既定: updated_desc
            sql.append("ORDER BY r.updated_at DESC, r.id DESC ");
        }

        sql.append("LIMIT ? OFFSET ?");

        List<RewardSummary> out = new ArrayList<RewardSummary>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            int i = 1;
            for (Object v : params) {
                if (v instanceof String) ps.setString(i++, (String)v);
                else if (v instanceof Boolean) ps.setBoolean(i++, (Boolean) v);
                else ps.setObject(i++, v);
            }
            ps.setInt(i++, limit);
            ps.setInt(i++, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new RewardSummary(
                            rs.getInt("id"),
                            rs.getString("display_name"),
                            rs.getInt("need_point"),
                            rs.getInt("need_min_total"),
                            rs.getBoolean("repeatable"),
                            rs.getBoolean("active"),
                            rs.getTimestamp("updated_at"),
                            rs.getInt("command_count")
                    ));
                }
            }
        }
        return out;
    }

    @Override
    public int countRewards(Boolean active, String query) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(Names.t("rewards")).append(" r ");

        List<Object> params = new ArrayList<Object>();
        boolean whereOpen = false;

        if (active != null) {
            sql.append(whereOpen ? " AND " : " WHERE ");
            whereOpen = true;
            sql.append("r.active = ? ");
            params.add(active);
        }
        if (query != null && !query.isEmpty()) {
            sql.append(whereOpen ? " AND " : " WHERE ");
            whereOpen = true;
            sql.append("r.display_name LIKE ? ");
            params.add("%" + query + "%");
        }

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            for (Object v : params) {
                if (v instanceof String) ps.setString(i++, (String)v);
                else if (v instanceof Boolean) ps.setBoolean(i++, (Boolean) v);
                else ps.setObject(i++, v);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public RewardDetail getRewardDetail(int rewardId, boolean onlyEnabledCommands) throws SQLException {
        // 1) 本体
        String sql1 = "SELECT id, display_name, need_point, need_min_total, repeatable, active, created_at, updated_at " +
                "FROM " + Names.t("rewards") + " WHERE id=?";

        Integer id = null;
        String displayName = null;
        Integer needPoint = null;
        Integer needMinTotal = null;
        Boolean repeatable = null;
        Boolean active = null;
        java.sql.Timestamp createdAt = null;
        java.sql.Timestamp updatedAt = null;

        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql1)) {
            ps.setInt(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                id = rs.getInt("id");
                displayName = rs.getString("display_name");
                needPoint = rs.getInt("need_point");
                needMinTotal = rs.getInt("need_min_total");
                repeatable = rs.getBoolean("repeatable");
                active = rs.getBoolean("active");
                createdAt = rs.getTimestamp("created_at");
                updatedAt = rs.getTimestamp("updated_at");
            }
        }

        // 2) コマンド（idx順）
        StringBuilder sql2 = new StringBuilder();
        sql2.append("SELECT idx, command_text, enabled ")
                .append("FROM ").append(Names.t("reward_commands"))
                .append(" WHERE reward_id=? ");
        if (onlyEnabledCommands) {
            sql2.append("AND enabled=1 ");
        }
        sql2.append("ORDER BY idx ASC");

        List<RewardCommandRow> cmds = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql2.toString())) {
            ps.setInt(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cmds.add(new RewardCommandRow(
                            rs.getInt("idx"),
                            rs.getString("command_text"),
                            rs.getBoolean("enabled")
                    ));
                }
            }
        }

        return new RewardDetail(
                id, displayName,
                needPoint, needMinTotal,
                repeatable, active,
                createdAt, updatedAt, cmds
        );
    }

    private static void resequenceCommandsTx(Connection c, int rewardId) throws SQLException {
        // 1) 退避
        String step1 = "UPDATE " + Names.t("reward_commands") +
                " SET idx = idx + 1000000 WHERE reward_id = ?";
        try (PreparedStatement ps = c.prepareStatement(step1)) {
            ps.setInt(1, rewardId);
            ps.executeUpdate();
        }

        // 2) 0..Nに確定（@rownum）
        String step2 = "UPDATE " + Names.t("reward_commands") + " rc " +
                "JOIN (" +
                "  SELECT reward_id, idx, (@rownum := @rownum + 1) AS new_idx " +
                "  FROM " + Names.t("reward_commands") + ", (SELECT @rownum := -1) vars " +
                "  WHERE reward_id = ? " +
                "  ORDER BY idx ASC" +
                ") t ON rc.reward_id = t.reward_id AND rc.idx = t.idx " +
                "SET rc.idx = t.new_idx";
        try (PreparedStatement ps = c.prepareStatement(step2)) {
            ps.setInt(1, rewardId);
            ps.executeUpdate();
        }
    }

    @Override
    public void dispatchManual(UUID playerUuid, String playerName, int rewardId) throws SQLException {
        String subjectType = "PLAYER";
        String subjectKey = playerUuid.toString(); // 必ずUUID文字列
        List<String> commands = findEnabledCommandsByRewardId(rewardId);
        if (commands.isEmpty()) return;

        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);
            try {
                // subjects を upsert して ID を確保
                long subjectId = Pointed.getInstance().getPointService().ensureSubject(con, subjectType, subjectKey, playerName);

                // rankNo/pointsは単発付与なので0固定
                PlayerRank r = new PlayerRank(subjectId, playerName, 0, 0);

                // ログINSERT
                String sql = "INSERT INTO " + Names.t("reward_dispatch_log") +
                        " (team_subject_id, scope, from_date, to_date, " +
                        "  player_subject_id, rank_no, points, reward_id, executed_commands) " +
                        " VALUES (NULL, 'manual', CURRENT_DATE, CURRENT_DATE, ?, ?, ?, ?, ?)";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    String executed = buildExecutedCommands(commands, r);
                    int i = 1;
                    ps.setLong(i++, subjectId);   // player_subject_id
                    ps.setInt(i++, r.rankNo);     // rank = 0
                    ps.setLong(i++, r.points);    // points = 0
                    ps.setInt(i++, rewardId);
                    ps.setString(i++, executed);
                    ps.executeUpdate();
                }

                // コマンド実行
                for (String raw : commands) {
                    String cmd = substitute(raw, r);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // ==== 存在確認（非トランザクション版）====
    public boolean rewardExists(long rewardId) {
        try (Connection con = ds.getConnection()) {
            return rewardExists(con, rewardId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // ==== 存在確認（トランザクション版・接続使い回し）====
    public boolean rewardExists(Connection con, long rewardId) throws SQLException {
        final String sql = "SELECT 1 FROM rewards WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // 1行でも返れば存在
            }
        }
    }

    // もし名前で唯一ならこっちも（任意）
    public boolean rewardExistsByName(Connection con, String name) throws SQLException {
        final String sql = "SELECT 1 FROM rewards WHERE name = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

}
