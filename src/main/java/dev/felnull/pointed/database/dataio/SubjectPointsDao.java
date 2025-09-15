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
}