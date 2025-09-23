package dev.felnull.pointed.teams.manager.reward;

import dev.felnull.pointed.teams.manager.reward.data.PlayerRank;
import dev.felnull.pointed.teams.manager.reward.data.RankRangeType;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface RankingService {
    List<PlayerRank> getTeamRanking(
            long teamSubjectId, String scope,
            RankRangeType rangeType, LocalDate fromDate, LocalDate toDate,
            int limit, int offset
    ) throws SQLException;

    // LIMIT/OFFSET なしで全件返す版（境界の同点を漏らさない）
    List<PlayerRank> getTeamRankingAll(long teamSubjectId, String scope,
                                       RankRangeType rangeType, LocalDate fromDate, LocalDate toDate)
            throws SQLException;
}