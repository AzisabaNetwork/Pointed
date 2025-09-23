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


            //rewards
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("reward_commands") + " (" +
                    " reward_id INT NOT NULL," +
                    " idx INT NOT NULL," +                        // 実行順
                    " PRIMARY KEY (reward_id, idx)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + Names.t("reward_dispatch_log") + " (" +
                    " id BIGINT AUTO_INCREMENT PRIMARY KEY" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");


            // ========== 2) 不足カラムを追加 ==========
            // team_meta
            addColumnIfNotExists(conn, "team_meta", "color_code", "VARCHAR(16) NOT NULL");
            addColumnIfNotExists(conn, "team_meta", "sort_order", "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(conn, "team_meta", "active",     "TINYINT(1) NOT NULL DEFAULT 1");

            // reward_commands
            addColumnIfNotExists(conn, "reward_commands", "command_text", "VARCHAR(500) NOT NULL");
            addColumnIfNotExists(conn, "reward_commands", "enabled",      "TINYINT(1) NOT NULL DEFAULT 1");

            // reward_dispatch_log（二重配布防止のキーに使う）
            addColumnIfNotExists(conn, "reward_dispatch_log", "team_subject_id", "BIGINT NULL");
            addColumnIfNotExists(conn, "reward_dispatch_log", "scope",             "VARCHAR(64) NOT NULL");
            addColumnIfNotExists(conn, "reward_dispatch_log", "from_date",         "DATE NOT NULL");   // [from, to) の from
            addColumnIfNotExists(conn, "reward_dispatch_log", "to_date",           "DATE NOT NULL");   // [from, to) の to
            addColumnIfNotExists(conn, "reward_dispatch_log", "player_subject_id", "BIGINT NOT NULL");
            addColumnIfNotExists(conn, "reward_dispatch_log", "rank_no",           "INT NOT NULL");
            addColumnIfNotExists(conn, "reward_dispatch_log", "points",            "BIGINT NOT NULL");
            addColumnIfNotExists(conn, "reward_dispatch_log", "reward_id",         "INT NULL");
            addColumnIfNotExists(conn, "reward_dispatch_log", "executed_commands", "TEXT NOT NULL");
            addColumnIfNotExists(conn, "reward_dispatch_log", "executed_at",       "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");

            // team_members
            addColumnIfNotExists(conn, "team_members", "joined_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            addColumnIfNotExists(conn, "team_members", "left_at",   "TIMESTAMP NULL DEFAULT NULL"); // 退会対応（任意）

            addColumnIfNotExists(conn, "rewards", "created_at",
                    "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            addColumnIfNotExists(conn, "rewards", "updated_at",
                    "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

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

            ensureForeignKey(conn, "reward_commands", "fk_rc_reward",
                    "reward_id", "rewards", "id", "CASCADE", "CASCADE");
            ensureIndex(conn, "reward_commands", "idx_rc_enabled", new String[]{"enabled"});

            ensureIndex(conn, "reward_commands", "idx_rc_reward_enabled_idx",
                    new String[]{"reward_id","enabled","idx"});

            ensureForeignKey(conn, "reward_dispatch_log", "fk_rdl_reward",
                    "reward_id", "rewards", "id", "SET NULL", "CASCADE");
            ensureIndex(conn, "reward_dispatch_log", "idx_rdl_team_scope", new String[]{"team_subject_id","scope"});
            ensureUniqueIndex(conn, "reward_dispatch_log", "uniq_rdl_once",
                    new String[]{"team_subject_id","scope","from_date","to_date","player_subject_id"});
            ensureIndex(conn, "reward_dispatch_log", "idx_rdl_executed_at", new String[]{"executed_at"});
            ensureIndex(conn, "reward_dispatch_log", "idx_rdl_player",      new String[]{"player_subject_id"});

            ensureForeignKey(conn, "team_members", "fk_tm_team_subject",
                    "team_subject_id", "subjects", "id", "CASCADE", "CASCADE");
            ensureForeignKey(conn, "team_members", "fk_tm_player_subject",
                    "player_subject_id", "subjects", "id", "CASCADE", "CASCADE");
            ensureIndex(conn, "team_members", "idx_tm_by_team",   new String[]{"team_subject_id"});
            ensureIndex(conn, "team_members", "idx_tm_by_player", new String[]{"player_subject_id"});

            // ランキングで効く索引（既存に加えて）
            ensureIndex(conn, "accounts", "idx_accounts_scope_subject", new String[]{"scope","subject_id"});
            ensureIndex(conn, "account_daily", "idx_ad_acc_day", new String[]{"account_id","day"});

            ensureIndex(conn, "rewards", "idx_rewards_updated_at", new String[]{"updated_at"});


            LOGGER.info("[Pointed] テーブル初期化完了！");

        } catch (SQLException e) {
            LOGGER.warning("[Pointed] テーブル初期化中にエラー: " + e.getMessage());
        }
    }
}
