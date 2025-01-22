package dev.felnull.pointed.gui.page;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import dev.felnull.pointed.fileio.RewardDataIO;
import dev.felnull.pointed.gui.PointedGUIPage;
import dev.felnull.pointed.gui.item.Reward.GetReward;
import dev.felnull.pointed.gui.item.RewardSettingsGUI.*;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RewardPage extends PointedGUIPage {
    PlayerPointData playerPointData;
    public RewardPage(InventoryGUI gui) {
        super(gui, ChatColor.translateAlternateColorCodes('&', "&a[Pointed]&6&lRewardSettings"), 6*9);
    }

    @Override
    public void setUp() {
        this.playerPointData = PlayerPointDataIO.loadPlayerPointData(gui.player);
        List<RewardData> rewardDataList = RewardDataIO.loadRewards();
        for(RewardData rewardData : rewardDataList){
            setItem(rewardData.rewardID, new GetReward(gui, rewardData, playerPointData));
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
