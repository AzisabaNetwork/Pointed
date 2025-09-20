package dev.felnull.pointed.teams.database;

import dev.felnull.pointed.core.database.Db;
import dev.felnull.pointed.core.database.Names;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static dev.felnull.pointed.core.database.TableInitializer.*;

public class TeamTableInitializer {
    private static final Logger LOGGER = Logger.getLogger("Pointed");
    public static void initTables() {
        try (Connection conn = Db.get().getConnection();
             Statement stmt = conn.createStatement()) {

            // ========== 1) 骨格だけCREATE（最小列） ==========
            // team_meta（骨格）
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("team_meta") + " (" +
                    " subject_id BIGINT NOT NULL," +
                    " PRIMARY KEY (subject_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // ========== 2) 不足カラムを追加 ==========
            // team_meta
            addColumnIfNotExists(conn, "team_meta", "color_code", "VARCHAR(16) NOT NULL");
            addColumnIfNotExists(conn, "team_meta", "sort_order", "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "team_meta", "active",     "TINYINT(1) NOT NULL DEFAULT 1");

            // ========== 3) インデックス・制約 ==========

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
}
