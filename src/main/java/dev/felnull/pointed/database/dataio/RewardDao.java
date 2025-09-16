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
        public Integer id;
        public String displayName;
        public int pointTypeId;
        public Integer needPoint;
        public Integer needMinTotal;
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
                r.id = (Integer) rs.getInt(1);
                r.displayName = rs.getString(2);
                r.pointTypeId = rs.getInt(3);
                r.needPoint = (Integer) rs.getInt(4);
                r.needMinTotal = (Integer) rs.getInt(5);
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
     * subject_rewards をロックして現在の obtained(受け取った回数) を取得。
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

    public static List<RewardDao.RewardRow> listByPointTypeId(int pointTypeId, boolean onlyActive) throws SQLException {
        String sql =
                "SELECT id, display_name, point_type, need_point, need_min_total, repeatable, active " +
                        "FROM rewards WHERE point_type=? " + (onlyActive ? "AND active=1 " : "") +
                        "ORDER BY need_point ASC, id ASC";

        List<RewardDao.RewardRow> out = new ArrayList<RewardDao.RewardRow>();
        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pointTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RewardDao.RewardRow r = new RewardDao.RewardRow();
                    r.id           = (Integer) rs.getInt(1);
                    r.displayName  = rs.getString(2);
                    r.pointTypeId  = rs.getInt(3);
                    r.needPoint    = (Integer) rs.getInt(4);
                    r.needMinTotal = (Integer) rs.getInt(5);
                    r.repeatable   = rs.getBoolean(6);
                    r.active       = rs.getBoolean(7);
                    out.add(r);
                }
            }
        }
        return out;
    }

    /** 一覧取得のオプション */
    public static class ListOpts {
        /** どちらか片方を指定（優先: pointTypeId） */
        public Integer pointTypeId;      // 例: 1
        public String  pointTypeName;    // 例: "EVENT_POINT"

        /** 有効なものだけに絞る（デフォルト false） */
        public boolean onlyActive = false;

        /** ページング：limit 未指定なら全件、offset デフォルト 0 */
        public Integer limit;            // 例: 20
        public Integer offset = (Integer) 0;

        /** RewardData 生成時にアイテムも読み込むか */
        public boolean includeItems = true;
    }

    /** 生の RewardRow 一覧（軽量） */
    public static List<RewardRow> listRows(ListOpts opts) throws SQLException {
        if (opts == null) throw new IllegalArgumentException("opts is null");
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<Object>();

        // pointTypeName 指定なら JOIN、Id 指定なら単純 WHERE
        if (opts.pointTypeId != null) {
            sql.append("SELECT id, display_name, point_type, need_point, need_min_total, repeatable, active ")
                    .append("FROM rewards WHERE point_type=? ");
            params.add(opts.pointTypeId);
            if (opts.onlyActive) sql.append("AND active=1 ");
        } else if (opts.pointTypeName != null) {
            sql.append("SELECT r.id, r.display_name, r.point_type, r.need_point, r.need_min_total, r.repeatable, r.active ")
                    .append("FROM rewards r JOIN point_types pt ON pt.id = r.point_type ")
                    .append("WHERE pt.name=? ");
            params.add(opts.pointTypeName);
            if (opts.onlyActive) sql.append("AND r.active=1 ");
        } else {
            throw new IllegalArgumentException("pointTypeId or pointTypeName is required");
        }

        sql.append("ORDER BY need_point ASC, id ASC ");
        if (opts.limit != null) {
            sql.append("LIMIT ? OFFSET ? ");
            params.add(opts.limit);
            params.add(opts.offset);
        }

        List<RewardRow> out = new ArrayList<RewardRow>();
        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = prepare(con, sql.toString(), params)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RewardRow r = new RewardRow();
                    r.id           = (Integer) rs.getInt(1);
                    r.displayName  = rs.getString(2);
                    r.pointTypeId  = rs.getInt(3);
                    r.needPoint    = (Integer) rs.getInt(4);
                    r.needMinTotal = (Integer) rs.getInt(5);
                    r.repeatable   = rs.getBoolean(6);
                    r.active       = rs.getBoolean(7);
                    out.add(r);
                }
            }
        }
        return out;
    }

    /** GUI向け：RewardData にして返す（必要ならアイテムも同時ロード） */
    public static List<RewardData> listData(ListOpts opts) throws SQLException {
        List<RewardRow> rows = listRows(opts);
        String pointTypeNameForData = opts.pointTypeName;

        List<RewardData> out = new ArrayList<RewardData>();
        try (Connection con = Db.get().getConnection()) {
            for (RewardRow r : rows) {
                List<ItemStack> items = new ArrayList<ItemStack>();
                if (opts.includeItems) {
                    try {
                        items = loadRewardItems(con, r.id); // 既存メソッド
                    } catch (Exception ignore) {
                        items = new ArrayList<ItemStack>();
                    }
                }
                RewardData d = new RewardData(
                        r.id,
                        r.displayName,
                        pointTypeNameForData, // あればセット
                        r.needPoint,
                        r.needMinTotal,
                        r.repeatable,
                        r.active,
                        items
                );
                out.add(d);
            }
        }
        return out;
    }

    /** 総件数（ページングUI用） */
    public static int count(ListOpts opts) throws SQLException {
        if (opts == null) throw new IllegalArgumentException("opts is null");
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<Object>();

        if (opts.pointTypeId != null) {
            sql.append("SELECT COUNT(*) FROM rewards WHERE point_type=? ");
            params.add(opts.pointTypeId);
            if (opts.onlyActive) sql.append("AND active=1 ");
        } else if (opts.pointTypeName != null) {
            sql.append("SELECT COUNT(*) FROM rewards r JOIN point_types pt ON pt.id=r.point_type WHERE pt.name=? ");
            params.add(opts.pointTypeName);
            if (opts.onlyActive) sql.append("AND r.active=1 ");
        } else {
            throw new IllegalArgumentException("pointTypeId or pointTypeName is required");
        }

        try (Connection con = Db.get().getConnection();
             PreparedStatement ps = prepare(con, sql.toString(), params);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ---- 小さなユーティリティ ----
    private static PreparedStatement prepare(Connection con, String sql, List<Object> params) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        for (int i = 0; i < params.size(); i++) {
            Object v = params.get(i);
            int idx = i + 1;
            if (v instanceof Integer)      ps.setInt(idx, ((Integer) v).intValue());
            else if (v instanceof Long)    ps.setLong(idx, ((Long) v).longValue());
            else if (v instanceof String)  ps.setString(idx, (String) v);
            else                           ps.setObject(idx, v);
        }
        return ps;
    }
}