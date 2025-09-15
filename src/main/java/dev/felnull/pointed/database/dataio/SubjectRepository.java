package dev.felnull.pointed.database.dataio;

import dev.felnull.pointed.util.Util;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;

/*
（プレイヤー/チーム → subject_id 解決）
 */
public class SubjectRepository {
    private final DataSource ds;

    public SubjectRepository(DataSource ds) { this.ds = ds; }

    public long ensurePlayer(UUID uuid, String displayName) throws SQLException {
        try (Connection con = ds.getConnection()) {
            // 既存
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM subjects WHERE player_uuid=?")) {
                ps.setBytes(1, Util.uuidToBytes(uuid));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
            // 作成
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO subjects(type,name,player_uuid) VALUES('PLAYER',?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, displayName);
                ps.setBytes(2, Util.uuidToBytes(uuid));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
            }
        }
    }

    public long ensureTeam(String teamKey, String displayName) throws SQLException {
        try (Connection con = ds.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM subjects WHERE team_key=?")) {
                ps.setString(1, teamKey);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO subjects(type,name,team_key) VALUES('TEAM',?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, displayName);
                ps.setString(2, teamKey);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getLong(1); }
            }
        }
    }
}