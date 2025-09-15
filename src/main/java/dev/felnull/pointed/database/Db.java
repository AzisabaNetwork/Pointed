package dev.felnull.pointed.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class Db {
    private static HikariDataSource DS;

    public static void init(String host, int port, String db, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + db
                + "?useUnicode=true&characterEncoding=utf8mb4"
                + "&sessionVariables=time_zone='%2B00:00'");
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setPoolName("PointedPool");
        cfg.setDriverClassName("dev.felnull.pointed.shaded.mariadb.jdbc.Driver");
        // 推奨: MySQL/MariaDB向け
        cfg.addDataSourceProperty("useBulkStmts", "true");
        cfg.addDataSourceProperty("rewriteBatchedStatements", "true");
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