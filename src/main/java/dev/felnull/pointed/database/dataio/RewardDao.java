package dev.felnull.pointed.database.dataio;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.database.Db;
import org.bukkit.inventory.ItemStack;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RewardDao {

    // --- DTO（テーブル1行をJavaで扱うための入れ物） ---

    /** rewards テーブルの1行 */
    public static class RewardRow {
        public int id;
        public String displayName;
        public int pointTypeId;
        public int needPoint;
        public int needMinTotal;
        public boolean repeatable;
        public boolean active;
    }

    /** reward_prerequisites テーブルの1行 */
    public static class PrereqRow {
        public int prereqRewardId;
        public int minObtained;
        public int haveObtained; // subject_rewards からJOINした取得数
    }

    private RewardDao() {} // インスタンス化禁止。static利用前提。


    // --- rewards テーブル関連 ---

    /**
     * リワード定義を保存する（新規 or 更新）。
     * PointTypeId は呼び出し側で既に持っている前提。
     */
    public static boolean saveReward(RewardRow reward) throws SQLException {
        String sql = """
            INSERT INTO rewards(id, display_name, point_type, need_point, need_min_total, repeatable, active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              display_name = VALUES(display_name),
              point_type = VALUES(point_type),
              need_point = VALUES(need_point),
              need_min_total = VALUES(need_min_total),
              repeatable = VALUES(repeatable),
              active = VALUES(active)
        """;

        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, reward.id);
            ps.setString(2, reward.displayName);
            ps.setInt(3, reward.pointTypeId);     // 呼び出し側で確定済み
            ps.setInt(4, reward.needPoint);
            ps.setInt(5, reward.needMinTotal);
            ps.setBoolean(6, reward.repeatable);
            ps.setBoolean(7, reward.active);

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * リワードを FOR UPDATE でロックして取得。
     * → 並列トランザクションで条件変更されないようにする。
     */
    public static RewardRow getRewardForUpdate(Connection con, int rewardId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id, display_name, point_type, need_point, need_min_total, repeatable, active " +
                        "FROM rewards WHERE id=? FOR UPDATE")) {
            ps.setInt(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                RewardRow r = new RewardRow();
                r.id = rs.getInt(1);
                r.displayName = rs.getString(2);
                r.pointTypeId = rs.getInt(3);
                r.needPoint = rs.getInt(4);
                r.needMinTotal = rs.getInt(5);
                r.repeatable = rs.getBoolean(6);
                r.active = rs.getBoolean(7);
                return r;
            }
        }
    }


    // --- 前提条件関連 ---

    /**
     * 指定リワードの前提条件を FOR UPDATE で取得。
     * subject_rewards と JOINして「haveObtained」を埋める。
     */
    public static List<PrereqRow> getPrereqsForUpdate(Connection con, int rewardId, long subjectId) throws SQLException {
        String sql = """
            SELECT rp.prereq_reward_id, rp.min_obtained, COALESCE(sr.obtained,0) AS have
            FROM reward_prerequisites rp
            LEFT JOIN subject_rewards sr
              ON sr.subject_id=? AND sr.reward_id=rp.prereq_reward_id
            WHERE rp.reward_id=? FOR UPDATE
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setInt(2, rewardId);
            List<PrereqRow> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrereqRow p = new PrereqRow();
                    p.prereqRewardId = rs.getInt(1);
                    p.minObtained = rs.getInt(2);
                    p.haveObtained = rs.getInt(3); // ← JOINで取れた実績回数
                    list.add(p);
                }
            }
            return list;
        }
    }

    /**
     * subject_rewards をロックして現在の obtained を取得。
     */
    public static int getSubjectRewardObtainedForUpdate(Connection con, long subjectId, int rewardId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT obtained FROM subject_rewards WHERE subject_id=? AND reward_id=? FOR UPDATE")) {
            ps.setLong(1, subjectId);
            ps.setInt(2, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        }
    }


    // --- subject_points 関連 ---

    /**
     * subject_points をロックして held,total を取得。
     */
    public static int[] getHeldTotalForUpdate(Connection con, long subjectId, int pointTypeId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT held, total FROM subject_points WHERE subject_id=? AND point_type=? FOR UPDATE")) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new int[]{ rs.getInt(1), rs.getInt(2) };
                return new int[]{ 0, 0 };
            }
        }
    }


    // --- リワード受け取り処理 ---

    /**
     * リワードに紐づくアイテムを復元して返す。
     */
    public static List<ItemStack> loadRewardItems(Connection con, int rewardId) throws Exception {
        List<ItemStack> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT item_bytes FROM reward_items WHERE reward_id=? ORDER BY idx ASC")) {
            ps.setInt(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byte[] b = rs.getBytes(1);
                    list.add(ItemStacks.fromBytes(b));
                }
            }
        }
        return list;
    }

    /**
     * subject_rewards に1行挿入 or 既存 obtained+1。
     * 受取時に必ず呼ぶ。
     */
    public static void incrementSubjectReward(Connection con, long subjectId, int rewardId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO subject_rewards(subject_id,reward_id,obtained,last_claimed_at) " +
                        "VALUES (?,?,1,CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE obtained=obtained+1,last_claimed_at=CURRENT_TIMESTAMP")) {
            ps.setLong(1, subjectId);
            ps.setInt(2, rewardId);
            ps.executeUpdate();
        }
    }

    /**
     * held を減算する（残高不足チェックはしない）。
     * → consumeIfEnoughTx を推奨。
     */
    public static void consumeHeld(Connection con, long subjectId, int pointTypeId, int consume) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO subject_points(subject_id,point_type,held,total) VALUES(?,?,?,0) " +
                        "ON DUPLICATE KEY UPDATE held = held - VALUES(held)")) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            ps.setInt(3, consume);
            ps.executeUpdate();
        }
    }

    /**
     * ポイントの加減算を point_ledger に記録。
     * delta は正負どちらでもOK。
     */
    public static void logLedger(Connection con, long subjectId, int pointTypeId, int delta, String reason, String refId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO point_ledger(subject_id,point_type,delta,reason,ref_id) VALUES(?,?,?,?,?)")) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            ps.setInt(3, delta);
            ps.setString(4, reason);
            ps.setString(5, refId);
            ps.executeUpdate();
        }
    }

    /**
     * 残高が十分な場合のみ held を減算する。
     * → アトミックに実行されるので同時実行でも安全。
     */
    public static boolean consumeIfEnoughTx(Connection con, long subjectId, int pointTypeId, int consume) throws SQLException {
        String sql = """
            UPDATE subject_points
               SET held = held - ?
             WHERE subject_id=? AND point_type=? AND held >= ?
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, consume);
            ps.setLong(2, subjectId);
            ps.setInt(3, pointTypeId);
            ps.setInt(4, consume);
            int updated = ps.executeUpdate();
            return updated > 0;
        }
    }
}