package dev.felnull.pointed.gui.page;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.data.SubjectPointData;
import dev.felnull.pointed.database.dataio.RewardDao;
import dev.felnull.pointed.database.dataio.SubjectPointsDao;
import dev.felnull.pointed.database.dataio.SubjectRepository;
import dev.felnull.pointed.gui.PointedGUIPage;
import dev.felnull.pointed.gui.item.Reward.GetReward;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

public class RewardPage extends PointedGUIPage {
    SubjectPointData subjectPointData;
    int pointTypeId;
    public RewardPage(InventoryGUI gui, int pointTypeId) {
        super(gui, ChatColor.translateAlternateColorCodes('&', "&a報酬受け取りGUI"), 6*9);
        this.pointTypeId = pointTypeId;
    }

    @Override
    public void setUp() {
        try {
            List<RewardData> rewardDataList = RewardDao.listData();
            for(RewardData rewardData : rewardDataList){
                setItem(rewardData.rewardID, new GetReward(gui, rewardData, subjectPointData));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void close() {
        super.close();
        gui.player.closeInventory();
    }

    @Override
    public void back() {
        this.close();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
