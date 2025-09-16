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
    public static class RewardRow {
        public int id;
        public String displayName;
        public int pointTypeId;
        public int needPoint;
        public int needMinTotal;
        public boolean repeatable;
        public boolean active;
    }
    public static class PrereqRow {
        public int prereqRewardId;
        public int minObtained;
    }

    public RewardDao() { }

    public static boolean saveReward(RewardData reward) {
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

            ps.setInt(1, reward.rewardID);
            ps.setString(2, reward.displayName);
            // ここは PointType の ID を解決して入れる
            int pointTypeId = PointTypeDao.ensurePointType(reward.pointTypeName);
            ps.setInt(3, pointTypeId);
            ps.setInt(4, reward.needPoint);
            ps.setInt(5, reward.needMinPoint != null ? reward.needMinPoint : 0);
            ps.setBoolean(6, reward.repeatable);
            ps.setBoolean(7, true); // active デフォルト true

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public RewardRow getRewardForUpdate(Connection con, int rewardId) throws SQLException {
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

    public List<PrereqRow> getPrereqsForUpdate(Connection con, int rewardId, long subjectId) throws SQLException {
        // subject_rewards を左結合して have を得る（ロック）
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT rp.prereq_reward_id, rp.min_obtained, COALESCE(sr.obtained,0) AS have " +
                        "FROM reward_prerequisites rp " +
                        "LEFT JOIN subject_rewards sr ON sr.subject_id=? AND sr.reward_id=rp.prereq_reward_id " +
                        "WHERE rp.reward_id=? FOR UPDATE")) {
            ps.setLong(1, subjectId);
            ps.setInt(2, rewardId);
            List<PrereqRow> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PrereqRow p = new PrereqRow();
                    p.prereqRewardId = rs.getInt(1);
                    p.minObtained = rs.getInt(2);
                    // rs.getInt(3) が have だが、チェックは呼び出し側で
                    list.add(p);
                }
            }
            return list;
        }
    }

    public int getSubjectRewardObtainedForUpdate(Connection con, long subjectId, int rewardId) throws SQLException {
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

    public int[] getHeldTotalForUpdate(Connection con, long subjectId, int pointTypeId) throws SQLException {
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

    public List<ItemStack> loadRewardItems(Connection con, int rewardId) throws Exception {
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

    public void incrementSubjectReward(Connection con, long subjectId, int rewardId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO subject_rewards(subject_id,reward_id,obtained,last_claimed_at) " +
                        "VALUES (?,?,1,CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE obtained=obtained+1,last_claimed_at=CURRENT_TIMESTAMP")) {
            ps.setLong(1, subjectId);
            ps.setInt(2, rewardId);
            ps.executeUpdate();
        }
    }

    public void consumeHeld(Connection con, long subjectId, int pointTypeId, int consume) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO subject_points(subject_id,point_type,held,total) VALUES(?,?,?,0) " +
                        "ON DUPLICATE KEY UPDATE held = held - VALUES(held)")) {
            ps.setLong(1, subjectId);
            ps.setInt(2, pointTypeId);
            ps.setInt(3, consume);
            ps.executeUpdate();
        }
    }

    public void logLedger(Connection con, long subjectId, int pointTypeId, int delta, String reason, String refId) throws SQLException {
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

    /** 残高が十分なときだけ減算する（行が無い or 不足なら false） */
    public static boolean consumeIfEnoughTx(Connection con, long subjectId, int pointTypeId, int consume) throws SQLException {
        String sql =
                "UPDATE subject_points " +
                        "SET held = held - ? " +
                        "WHERE subject_id=? AND point_type=? AND held >= ?";
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