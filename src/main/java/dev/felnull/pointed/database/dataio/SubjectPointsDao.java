package dev.felnull.pointed.database.dataio;

import dev.felnull.pointed.database.Db;

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

    /** 強制上書き（存在しなければINSERT）＋差分ログ
     *  - ledger には「held の差分」を delta として記録
     *  - total は “累計は減らさない” ルールに従い、現在値より小さい値を指定されたら据え置きにする
     *    （= 実際に保存される newTotalEffective = max(requestedTotal, currentTotal)）
     */
    public static void upsertExactWithLedger(long subjectId, int pointTypeId,
                                             int newHeld,
                                             String reason, String refId) throws SQLException {
        try (Connection con = Db.get().getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1) 現在値をロックして取得
                int[] cur = loadOneForUpdate(con, subjectId, pointTypeId); // [held,total] or {0,0}
                int curHeld  = cur[0];
                int curTotal = cur[1];

                // 2) 差分計算（ledger は held の差分だけを記録する）
                int deltaHeld  = newHeld  - curHeld;

                // 累計は減らさないポリシー：指定が小さければ据え置き
                int newTotalEffective = Math.max(newTotal, curTotal);

                // 3) 監査ログ（差分が 0 のときはログ省略してもOK。必要なら 0 でも記録して可）
                if (deltaHeld != 0) {
                    try (PreparedStatement log = con.prepareStatement(
                            "INSERT INTO point_ledger(subject_id, point_type, delta, reason, ref_id) VALUES(?,?,?,?,?)")) {
                        log.setLong(1, subjectId);
                        log.setInt(2, pointTypeId);
                        log.setInt(3, deltaHeld);   // ← 差分
                        log.setString(4, reason);
                        log.setString(5, refId);
                        log.executeUpdate();
                    }
                }

                // 4) 強制上書き（存在しなければ挿入）
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO subject_points(subject_id, point_type, held, total) VALUES (?,?,?,?) " +
                                "ON DUPLICATE KEY UPDATE held=VALUES(held), total=VALUES(total)")) {
                    ps.setLong(1, subjectId);
                    ps.setInt(2, pointTypeId);
                    ps.setInt(3, newHeld);
                    ps.setInt(4, newTotalEffective);
                    ps.executeUpdate();
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

    /** 減算（total は減らさない。存在しなければ何もしない） */
    public static boolean subtractPoints(long subjectId, int pointTypeId, int sub, String reason, String refId) throws SQLException {
        if (sub <= 0) return false; // マイナス値は無効

        try (Connection con = Db.get().getConnection()) {
            con.setAutoCommit(false);
            try {
                // 残高チェック & ロック
                int[] current = loadOneForUpdate(con, subjectId, pointTypeId);
                if (current[0] < sub) {
                    con.rollback();
                    return false; // 残高不足
                }

                // ログ記録
                try (PreparedStatement log = con.prepareStatement(
                        "INSERT INTO point_ledger(subject_id, point_type, delta, reason, ref_id) VALUES(?,?,?,?,?)")) {
                    log.setLong(1, subjectId);
                    log.setInt(2, pointTypeId);
                    log.setInt(3, -sub); // 減算なのでマイナス
                    log.setString(4, reason);
                    log.setString(5, refId);
                    log.executeUpdate();
                }

                // held だけ更新
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE subject_points SET held = held - ? WHERE subject_id=? AND point_type=?")) {
                    ps.setInt(1, sub);
                    ps.setLong(2, subjectId);
                    ps.setInt(3, pointTypeId);
                    ps.executeUpdate();
                }

                con.commit();
                return true;
            } catch (Throwable t) {
                con.rollback();
                throw t;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

}