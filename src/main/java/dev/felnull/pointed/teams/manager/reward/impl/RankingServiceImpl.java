package dev.felnull.pointed.teams.manager.reward.impl;

import dev.felnull.pointed.core.database.Names;
import dev.felnull.pointed.teams.manager.reward.RankingService;
import dev.felnull.pointed.teams.manager.reward.data.PlayerRank;
import dev.felnull.pointed.teams.manager.reward.data.RankRangeType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RankingServiceImpl implements RankingService {
    private final DataSource ds;

    public RankingServiceImpl(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public List<PlayerRank> getTeamRanking(long teamSubjectId, String scope,
                                           RankRangeType rangeType, LocalDate fromDate, LocalDate toDate,
                                           int limit, int offset) throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.id AS player_subject_id, s.name AS player_name, ")
                .append("       COALESCE(SUM(ad.gained),0) AS points ")
                .append("FROM ").append(Names.t("team_members")).append(" tm ")
                .append("JOIN ").append(Names.t("subjects")).append(" s ")
                .append("  ON s.id = tm.player_subject_id AND s.type = 'PLAYER' ")
                .append("JOIN ").append(Names.t("accounts")).append(" a ")
                .append("  ON a.subject_id = s.id AND a.scope = ? ")
                .append("LEFT JOIN ").append(Names.t("account_daily")).append(" ad ")
                .append("  ON ad.account_id = a.id ");

        boolean hasRange = (rangeType != RankRangeType.ALL_TIME);
        sql.append("WHERE tm.team_subject_id = ? ");

        // 在籍期間を厳密に扱う場合は以下を使用（任意）
        // sql.append("AND ad.day >= DATE(tm.joined_at) ")
        //    .append("AND (tm.left_at IS NULL OR ad.day < DATE(tm.left_at)) ");

        if (hasRange) {
            // 半開区間 [from, to)
            sql.append("AND ad.day >= ? AND ad.day < ? ");
        }

        sql.append("GROUP BY s.id, s.name ")
                .append("ORDER BY points DESC, player_subject_id ASC ")
                .append("LIMIT ? OFFSET ?");

        List<PlayerRank> out = new ArrayList<PlayerRank>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            int i = 1;
            ps.setString(i++, scope);
            ps.setLong(i++, teamSubjectId);
            if (hasRange) {
                ps.setDate(i++, java.sql.Date.valueOf(fromDate)); // Asia/Tokyo の日付をそのまま
                ps.setDate(i++, java.sql.Date.valueOf(toDate));
            }
            ps.setInt(i++, limit);
            ps.setInt(i++, offset);

            try (ResultSet rs = ps.executeQuery()) {
                // Java側で RANK() 同点同順位
                long lastPts = Long.MIN_VALUE;
                int lastRank = 0;
                int seen = 0;

                while (rs.next()) {
                    long pid = rs.getLong("player_subject_id");
                    String name = rs.getString("player_name");
                    long pts = rs.getLong("points");
                    seen++;
                    int rankNo = (pts == lastPts) ? lastRank : seen;
                    out.add(new PlayerRank(pid, name, pts, rankNo));
                    lastPts = pts;
                    lastRank = rankNo;
                }
            }
        }
        return out;
    }

    public List<PlayerRank> getTeamRankingAll(long teamSubjectId, String scope,
                                              RankRangeType rangeType, LocalDate fromDate, LocalDate toDate)
            throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.id AS player_subject_id, s.name AS player_name, ")
                .append("       COALESCE(SUM(ad.gained),0) AS points ")
                .append("FROM ").append(Names.t("team_members")).append(" tm ")
                .append("JOIN ").append(Names.t("subjects")).append(" s ")
                .append("  ON s.id = tm.player_subject_id AND s.type = 'PLAYER' ")
                .append("JOIN ").append(Names.t("accounts")).append(" a ")
                .append("  ON a.subject_id = s.id AND a.scope = ? ")
                .append("LEFT JOIN ").append(Names.t("account_daily")).append(" ad ")
                .append("  ON ad.account_id = a.id ")
                .append("WHERE tm.team_subject_id = ? ");
        boolean hasRange = (rangeType != RankRangeType.ALL_TIME);
        if (hasRange) {
            sql.append("AND ad.day >= ? AND ad.day < ? ");
        }
        sql.append("GROUP BY s.id, s.name ")
                .append("ORDER BY points DESC, player_subject_id ASC");

        List<PlayerRank> out = new ArrayList<PlayerRank>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setString(i++, scope);
            ps.setLong(i++, teamSubjectId);
            if (hasRange) {
                ps.setDate(i++, java.sql.Date.valueOf(fromDate));
                ps.setDate(i++, java.sql.Date.valueOf(toDate));
            }
            try (ResultSet rs = ps.executeQuery()) {
                long lastPts = Long.MIN_VALUE;
                int lastRank = 0;
                int seen = 0;
                while (rs.next()) {
                    long pid = rs.getLong("player_subject_id");
                    String name = rs.getString("player_name");
                    long pts = rs.getLong("points");
                    seen++;
                    int rankNo = (pts == lastPts) ? lastRank : seen; // 同点は同順位
                    out.add(new PlayerRank(pid, name, pts, rankNo));
                    lastPts = pts;
                    lastRank = rankNo;
                }
            }
        }
        return out;
    }
}