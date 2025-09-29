package dev.felnull.pointed.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class Db {
    private static HikariDataSource DS;

    public static void init(String host, int port, String db, String user, String pass) {
        String jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + db
                + "?useUnicode=true&characterEncoding=utf8mb4";

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setDriverClassName("dev.felnull.pointed.shaded.mariadb.jdbc.Driver");

        // プール設定
        cfg.setMaximumPoolSize(30);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(15000);
        cfg.setIdleTimeout(600000);
        cfg.setMaxLifetime(1800000);

        // タイムゾーン指定（例: "+09:00" または "Asia/Tokyo"）
        cfg.setConnectionInitSql("SET time_zone = '+09:00'");

        // パフォーマンス系の推奨設定
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        cfg.addDataSourceProperty("useServerPrepStmts", "true");
        cfg.addDataSourceProperty("useLocalSessionState", "true");
        cfg.addDataSourceProperty("rewriteBatchedStatements", "true");
        cfg.addDataSourceProperty("cacheResultSetMetadata", "true");
        cfg.addDataSourceProperty("cacheServerConfiguration", "true");
        cfg.addDataSourceProperty("elideSetAutoCommits", "true");
        cfg.addDataSourceProperty("maintainTimeStats", "false");

        DS = new HikariDataSource(cfg);
    }

    public static DataSource get() {
        if (DS == null) throw new IllegalStateException("Db not initialized");
        return DS;
    }

    public static void close() {
        if (DS != null) DS.close();
    }
}