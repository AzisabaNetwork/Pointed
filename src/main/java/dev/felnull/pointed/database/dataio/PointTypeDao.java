package dev.felnull.pointed.database.dataio;

import dev.felnull.pointed.database.Db;

import javax.sql.DataSource;
import java.sql.*;

/*
（Point名 → Subject_id解決）
 */
public class PointTypeDao {
    private final DataSource ds;

    public PointTypeDao(DataSource ds) { this.ds = ds; }

    public static int ensurePointType(String name) throws SQLException {
        try (Connection con = Db.get().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM point_types WHERE name=?")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO point_types(name) VALUES(?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
            }
        }
    }
}