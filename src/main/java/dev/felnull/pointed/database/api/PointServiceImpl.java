package dev.felnull.pointed.database.api;

import dev.felnull.pointed.data.RankRow;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PointServiceImpl implements PointService {
    private final DataSource ds;
    private final ZoneId zoneId;

    public PointServiceImpl(DataSource ds, ZoneId zoneId) {
        this.ds = ds;
        this.zoneId = zoneId;
    }

    // ===== ユーティリティ =====
    private long getAccountId(Connection con, String subjectType, String subjectKey, String scope) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "SELECT a.id FROM accounts a " +
                        "JOIN subjects s ON s.id=a.subject_id " +
                        "WHERE s.type=? AND s.subject_key=? AND a.scope=?");
        ps.setString(1, subjectType);
        ps.setString(2, subjectKey);
        ps.setString(3, scope);
        ResultSet rs = ps.executeQuery();
        try {
            if (!rs.next()) throw new IllegalStateException("Account not ensured");
            return rs.getLong(1);
        } finally {
            rs.close();
            ps.close();
        }
    }

    private static Date toSqlDate(LocalDate d) {
        return Date.valueOf(d);
    }

    private Date todayLocal() {
        return toSqlDate(LocalDate.now(zoneId));
    }

    @Override
    public void ensureAccount(String subjectType, String subjectKey, String scope, String name) {
        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);
            try {
                // subjects
                PreparedStatement ps1 = con.prepareStatement(
                        "INSERT INTO subjects (type, subject_key, name) VALUES(?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE name=VALUES(name)");
                ps1.setString(1, subjectType);
                ps1.setString(2, subjectKey);
                ps1.setString(3, name);
                ps1.executeUpdate();
                ps1.close();

                // accounts
                PreparedStatement ps2 = con.prepareStatement(
                        "INSERT INTO accounts (subject_id, scope) " +
                                "SELECT id, ? FROM subjects WHERE type=? AND subject_key=? " +
                                "ON DUPLICATE KEY UPDATE scope=scope");
                ps2.setString(1, scope);
                ps2.setString(2, subjectType);
                ps2.setString(3, subjectKey);
                ps2.executeUpdate();
                ps2.close();

                // account_balances
                PreparedStatement ps3 = con.prepareStatement(
                        "INSERT IGNORE INTO account_balances (account_id, now_point, total_point) " +
                                "SELECT a.id, 0, 0 FROM accounts a " +
                                "JOIN subjects s ON s.id=a.subject_id " +
                                "WHERE s.type=? AND s.subject_key=? AND a.scope=?");
                ps3.setString(1, subjectType);
                ps3.setString(2, subjectKey);
                ps3.setString(3, scope);
                ps3.executeUpdate();
                ps3.close();

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getNowPoint(String subjectType, String subjectKey, String scope) {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ab.now_point FROM account_balances ab " +
                             "JOIN accounts a ON a.id=ab.account_id " +
                             "JOIN subjects s ON s.id=a.subject_id " +
                             "WHERE s.type=? AND s.subject_key=? AND a.scope=?")) {
            ps.setString(1, subjectType);
            ps.setString(2, subjectKey);
            ps.setString(3, scope);
            ResultSet rs = ps.executeQuery();
            try {
                return rs.next() ? rs.getLong(1) : 0L;
            } finally {
                rs.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getTotalPoint(String subjectType, String subjectKey, String scope) {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ab.total_point FROM account_balances ab " +
                             "JOIN accounts a ON a.id=ab.account_id " +
                             "JOIN subjects s ON s.id=a.subject_id " +
                             "WHERE s.type=? AND s.subject_key=? AND a.scope=?")) {
            ps.setString(1, subjectType);
            ps.setString(2, subjectKey);
            ps.setString(3, scope);
            ResultSet rs = ps.executeQuery();
            try {
                return rs.next() ? rs.getLong(1) : 0L;
            } finally {
                rs.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long[] getNowAndTotal(String subjectType, String subjectKey, String scope) {
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT ab.now_point, ab.total_point FROM account_balances ab " +
                             "JOIN accounts a ON a.id=ab.account_id " +
                             "JOIN subjects s ON s.id=a.subject_id " +
                             "WHERE s.type=? AND s.subject_key=? AND a.scope=?")) {
            ps.setString(1, subjectType);
            ps.setString(2, subjectKey);
            ps.setString(3, scope);
            ResultSet rs = ps.executeQuery();
            try {
                if (rs.next()) return new long[]{ rs.getLong(1), rs.getLong(2) };
                return new long[]{ 0L, 0L };
            } finally {
                rs.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long[] add(String subjectType, String subjectKey, String scope, long amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);
            try {
                long accountId = getAccountId(con, subjectType, subjectKey, scope);

                PreparedStatement ps = con.prepareStatement(
                        "UPDATE account_balances " +
                                "SET now_point = now_point + ?, total_point = total_point + ? " +
                                "WHERE account_id = ?");
                ps.setLong(1, amount);
                ps.setLong(2, amount);
                ps.setLong(3, accountId);
                if (ps.executeUpdate() != 1) {
                    ps.close();
                    throw new IllegalStateException("Account missing");
                }
                ps.close();

                PreparedStatement psDaily = con.prepareStatement(
                        "INSERT INTO account_daily (account_id, day, gained) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE gained = gained + VALUES(gained)");
                psDaily.setLong(1, accountId);
                psDaily.setDate(2, todayLocal());
                psDaily.setLong(3, amount);
                psDaily.executeUpdate();
                psDaily.close();

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return getNowAndTotal(subjectType, subjectKey, scope);
    }

    @Override
    public boolean subtract(String subjectType, String subjectKey, String scope, long amount) {
        if (amount <= 0) return true;
        try (Connection con = ds.getConnection()) {
            long accountId = getAccountId(con, subjectType, subjectKey, scope);
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE account_balances SET now_point = now_point - ? " +
                            "WHERE account_id = ? AND now_point >= ?");
            ps.setLong(1, amount);
            ps.setLong(2, accountId);
            ps.setLong(3, amount);
            int updated = ps.executeUpdate();
            ps.close();
            return updated == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long[] set(String subjectType, String subjectKey, String scope, long newNow) {
        if (newNow < 0) throw new IllegalArgumentException("newNow must be >= 0");
        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);
            try {
                long accountId = getAccountId(con, subjectType, subjectKey, scope);

                // old now
                PreparedStatement psNow = con.prepareStatement(
                        "SELECT now_point FROM account_balances WHERE account_id=?");
                psNow.setLong(1, accountId);
                ResultSet rs = psNow.executeQuery();
                long oldNow = rs.next() ? rs.getLong(1) : 0L;
                rs.close();
                psNow.close();

                PreparedStatement ps = con.prepareStatement(
                        "UPDATE account_balances " +
                                "SET total_point = total_point + GREATEST(? - now_point, 0), " +
                                "    now_point   = ? " +
                                "WHERE account_id = ?");
                ps.setLong(1, newNow);
                ps.setLong(2, newNow);
                ps.setLong(3, accountId);
                if (ps.executeUpdate() != 1) {
                    ps.close();
                    throw new IllegalStateException("Account missing");
                }
                ps.close();

                long inc = newNow - oldNow;
                if (inc > 0) {
                    PreparedStatement psDaily = con.prepareStatement(
                            "INSERT INTO account_daily (account_id, day, gained) VALUES (?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE gained = gained + VALUES(gained)");
                    psDaily.setLong(1, accountId);
                    psDaily.setDate(2, todayLocal());
                    psDaily.setLong(3, inc);
                    psDaily.executeUpdate();
                    psDaily.close();
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return getNowAndTotal(subjectType, subjectKey, scope);
    }

    @Override
    public List<RankRow> getDailyTop(String subjectType, String scope, LocalDate day, int limit) {
        List<RankRow> list = new ArrayList<>();
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT s.type, s.subject_key, s.name, d.gained, ab.now_point, ab.total_point " +
                             "FROM account_daily d " +
                             "JOIN account_balances ab ON ab.account_id = d.account_id " +
                             "JOIN accounts a ON a.id = d.account_id " +
                             "JOIN subjects s ON s.id = a.subject_id " +
                             "WHERE d.day = ? AND a.scope = ? AND s.type = ? " +
                             "ORDER BY d.gained DESC LIMIT ?")) {
            ps.setDate(1, toSqlDate(day));
            ps.setString(2, scope);
            ps.setString(3, subjectType);
            ps.setInt(4, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RankRow(
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getLong(4), rs.getLong(5), rs.getLong(6)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<RankRow> getWeeklyTop(String subjectType, String scope, LocalDate startInclusive, LocalDate endInclusive, int limit) {
        List<RankRow> list = new ArrayList<>();
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT s.type, s.subject_key, s.name, SUM(d.gained) AS week_gained, ab.now_point, ab.total_point " +
                             "FROM account_daily d " +
                             "JOIN account_balances ab ON ab.account_id = d.account_id " +
                             "JOIN accounts a ON a.id = d.account_id " +
                             "JOIN subjects s ON s.id = a.subject_id " +
                             "WHERE d.day BETWEEN ? AND ? AND a.scope = ? AND s.type = ? " +
                             "GROUP BY s.type, s.subject_key, s.name, ab.now_point, ab.total_point " +
                             "ORDER BY week_gained DESC LIMIT ?")) {
            ps.setDate(1, toSqlDate(startInclusive));
            ps.setDate(2, toSqlDate(endInclusive));
            ps.setString(3, scope);
            ps.setString(4, subjectType);
            ps.setInt(5, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RankRow(
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getLong(4), rs.getLong(5), rs.getLong(6)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<RankRow> getGlobalTop(String subjectType, String scope, int limit) {
        List<RankRow> list = new ArrayList<>();
        try (Connection con = ds.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT s.type, s.subject_key, s.name, ab.total_point, ab.now_point, ab.total_point " +
                             "FROM account_balances ab " +
                             "JOIN accounts a ON a.id = ab.account_id " +
                             "JOIN subjects s ON s.id = a.subject_id " +
                             "WHERE a.scope = ? AND s.type = ? " +
                             "ORDER BY ab.total_point DESC LIMIT ?")) {
            ps.setString(1, scope);
            ps.setString(2, subjectType);
            ps.setInt(3, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RankRow(
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getLong(4), rs.getLong(5), rs.getLong(6) // gained = total
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}