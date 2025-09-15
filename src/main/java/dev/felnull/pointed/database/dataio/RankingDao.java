package dev.felnull.pointed.database.dataio;

import javax.sql.DataSource;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;



public class RankingDao {
    private final DataSource ds;
    public RankingDao(DataSource ds) { this.ds = ds; }

    /** 上位N人を total DESC で取得 */
    public List<RankingEntry> topNByTotal(int pointTypeId, int limit) throws SQLException {
        String sql = """
            SELECT s.id, s.player_uuid, COALESCE(s.name,'unknown') AS name,
                   sp.held, sp.total
            FROM subject_points sp
            JOIN subjects s ON s.id = sp.subject_id AND s.type='PLAYER'
            WHERE sp.point_type = ?
            ORDER BY sp.total DESC, sp.updated_at DESC, s.id ASC
            LIMIT ?
        """;
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, pointTypeId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<RankingEntry> list = new ArrayList<>();
                while (rs.next()) {
                    long sid = rs.getLong(1);
                    UUID uuid = bytesToUuid(rs.getBytes(2));
                    String name = rs.getString(3);
                    int held = rs.getInt(4);
                    int total = rs.getInt(5);
                    list.add(new RankingEntry(sid, uuid, name, held, total));
                }
                return list;
            }
        }
    }

    /** 自分の順位と累計を返す [0]=rank, [1]=total */
    public int[] myRankAndTotal(long subjectId, int pointTypeId) throws SQLException {
        try (Connection con = ds.getConnection()) {
            int myTotal = 0;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT total FROM subject_points WHERE subject_id=? AND point_type=?")) {
                ps.setLong(1, subjectId);
                ps.setInt(2, pointTypeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) myTotal = rs.getInt(1);
                }
            }
            int rank = 0;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 + COUNT(*) FROM subject_points WHERE point_type=? AND total > ?")) {
                ps.setInt(1, pointTypeId);
                ps.setInt(2, myTotal);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) rank = rs.getInt(1);
                }
            }
            return new int[]{rank, myTotal};
        }
    }

    private static UUID bytesToUuid(byte[] b) {
        if (b == null || b.length != 16) return null;
        ByteBuffer bb = ByteBuffer.wrap(b);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }
}

