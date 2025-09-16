package dev.felnull.pointed.database.dataio;

import dev.felnull.pointed.database.Db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SubjectPointsDao {

    public SubjectPointsDao() {}

    /** 原子的に加算（totalは加算時のみ増やす仕様） */
    public static void addPoints(long subjectId, int pointTypeId, int delta, String reason, String refId) throws SQLException {
        try (Connection con = Db.get().getConnection()) {
            con.setAutoCommit(false);
            try {
                // 監査ログ
                try (PreparedStatement log = con.prepareStatement(
                        "INSERT INTO point_ledger(subject_id, point_type, delta, reason, ref_id) VALUES(?,?,?,?,?)")) {
                    log.setLong(1, subjectId);
                    log.setInt(2, pointTypeId);
                    log.setInt(3, delta);
                    log.setString(4, reason);
                    log.setString(5, refId);
                    log.executeUpdate();
                }
                // 現在値
                try (PreparedStatement up = con.prepareStatement(
                        "INSERT INTO subject_points(subject_id, point_type, held, total) VALUES(?,?,?,?) " +
                                "ON DUPLICATE KEY UPDATE " +
                                "  held  = held  + VALUES(held)," +
                                "  total = total + VALUES(total)")) {
                    up.setLong(1, subjectId);
                    up.setInt(2, pointTypeId);
                    up.setInt(3, delta);
                    up.setInt(4, Math.max(delta, 0));
                    up.executeUpdate();
                }
                con.commit();
            } catch (Throwable t) {
                con.rollback();
                throw t;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /** 現在値の取得（name→held/totalのMapを返すためJOIN） */
    public static Map<String, int[]> loadAll(long subjectId) throws SQLException {
        Map<String, int[]> out = new HashMap<>();
        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT pt.name, sp.held, sp.total " +
                             "FROM subject_points sp JOIN point_types pt ON pt.id = sp.point_type " +
                             "WHERE sp.subject_id=?")) {
            ps.setLong(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), new int[]{ rs.getInt(2), rs.getInt(3) });
                }
            }
        }
        return out; // value[0]=held, value[1]=total
    }

    /** subject_id × point_type_id からこのプレイヤーの 現在保持ポイント と 累計ポイントを個別に取得（存在しなければ {0,0} を返す） */
    public static int[] loadOne(long subjectId, int pointTypeId) throws SQLException {
        String sql = "SELECT held, total FROM subject_points WHERE subject_id=? AND point_type=?";
        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{ rs.getInt(1), rs.getInt(2) };
                } else {
                    return new int[]{ 0, 0 };
                }
            }
        }
    }

    /** subject_id × point_type_name("EVENT_POINT"みたいな) からこのプレイヤーの 現在保持ポイント と 累計ポイントを個別に取得（存在しなければ {0,0} を返す） */
    public static int[] loadOneByName(long subjectId, String pointTypeName) throws SQLException {
        String sql =
                "SELECT sp.held, sp.total " +
                        "FROM subject_points sp " +
                        "JOIN point_types pt ON pt.id = sp.point_type " +
                        "WHERE sp.subject_id=? AND pt.name=?";
        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setString(2, pointTypeName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{ rs.getInt(1), rs.getInt(2) };
                } else {
                    return new int[]{ 0, 0 };
                }
            }
        }
    }

    /** FOR UPDATE 版（トランザクション中の検証や更新の直前に使う） トランザクション中に「この subject のこのポイント」を 排他ロック（FOR UPDATE）付きで取得する。 */
    public static int[] loadOneForUpdate(Connection con, long subjectId, int pointTypeId) throws SQLException {
        String sql = "SELECT held, total FROM subject_points WHERE subject_id=? AND point_type=? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{ rs.getInt(1), rs.getInt(2) };
                } else {
                    return new int[]{ 0, 0 };
                }
            }
        }
    }

    /** 強制データ上書き（なければINSERT） */
    public static void upsertExact(long subjectId, int pointTypeId, int held, int total) throws SQLException {
        String sql =
                "INSERT INTO subject_points(subject_id, point_type, held, total) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE held=VALUES(held), total=VALUES(total)";
        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            ps.setInt(3, held);
            ps.setInt(4, total);
            ps.executeUpdate();
        }
    }

    /** 加算：存在しなければINSERT、あれば held/total 両方に加算 */
    public static void addPoints(long subjectId, int pointTypeId, int add) throws SQLException {
        if (add == 0) return;
        String sql =
                "INSERT INTO subject_points(subject_id, point_type, held, total) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  held  = held  + VALUES(held), " +
                        "  total = total + VALUES(total)";
        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            ps.setInt(3, add);
            ps.setInt(4, add);
            ps.executeUpdate();
        }
    }

    /** トランザクション用の加算（外からConnectionを渡す版） */
    public static void addPointsTx(Connection con, long subjectId, int pointTypeId, int add) throws SQLException {
        if (add == 0) return;
        String sql =
                "INSERT INTO subject_points(subject_id, point_type, held, total) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "  held  = held  + VALUES(held), " +
                        "  total = total + VALUES(total)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            ps.setInt(3, add);
            ps.setInt(4, add);
            ps.executeUpdate();
        }
    }


}