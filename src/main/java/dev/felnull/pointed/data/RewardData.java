package dev.felnull.pointed.data;

import dev.felnull.pointed.database.dataio.PointTypeDao;
import dev.felnull.pointed.database.dataio.RewardDao;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RewardData implements Comparable<RewardData> {

    /** リワードID（主キー） */
    public Integer rewardID;

    /** 表示名（GUIやメッセージ用） */
    public String displayName;

    /** ポイントタイプ名（外部から指定される文字列） */
    public String pointTypeName;

    /** 必要な保有ポイント */
    public Integer needPoint;

    /** 累計ポイントの最低条件 */
    public Integer needMinPoint;

    /** 繰り返し取得可能かどうか */
    public boolean repeatable;

    /** 有効フラグ（無効化すると受け取れなくなる） */
    public boolean active = true;

    /** 付与されるアイテムリスト */
    @Getter
    private List<ItemStack> rewardList = new ArrayList<>();

    // ------------------ コンストラクタ ------------------

    public RewardData(Integer rewardID, String displayName, String pointTypeName,
                      Integer needPoint, Integer needMinPoint, boolean repeatable, boolean active) {
        this.rewardID = rewardID;
        this.displayName = displayName;
        this.pointTypeName = pointTypeName;
        this.needPoint = needPoint;
        this.needMinPoint = needMinPoint;
        this.repeatable = repeatable;
        this.active = active;
    }

    public RewardData(Integer rewardID, String displayName, String pointTypeName,
                      Integer needPoint, Integer needMinPoint, boolean repeatable,
                      boolean active, List<ItemStack> rewardList) {
        this.rewardID = rewardID;
        this.displayName = displayName;
        this.pointTypeName = pointTypeName;
        this.needPoint = needPoint;
        this.needMinPoint = needMinPoint;
        this.repeatable = repeatable;
        this.active = active;
        this.rewardList = rewardList;
    }

    // ------------------ 操作メソッド ------------------

    /** アイテムを追加 */
    public void addReward(ItemStack reward) {
        rewardList.add(reward);
    }

    /** 指定スロットのアイテムを削除 */
    public void removeReward(int index) {
        rewardList.remove(index);
    }

    /** 報酬リストを置き換え */
    public void setReward(List<ItemStack> itemStacks) {
        rewardList = itemStacks;
    }

    @Override
    public int compareTo(@NotNull RewardData o) {
        return Integer.compare(this.rewardID, o.rewardID);
    }

    // ------------------ DAO連携用 ------------------

    /** DAO用の RewardRow へ変換 */
    public RewardDao.RewardRow toRow() throws SQLException {
        RewardDao.RewardRow row = new RewardDao.RewardRow();
        row.id = this.rewardID;
        row.displayName = this.displayName;
        row.pointTypeId = PointTypeDao.ensurePointType(this.pointTypeName); // 名前からIDを解決
        row.needPoint = this.needPoint != null ? this.needPoint : (Integer) 0;
        row.needMinTotal = this.needMinPoint != null ? this.needMinPoint : (Integer) 0;
        row.repeatable = this.repeatable;
        row.active = this.active;
        return row;
    }

    /** DAOから取得した RewardRow を GUI 用の RewardData に変換 */
    public static RewardData fromRow(RewardDao.RewardRow row, String pointTypeName, List<ItemStack> items) {
        return new RewardData(
                row.id,
                row.displayName,
                pointTypeName,
                row.needPoint,
                row.needMinTotal,
                row.repeatable,
                row.active,
                items
        );
    }
}