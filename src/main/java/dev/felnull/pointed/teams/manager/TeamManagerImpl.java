package dev.felnull.pointed.teams.manager;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.database.Db;
import dev.felnull.pointed.core.database.Names;
import dev.felnull.pointed.core.database.api.PointServiceImpl;
import dev.felnull.pointed.core.database.api.SubjectType;
import dev.felnull.pointed.core.database.data.RankRow;
import dev.felnull.pointed.core.util.Util;
import org.bukkit.command.CommandSender;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class TeamManagerImpl implements TeamManager{
    PointServiceImpl ptService = Pointed.pointService;
    String subjectType = SubjectType.TEAM.name();
    DataSource ds = Db.get();
    private final Executor executor = ForkJoinPool.commonPool();

    @Override
    public long getTeamNowPoint(String teamID, String scope) {
        return ptService.getNowPoint(subjectType, teamID, scope);
    }

    @Override
    public long getTeamTotalPoint(String teamID, String scope) {
        return ptService.getTotalPoint(subjectType, teamID, scope);
    }

    @Override
    public long[] getTeamNowAndTotal(String teamID, String scope) {
        return ptService.getNowAndTotal(subjectType, teamID, scope);
    }

    @Override
    public long[] addTeamPt(String teamID, String scope, long amount) {
        return ptService.add(subjectType, teamID, scope, amount);
    }

    @Override
    public boolean subtractTeamPt(String teamID, String scope, long amount) {
        return ptService.subtract(subjectType, teamID, scope, amount);
    }

    @Override
    public long[] setTeamPt(String teamID, String scope, long newNow) {
        return ptService.set(subjectType, teamID, scope, newNow);
    }

    @Override
    public void createTeam(String teamID, String scope, String displayName) {
        ptService.ensureAccount(subjectType, teamID, scope, displayName);
    }

    @Override
    public List<RankRow> getTeamDailyTop(String scope, LocalDate day, int limit) {
        return ptService.getDailyTop(subjectType, scope, day, limit);
    }

    @Override
    public List<RankRow> getTeamWeeklyTop(String scope, LocalDate startInclusive, LocalDate endInclusive, int limit) {
        return ptService.getWeeklyTop(subjectType, scope, startInclusive, endInclusive, limit);
    }

    @Override
    public List<RankRow> getTeamGlobalTop(String scope, int limit) {
        return ptService.getGlobalTop(subjectType, scope, limit);
    }

    // ===== 管理系 =====

    @Override
    public void upsertTeam(String teamID, String scope, String displayName, String color, Integer sortOrder, Boolean active) {
        // subjects / accounts / balances を整え、name を最新化
        ptService.ensureAccount(subjectType, teamID, scope, displayName);

        try (var con = ds.getConnection()) {
            long subjectId = findSubjectId(con, teamID);
            try (var ps = con.prepareStatement(
                    "INSERT INTO " + Names.t("team_meta") + " (subject_id, color_code, sort_order, active) VALUES (?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE color_code=VALUES(color_code), sort_order=COALESCE(VALUES(sort_order), sort_order), active=COALESCE(VALUES(active), active)")) {
                ps.setLong(1, subjectId);
                ps.setString(2, color != null ? color : "&f");
                ps.setInt(3, sortOrder != null ? sortOrder : 0);
                ps.setBoolean(4, active != null ? active : true);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void setTeamDisplayName(String teamID, String newName) {
        try (var con = ds.getConnection();
             var ps = con.prepareStatement("UPDATE " + Names.t("subjects") + " SET name=? WHERE type='team' AND subject_key=?")) {
            ps.setString(1, newName);
            ps.setString(2, teamID);
            ps.executeUpdate();
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void setTeamColor(String teamID, String color) {
        try (var con = ds.getConnection()) {
            long subjectId = findSubjectId(con, teamID);
            try (var ps = con.prepareStatement(
                    "INSERT INTO " + Names.t("team_meta") + " (subject_id, color_code) VALUES(?, ?) " +
                            "ON DUPLICATE KEY UPDATE color_code=VALUES(color_code)")) {
                ps.setLong(1, subjectId);
                ps.setString(2, color);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void setTeamSortOrder(String teamID, int sortOrder) {
        try (var con = ds.getConnection()) {
            long subjectId = findSubjectId(con, teamID);
            try (var ps = con.prepareStatement(
                    "INSERT INTO " + Names.t("team_meta") + " (subject_id, color_code, sort_order) VALUES(?, COALESCE((SELECT color_code FROM " + Names.t("team_meta") + " WHERE subject_id=?),'&f'), ?) " +
                            "ON DUPLICATE KEY UPDATE sort_order=?")) {
                ps.setLong(1, subjectId);
                ps.setLong(2, subjectId);
                ps.setInt(3, sortOrder);
                ps.setInt(4, sortOrder);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void setTeamActive(String teamID, boolean active) {
        try (var con = ds.getConnection()) {
            long subjectId = findSubjectId(con, teamID);
            try (var ps = con.prepareStatement(
                    "INSERT INTO " + Names.t("team_meta") + " (subject_id, color_code, active) VALUES(?, COALESCE((SELECT color_code FROM " + Names.t("team_meta") + " WHERE subject_id=?),'&f'), ?) " +
                            "ON DUPLICATE KEY UPDATE active=?")) {
                ps.setLong(1, subjectId);
                ps.setLong(2, subjectId);
                ps.setBoolean(3, active);
                ps.setBoolean(4, active);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public java.util.Optional<TeamData> loadTeam(String teamID) {
        String sql = "SELECT s.subject_key, s.name, tm.color_code " +
                "FROM " + Names.t("team_meta") + " tm JOIN " + Names.t("subjects") + " s ON s.id=tm.subject_id " +
                "WHERE s.type='team' AND s.subject_key=?";
        try (var con = ds.getConnection();
             var ps = con.prepareStatement(sql)) {
            ps.setString(1, teamID);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return java.util.Optional.of(new TeamData(rs.getString(1), rs.getString(2), rs.getString(3)));
                return java.util.Optional.empty();
            }
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public java.util.List<TeamData> loadAllTeamsOrdered() {
        String sql = "SELECT s.subject_key, s.name, tm.color_code " +
                "FROM " + Names.t("team_meta") + " tm JOIN " + Names.t("subjects") + " s ON s.id=tm.subject_id " +
                "WHERE s.type='team' AND tm.active=1 " +
                "ORDER BY tm.sort_order, s.subject_key";
        java.util.List<TeamData> out = new java.util.ArrayList<>();
        try (var con = ds.getConnection();
             var ps = con.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) out.add(new TeamData(rs.getString(1), rs.getString(2), rs.getString(3)));
            return out;
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void deleteTeamEverywhere(String teamID) {
        ptService.deleteSubjectEverywhere(SubjectType.TEAM.name(), teamID);
    }

    @Override
    public void deleteTeamInScope(String teamID, String scope) {
        ptService.deleteSubjectInScope(SubjectType.TEAM.name(), teamID, scope);
    }

    // ===== 内部ヘルパ =====

    private void ensureTeamMetaDDL() {
        String ddl = "CREATE TABLE IF NOT EXISTS " + Names.t("team_meta") + " (" +
                " subject_id BIGINT UNSIGNED NOT NULL," +
                " color_code VARCHAR(16) NOT NULL," +
                " sort_order INT NOT NULL DEFAULT 0," +
                " active     TINYINT(1) NOT NULL DEFAULT 1," +
                " PRIMARY KEY (subject_id)," +
                " CONSTRAINT fk_team_meta_subject FOREIGN KEY (subject_id) REFERENCES " + Names.t("subjects") + "(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (var con = ds.getConnection(); var st = con.createStatement()) {
            st.executeUpdate(ddl);
        } catch (java.sql.SQLException e) { throw new RuntimeException(e); }
    }

    private long findSubjectId(java.sql.Connection con, String teamID) throws java.sql.SQLException {
        try (var ps = con.prepareStatement(
                "SELECT id FROM " + Names.t("subjects") + " WHERE type='team' AND subject_key=?")) {
            ps.setString(1, teamID);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalStateException("team subject not found: " + teamID);
                return rs.getLong(1);
            }
        }
    }

    private Long findSubjectIdOrNullForUpdate(java.sql.Connection con, String teamID) throws java.sql.SQLException {
        try (var ps = con.prepareStatement(
                "SELECT id FROM " + Names.t("subjects") + " WHERE type='team' AND subject_key=? FOR UPDATE")) {
            ps.setString(1, teamID);
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : null; }
        }
    }

    // 可視化データの入れ物
    private static final class TeamVizData {
        final Map<String, Long> points;
        final Map<String, String> colors;
        TeamVizData(Map<String, Long> p, Map<String, String> c) { this.points = p; this.colors = c; }
    }

    // DBアクセスは非同期で1回だけ
    private CompletableFuture<TeamVizData> loadVizDataAsync(String scope) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> points = new java.util.HashMap<>();
            Map<String, String> colors = new java.util.HashMap<>();
            try (var con = ds.getConnection();
                 var ps = con.prepareStatement(
                         "SELECT s.subject_key, tm.color_code, ab.now_point " +
                                 "FROM " + Names.t("subjects") + " s " +
                                 "JOIN " + Names.t("accounts") + " a ON a.subject_id = s.id " +
                                 "JOIN " + Names.t("account_balances") + " ab ON ab.account_id = a.id " +
                                 "LEFT JOIN " + Names.t("team_meta") + " tm ON tm.subject_id = s.id " +
                                 "WHERE s.type=? AND a.scope=?")) {
                ps.setString(1, subjectType);
                ps.setString(2, scope);
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String teamId = rs.getString(1);
                        String color  = rs.getString(2);
                        long now      = rs.getLong(3);
                        points.put(teamId, now);
                        if (color != null) colors.put(teamId, color);
                    }
                }
            } catch (java.sql.SQLException e) {
                throw new RuntimeException(e);
            }
            return new TeamVizData(points, colors);
        }, executor != null ? executor : java.util.concurrent.ForkJoinPool.commonPool());
    }

    @Override
    public void sendBarAndLegendAsync(CommandSender sender,
                                      String scope, int width, java.util.List<String> order) {
        loadVizDataAsync(scope).whenComplete((data, err) -> {
            org.bukkit.Bukkit.getScheduler().runTask(Pointed.getInstance(), () -> {
                if (err != null) {
                    sender.sendMessage(Util.f("&c[Error] 可視化生成に失敗しました: &7{0}", err.getMessage()));
                    return;
                }
                String bar    = TeamManagerUtil.renderBar(data.points, width, data.colors, order);
                String legend = TeamManagerUtil.renderHanrei(data.points, data.colors, order);

                sender.sendMessage(bar);
                sender.sendMessage(legend);
            });
        });
    }

}
