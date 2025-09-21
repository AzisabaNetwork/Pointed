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

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("team_members") + " (" +
                    " team_subject_id BIGINT NOT NULL," +      // subjects.id（TEAM）
                    " player_subject_id BIGINT NOT NULL" +     // subjects.id（PLAYER）
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // ========== 2) 不足カラムを追加 ==========
            // team_meta
            addColumnIfNotExists(conn, "team_meta", "color_code", "VARCHAR(16) NOT NULL");
            addColumnIfNotExists(conn, "team_meta", "sort_order", "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "team_meta", "active",     "TINYINT(1) NOT NULL DEFAULT 1");

            //team_members
            addColumnIfNotExists(conn, "team_members", "joined_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");

            // ========== 3) インデックス・制約 ==========

            // subjects(id) へのFK。subjects 削除時に team_meta も自動削除
            ensureForeignKey(conn, "team_meta", "fk_tm_subject",
                    "subject_id", "subjects", "id", "CASCADE", "CASCADE");

            // 並び順・有効/無効フィルタの補助INDEX
            ensureIndex(conn, "team_meta", "idx_tm_sort",   new String[]{"sort_order"});
            ensureIndex(conn, "team_meta", "idx_tm_active", new String[]{"active"});

            // 複合PrimaryKey（同じプレイヤーが同じチームに二重登録されない）
            ensurePrimaryKey(conn, "team_members", new String[]{"team_subject_id", "player_subject_id"});
            // 外部キー（親subjectsが消えたら自動削除）
            ensureForeignKey(conn, "team_members", "fk_tm_team_subject",
                    "team_subject_id", "subjects", "id", "CASCADE", "CASCADE");
            ensureForeignKey(conn, "team_members", "fk_tm_player_subject",
                    "player_subject_id", "subjects", "id", "CASCADE", "CASCADE");

            // 参照用インデックス（検索パターン次第でどちらも貼ると便利）
            ensureIndex(conn, "team_members", "idx_tm_by_team",   new String[]{"team_subject_id"});
            ensureIndex(conn, "team_members", "idx_tm_by_player", new String[]{"player_subject_id"});

            LOGGER.info("[Pointed] テーブル初期化完了！");

        } catch (SQLException e) {
            LOGGER.warning("[Pointed] テーブル初期化中にエラー: " + e.getMessage());
        }
    }
}
