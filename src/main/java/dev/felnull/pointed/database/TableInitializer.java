package dev.felnull.pointed.database;

import java.sql.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

public final class TableInitializer {

    private static final Logger LOGGER = Logger.getLogger("Pointed");

    public static void initTables() {
        try (Connection conn = Db.get().getConnection();
             Statement stmt = conn.createStatement()) {

            // ========== 1) 骨格だけCREATE（最小列） ==========
            // subjects
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subjects (" +
                    " id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // point_types
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS point_types (" +
                    " id INT AUTO_INCREMENT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // subject_points
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subject_points (" +
                    " subject_id BIGINT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // rewards
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS rewards (" +
                    " id INT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // reward_items
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reward_items (" +
                    " reward_id INT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // reward_prerequisites
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reward_prerequisites (" +
                    " reward_id INT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // subject_rewards
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS subject_rewards (" +
                    " subject_id BIGINT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // point_ledger
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS point_ledger (" +
                    " id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // ========== 2) 不足カラムを追加-only で補完 ==========
            // subjects
            addColumnIfNotExists(conn, "subjects", "type",        "VARCHAR(16) NOT NULL"); // 'PLAYER' or 'TEAM'
            addColumnIfNotExists(conn, "subjects", "name",        "VARCHAR(255)");
            addColumnIfNotExists(conn, "subjects", "player_uuid", "BINARY(16) NULL");
            addColumnIfNotExists(conn, "subjects", "team_key",    "VARCHAR(128) NULL");
            addColumnIfNotExists(conn, "subjects", "created_at",  "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            addColumnIfNotExists(conn, "subjects", "updated_at",  "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

            // point_types
            addColumnIfNotExists(conn, "point_types", "name", "VARCHAR(64) NOT NULL");

            // subject_points
            addColumnIfNotExists(conn, "subject_points", "point_type", "INT NOT NULL");
            addColumnIfNotExists(conn, "subject_points", "held",       "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "subject_points", "total",      "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "subject_points", "updated_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

            // rewards
            addColumnIfNotExists(conn, "rewards", "display_name",  "VARCHAR(255) NOT NULL");
            addColumnIfNotExists(conn, "rewards", "point_type",     "INT NOT NULL");
            addColumnIfNotExists(conn, "rewards", "need_point",     "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "rewards", "need_min_total", "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "rewards", "repeatable",     "TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "rewards", "active",         "TINYINT(1) NOT NULL DEFAULT 1");

            // reward_items
            addColumnIfNotExists(conn, "reward_items", "idx",        "INT NOT NULL");
            addColumnIfNotExists(conn, "reward_items", "item_bytes", "LONGBLOB NOT NULL");

            // reward_prerequisites
            addColumnIfNotExists(conn, "reward_prerequisites", "prereq_reward_id", "INT NOT NULL");
            addColumnIfNotExists(conn, "reward_prerequisites", "min_obtained",     "INT NOT NULL DEFAULT 1");

            // subject_rewards
            addColumnIfNotExists(conn, "subject_rewards", "reward_id",        "INT NOT NULL");
            addColumnIfNotExists(conn, "subject_rewards", "obtained",         "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "subject_rewards", "last_claimed_at",  "TIMESTAMP NULL DEFAULT NULL");

            // point_ledger
            addColumnIfNotExists(conn, "point_ledger", "subject_id", "BIGINT NOT NULL");
            addColumnIfNotExists(conn, "point_ledger", "point_type", "INT NOT NULL");
            addColumnIfNotExists(conn, "point_ledger", "delta",      "INT NOT NULL");
            addColumnIfNotExists(conn, "point_ledger", "reason",     "VARCHAR(64)");
            addColumnIfNotExists(conn, "point_ledger", "ref_id",     "VARCHAR(64)");
            addColumnIfNotExists(conn, "point_ledger", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");

            // ========== 3) 主キー・ユニーク・インデックス ==========
            // subjects
            ensureUniqueIndex(conn, "subjects", "uniq_subject_player_uuid", new String[]{"player_uuid"});
            ensureUniqueIndex(conn, "subjects", "uniq_subject_team_key",    new String[]{"team_key"});
            ensureIndex(conn, "subjects", "idx_subject_type", new String[]{"type"});

            // point_types
            ensureUniqueIndex(conn, "point_types", "uniq_point_types_name", new String[]{"name"});

            // subject_points: PrimaryKey(subject_id,point_type)
            ensurePrimaryKey(conn, "subject_points", new String[]{"subject_id","point_type"});
            ensureIndex(conn, "subject_points", "idx_sp_updated", new String[]{"updated_at"});

            // rewards
            ensureIndex(conn, "rewards", "idx_rewards_pointtype", new String[]{"point_type"});
            ensureIndex(conn, "rewards", "idx_rewards_active",    new String[]{"active"});

            // reward_items: PrimaryKey(reward_id, idx)
            ensurePrimaryKey(conn, "reward_items", new String[]{"reward_id","idx"});

            // reward_prerequisites: PrimaryKey(reward_id, prereq_reward_id)
            ensurePrimaryKey(conn, "reward_prerequisites", new String[]{"reward_id","prereq_reward_id"});

            // subject_rewards: PrimaryKey(subject_id, reward_id)
            ensurePrimaryKey(conn, "subject_rewards", new String[]{"subject_id","reward_id"});
            ensureIndex(conn, "subject_rewards", "idx_sr_last_claimed", new String[]{"last_claimed_at"});

            // point_ledger
            ensureIndex(conn, "point_ledger", "idx_ledger_subject",   new String[]{"subject_id"});
            ensureIndex(conn, "point_ledger", "idx_ledger_pointtype", new String[]{"point_type"});
            ensureIndex(conn, "point_ledger", "idx_ledger_created",   new String[]{"created_at"});

            // ========== 4) 外部キー（存在しないときのみ付与） ==========
            // 注意: 本番運用中の既存foreignKeyは壊さない方針。無ければ付与。
            ensureForeignKey(conn, "subject_points", "fk_sp_subject",
                    "subject_id", "subjects", "id", "CASCADE", "CASCADE");
            ensureForeignKey(conn, "subject_points", "fk_sp_pointtype",
                    "point_type", "point_types", "id", "RESTRICT", "CASCADE");

            ensureForeignKey(conn, "rewards", "fk_rewards_pointtype",
                    "point_type", "point_types", "id", "RESTRICT", "CASCADE");

            ensureForeignKey(conn, "reward_items", "fk_ri_reward",
                    "reward_id", "rewards", "id", "CASCADE", "CASCADE");

            ensureForeignKey(conn, "reward_prerequisites", "fk_rp_reward",
                    "reward_id", "rewards", "id", "CASCADE", "CASCADE");
            ensureForeignKey(conn, "reward_prerequisites", "fk_rp_prereq",
                    "prereq_reward_id", "rewards", "id", "RESTRICT", "CASCADE");

            ensureForeignKey(conn, "subject_rewards", "fk_sr_subject",
                    "subject_id", "subjects", "id", "CASCADE", "CASCADE");
            ensureForeignKey(conn, "subject_rewards", "fk_sr_reward",
                    "reward_id", "rewards", "id", "RESTRICT", "CASCADE");

            ensureForeignKey(conn, "point_ledger", "fk_pl_subject",
                    "subject_id", "subjects", "id", "CASCADE", "CASCADE");
            ensureForeignKey(conn, "point_ledger", "fk_pl_pointtype",
                    "point_type", "point_types", "id", "RESTRICT", "CASCADE");

            LOGGER.info("[Pointed] テーブル初期化（骨格→列補完→制約付与）完了！");

        } catch (SQLException e) {
            LOGGER.warning("[Pointed] テーブル初期化中にエラー: " + e.getMessage());
        }
    }

    // ========= ユーティリティ =========

    private static void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnDefinition) {
        try {
            boolean exists = false;
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, tableName, columnName)) {
                exists = rs.next();
            }
            if (!exists) {
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                    exists = rs.next();
                }
            }
            if (exists) return;

            String sql = "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + columnDefinition;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
                LOGGER.info("[Pointed] " + tableName + " にカラム '" + columnName + "' を追加");
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] addColumn 失敗 (" + tableName + "." + columnName + "): " + e.getMessage());
        }
    }

    private static void ensurePrimaryKey(Connection conn, String table, String[] columns) {
        try {
            Set<String> existing = new LinkedHashSet<String>();
            try (ResultSet rs = conn.getMetaData().getPrimaryKeys(null, null, table)) {
                while (rs.next()) existing.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            Set<String> target = new LinkedHashSet<String>();
            for (String c : columns) target.add(c.toLowerCase());
            if (existing.equals(target)) return;
            if (!existing.isEmpty()) {
                LOGGER.warning("[Pointed] " + table + " に既存PrimaryKeyがあり変更しない");
                return;
            }
            String cols = joinBackticked(columns);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE `" + table + "` ADD PRIMARY KEY (" + cols + ")");
                LOGGER.info("[Pointed] " + table + " に PRIMARY KEY 付与: " + Arrays.toString(columns));
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] ensurePrimaryKey 失敗 (" + table + "): " + e.getMessage());
        }
    }

    private static void ensureUniqueIndex(Connection conn, String table, String indexName, String[] columns) {
        try {
            if (indexExists(conn, table, indexName)) return;
            String cols = joinBackticked(columns);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE UNIQUE INDEX `" + indexName + "` ON `" + table + "`(" + cols + ")");
                LOGGER.info("[Pointed] " + table + " に UNIQUE INDEX " + indexName + " 作成");
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] ensureUniqueIndex 失敗 (" + table + "/" + indexName + "): " + e.getMessage());
        }
    }

    private static void ensureIndex(Connection conn, String table, String indexName, String[] columns) {
        try {
            if (indexExists(conn, table, indexName)) return;
            String cols = joinBackticked(columns);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE INDEX `" + indexName + "` ON `" + table + "`(" + cols + ")");
                LOGGER.info("[Pointed] " + table + " に INDEX " + indexName + " 作成");
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] ensureIndex 失敗 (" + table + "/" + indexName + "): " + e.getMessage());
        }
    }

    private static boolean indexExists(Connection conn, String table, String indexName) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        try (ResultSet rs = md.getIndexInfo(null, null, table, false, false)) {
            while (rs.next()) {
                String existing = rs.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(existing)) return true;
            }
        }
        return false;
    }

    private static void ensureForeignKey(Connection conn, String table, String foreignKeyName,
                                         String col, String refTable, String refCol,
                                         String onDelete, String onUpdate) {
        try {
            if (foreignKeyExists(conn, table, foreignKeyName)) return;
            String sql = "ALTER TABLE `" + table + "` " +
                    "ADD CONSTRAINT `" + foreignKeyName + "` FOREIGN KEY (`" + col + "`) " +
                    "REFERENCES `" + refTable + "`(`" + refCol + "`) " +
                    "ON DELETE " + onDelete + " ON UPDATE " + onUpdate;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
                LOGGER.info("[Pointed] " + table + " に ForeignKey " + foreignKeyName + " 付与");
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] ensureForeignKey 失敗 (" + table + "/" + foreignKeyName + "): " + e.getMessage());
        }
    }

    private static boolean foreignKeyExists(Connection conn, String table, String foreignKey) throws SQLException {
        // INFORMATION_SCHEMA からforeignKeyの存在を確認
        String sql = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS " +
                "WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = ? AND TABLE_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, foreignKey);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String joinBackticked(String[] columns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("`").append(columns[i]).append("`");
        }
        return sb.toString();
    }

    private TableInitializer() {}
}