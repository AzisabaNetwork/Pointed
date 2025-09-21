package dev.felnull.pointed.core.database;

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
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("subjects") + " (" +
                    " id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // accounts
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("accounts") + " (" +
                    " id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // account_balances
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("account_balances") + " (" +
                    " account_id BIGINT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // account_daily
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("account_daily") + " (" +
                    " account_id BIGINT NOT NULL," +
                    " day DATE NOT NULL," +
                    " PRIMARY KEY (account_id, day)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // rewards
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("rewards") + " (" +
                    " id INT AUTO_INCREMENT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // reward_items
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("reward_items") + " (" +
                    " reward_id INT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // reward_prerequisites
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("reward_prerequisites") + " (" +
                    " reward_id INT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // subject_rewards
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("subject_rewards") + " (" +
                    " subject_id BIGINT NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // point_ledger
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("point_ledger") + " (" +
                    " id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // ====Team====
            // team_meta（骨格）
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("team_meta") + " (" +
                    " subject_id BIGINT NOT NULL," +
                    " PRIMARY KEY (subject_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // ========== 2) 不足カラムを追加 ==========

            // subjects
            addColumnIfNotExists(conn, "subjects", "type",       "VARCHAR(16) NOT NULL"); // PLAYER/TEAM
            addColumnIfNotExists(conn, "subjects", "subject_key","VARCHAR(128) NOT NULL");
            addColumnIfNotExists(conn, "subjects", "name",       "VARCHAR(255)");
            addColumnIfNotExists(conn, "subjects", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            addColumnIfNotExists(conn, "subjects", "updated_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

            // accounts
            addColumnIfNotExists(conn, "accounts", "subject_id", "BIGINT NOT NULL");
            addColumnIfNotExists(conn, "accounts", "scope",      "VARCHAR(64) NOT NULL");

            // account_balances
            addColumnIfNotExists(conn, "account_balances", "now_point",   "BIGINT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "account_balances", "total_point", "BIGINT NOT NULL DEFAULT 0");

            // account_daily
            addColumnIfNotExists(conn, "account_daily", "gained", "BIGINT NOT NULL DEFAULT 0");

            // rewards
            addColumnIfNotExists(conn, "rewards", "display_name",  "VARCHAR(255) NOT NULL");
            addColumnIfNotExists(conn, "rewards", "need_point",    "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "rewards", "need_min_total","INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "rewards", "repeatable",    "TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "rewards", "active",        "TINYINT(1) NOT NULL DEFAULT 1");

            // reward_items
            addColumnIfNotExists(conn, "reward_items", "idx",        "INT NOT NULL");
            addColumnIfNotExists(conn, "reward_items", "item_bytes", "LONGBLOB NOT NULL");

            // reward_prerequisites
            addColumnIfNotExists(conn, "reward_prerequisites", "prereq_reward_id", "INT NOT NULL");
            addColumnIfNotExists(conn, "reward_prerequisites", "min_obtained",     "INT NOT NULL DEFAULT 1");

            // subject_rewards
            addColumnIfNotExists(conn, "subject_rewards", "reward_id",       "INT NOT NULL");
            addColumnIfNotExists(conn, "subject_rewards", "obtained",        "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "subject_rewards", "last_claimed_at", "TIMESTAMP NULL DEFAULT NULL");

            // point_ledger
            addColumnIfNotExists(conn, "point_ledger", "account_id", "BIGINT NOT NULL");
            addColumnIfNotExists(conn, "point_ledger", "delta",      "BIGINT NOT NULL");
            addColumnIfNotExists(conn, "point_ledger", "reason",     "VARCHAR(64)");
            addColumnIfNotExists(conn, "point_ledger", "ref_id",     "VARCHAR(64)");
            addColumnIfNotExists(conn, "point_ledger", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");

            // ==== Team ====
            // team_meta
            addColumnIfNotExists(conn, "team_meta", "color_code", "VARCHAR(16) NOT NULL");
            addColumnIfNotExists(conn, "team_meta", "sort_order", "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "team_meta", "active",     "TINYINT(1) NOT NULL DEFAULT 1");

            // ========== 3) インデックス・制約 ==========

            ensureUniqueIndex(conn, "subjects", "uniq_subject_key", new String[]{"type","subject_key"});

            ensureUniqueIndex(conn, "accounts", "uniq_account_scope", new String[]{"subject_id","scope"});

            ensureForeignKey(conn, "accounts", "fk_acc_subject",
                    "subject_id", "subjects", "id", "CASCADE", "CASCADE");

            ensureForeignKey(conn, "account_balances", "fk_ab_acc",
                    "account_id", "accounts", "id", "CASCADE", "CASCADE");

            ensureForeignKey(conn, "account_daily", "fk_ad_acc",
                    "account_id", "accounts", "id", "CASCADE", "CASCADE");

            ensureForeignKey(conn, "subject_rewards", "fk_sr_subject",
                    "subject_id", "subjects", "id", "CASCADE", "CASCADE");
            ensureForeignKey(conn, "subject_rewards", "fk_sr_reward",
                    "reward_id", "rewards", "id", "RESTRICT", "CASCADE");

            ensureForeignKey(conn, "reward_items", "fk_ri_reward",
                    "reward_id", "rewards", "id", "CASCADE", "CASCADE");

            ensureForeignKey(conn, "reward_prerequisites", "fk_rp_reward",
                    "reward_id", "rewards", "id", "CASCADE", "CASCADE");
            ensureForeignKey(conn, "reward_prerequisites", "fk_rp_prereq",
                    "prereq_reward_id", "rewards", "id", "RESTRICT", "CASCADE");

            ensureForeignKey(conn, "point_ledger", "fk_pl_acc",
                    "account_id", "accounts", "id", "CASCADE", "CASCADE");

            // subjects(id) へのFK。subjects 削除時に team_meta も自動削除
            ensureForeignKey(conn, "team_meta", "fk_tm_subject",
                    "subject_id", "subjects", "id", "CASCADE", "CASCADE");

            // 並び順・有効/無効フィルタの補助INDEX
            ensureIndex(conn, "team_meta", "idx_tm_sort",   new String[]{"sort_order"});
            ensureIndex(conn, "team_meta", "idx_tm_active", new String[]{"active"});

            LOGGER.info("[Pointed] テーブル初期化完了！");

        } catch (SQLException e) {
            LOGGER.warning("[Pointed] テーブル初期化中にエラー: " + e.getMessage());
        }
    }

    // ========= ユーティリティ =========

    public static void addColumnIfNotExists(Connection conn, String baseTable, String columnName, String columnDefinition) {
        String phys = Names.phys(baseTable); // バッククォートなし実名
        try {
            boolean exists = false;
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, phys, columnName)) {
                exists = rs.next();
            }
            if (!exists) {
                try (ResultSet rs = md.getColumns(null, null, phys.toUpperCase(), columnName.toUpperCase())) {
                    exists = rs.next();
                }
            }
            if (exists) return;

            String sql = "ALTER TABLE " + Names.t(baseTable) + " ADD COLUMN `" + columnName + "` " + columnDefinition;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
                LOGGER.info("[Pointed] " + phys + " にカラム '" + columnName + "' を追加");
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] addColumn 失敗 (" + phys + "." + columnName + "): " + e.getMessage());
        }
    }

    public static void ensurePrimaryKey(Connection conn, String baseTable, String[] columns) {
        String phys = Names.phys(baseTable);
        try {
            Set<String> existing = new LinkedHashSet<String>();
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getPrimaryKeys(null, null, phys)) {
                while (rs.next()) existing.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            Set<String> target = new LinkedHashSet<String>();
            for (String c : columns) target.add(c.toLowerCase());
            if (existing.equals(target)) return;
            if (!existing.isEmpty()) {
                LOGGER.warning("[Pointed] " + phys + " に既存PrimaryKeyがあり変更しない");
                return;
            }
            String cols = joinBackticked(columns);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("ALTER TABLE " + Names.t(baseTable) + " ADD PRIMARY KEY (" + cols + ")");
                LOGGER.info("[Pointed] " + phys + " に PRIMARY KEY 付与: " + Arrays.toString(columns));
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] ensurePrimaryKey 失敗 (" + phys + "): " + e.getMessage());
        }
    }

    private static void ensureUniqueIndex(Connection conn, String baseTable, String indexName, String[] columns) {
        String phys = Names.phys(baseTable);
        try {
            if (indexExists(conn, phys, indexName)) return;
            String cols = joinBackticked(columns);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE UNIQUE INDEX `" + indexName + "` ON " + Names.t(baseTable) + "(" + cols + ")");
                LOGGER.info("[Pointed] " + phys + " に UNIQUE INDEX " + indexName + " 作成");
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] ensureUniqueIndex 失敗 (" + phys + "/" + indexName + "): " + e.getMessage());
        }
    }

    public static void ensureIndex(Connection conn, String baseTable, String indexName, String[] columns) {
        String phys = Names.phys(baseTable);
        try {
            if (indexExists(conn, phys, indexName)) return;
            String cols = joinBackticked(columns);
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE INDEX `" + indexName + "` ON " + Names.t(baseTable) + "(" + cols + ")");
                LOGGER.info("[Pointed] " + phys + " に INDEX " + indexName + " 作成");
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] ensureIndex 失敗 (" + phys + "/" + indexName + "): " + e.getMessage());
        }
    }

    private static boolean indexExists(Connection conn, String physTable, String indexName) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        try (ResultSet rs = md.getIndexInfo(null, null, physTable, false, false)) {
            while (rs.next()) {
                String existing = rs.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(existing)) return true;
            }
        }
        return false;
    }

    public static void ensureForeignKey(Connection conn, String baseTable, String foreignKeyName,
                                        String col, String refBaseTable, String refCol,
                                        String onDelete, String onUpdate) {
        String phys = Names.phys(baseTable);
        try {
            if (foreignKeyExists(conn, phys, foreignKeyName)) return;
            String sql = "ALTER TABLE " + Names.t(baseTable) + " " +
                    "ADD CONSTRAINT `" + foreignKeyName + "` FOREIGN KEY (`" + col + "`) " +
                    "REFERENCES " + Names.t(refBaseTable) + "(`" + refCol + "`) " +
                    "ON DELETE " + onDelete + " ON UPDATE " + onUpdate;
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
                LOGGER.info("[Pointed] " + phys + " に ForeignKey " + foreignKeyName + " 付与");
            }
        } catch (SQLException e) {
            LOGGER.warning("[Pointed] ensureForeignKey 失敗 (" + phys + "/" + foreignKeyName + "): " + e.getMessage());
        }
    }

    private static boolean foreignKeyExists(Connection conn, String physTable, String foreignKey) throws SQLException {
        String sql = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS " +
                "WHERE CONSTRAINT_SCHEMA = DATABASE() AND CONSTRAINT_NAME = ? AND TABLE_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, foreignKey);
            ps.setString(2, physTable);
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