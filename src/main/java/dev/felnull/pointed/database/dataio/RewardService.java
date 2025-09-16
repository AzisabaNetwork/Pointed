package dev.felnull.pointed.database.dataio;

import dev.felnull.pointed.data.SubjectType;
import org.bukkit.inventory.ItemStack;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class RewardService {
    private final DataSource ds;

    public interface RewardDelivery {
        void deliverToSubject(long subjectId, SubjectType type, List<ItemStack> items);
    }

    private final RewardDelivery delivery;

    public RewardService(DataSource ds, RewardDelivery delivery) {
        this.ds = ds;
        this.delivery = delivery;
    }

    /**
     * リワード交換の本体。全処理は単一トランザクション。
     */
    public void claimReward(long subjectId, SubjectType type, int rewardId) throws Exception {
        try (Connection con = ds.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 1) 報酬本体
                RewardDao.RewardRow r = RewardDao.getRewardForUpdate(con, rewardId);
                if (r == null || !r.active) throw new IllegalStateException("Reward not found or inactive");

                // 2) repeatableチェック
                int obtained = RewardDao.getSubjectRewardObtainedForUpdate(con, subjectId, rewardId);
                if (!r.repeatable && obtained >= 1) {
                    throw new IllegalStateException("Already claimed");
                }

                // 3) 前提ANDチェック
                List<RewardDao.PrereqRow> prereqs = RewardDao.getPrereqsForUpdate(con, rewardId, subjectId);
                if (!prereqs.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT reward_id, obtained FROM subject_rewards WHERE subject_id=? AND reward_id=? FOR UPDATE")) {
                        for (RewardDao.PrereqRow p : prereqs) {
                            ps.setLong(1, subjectId);
                            ps.setInt(2, p.prereqRewardId);
                            try (ResultSet rs = ps.executeQuery()) {
                                int have = 0;
                                if (rs.next()) have = rs.getInt(2);
                                if (have < p.minObtained) {
                                    throw new IllegalStateException("Prerequisite not met: " + p.prereqRewardId);
                                }
                            }
                        }
                    }
                }

                // 4) 残高/累計チェック
                int[] ht = RewardDao.getHeldTotalForUpdate(con, subjectId, r.pointTypeId);
                int held = ht[0], total = ht[1];
                if (total < r.needMinTotal) throw new IllegalStateException("Not enough total");
                if (held  < r.needPoint)    throw new IllegalStateException("Not enough held");

                // 5) 消費
                if (r.needPoint > 0) {
                    RewardDao.consumeHeld(con, subjectId, r.pointTypeId, r.needPoint);
                    RewardDao.logLedger(con, subjectId, r.pointTypeId, -r.needPoint, "reward_claim", String.valueOf(rewardId));
                }

                // 6) 受取回数 +1
                RewardDao.incrementSubjectReward(con, subjectId, rewardId);

                // 7) アイテムをロード
                List<ItemStack> items = RewardDao.loadRewardItems(con, rewardId);

                con.commit();

                // 8) アイテム配布（DB外：Bukkitメインスレで）
                // ここは呼び出し側で同期呼び出しにするなど調整してOK
                delivery.deliverToSubject(subjectId, type, items);

            } catch (Throwable t) {
                con.rollback();
                throw t;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }
}