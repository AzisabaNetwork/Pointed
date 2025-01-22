package dev.felnull.pointed.gui.page;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.gui.PointedGUIPage;
import dev.felnull.pointed.gui.item.RewardSettingsGUI.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RewardSettingsGUI extends PointedGUIPage {
    RewardData rewardData;
    List<ItemStack> itemList;
    public RewardSettingsGUI(InventoryGUI gui) {
        super(gui, ChatColor.translateAlternateColorCodes('&', "&a[Pointed]&6&lRewardSettings"), 6*9);
        this.rewardData = new RewardData(null, null, null, null, false);
    }
    public RewardSettingsGUI(InventoryGUI gui, RewardData rewardData) {
        super(gui, ChatColor.translateAlternateColorCodes('&', "&a[Pointed]&6&lRewardSettings"), 6*9);
        this.rewardData = rewardData;
    }
    public RewardSettingsGUI(InventoryGUI gui, RewardData rewardData, List<ItemStack> itemList) {
        super(gui, ChatColor.translateAlternateColorCodes('&', "&a[Pointed]&6&lRewardSettings"), 6*9);
        rewardData.setReward(itemList);
        this.rewardData = rewardData;
    }

    @Override
    public void setUp() {
        setItem(11,new SetRewardID(gui,rewardData));
        setItem(12,new SetDisplayName(gui,rewardData));
        setItem(13,new SetNeedPoint(gui,rewardData));
        setItem(14,new SetNeedMinPoint(gui,rewardData));
        setItem(15,new ChangeRepeatable(gui,rewardData));
        setItem(51,new OpenSetRewardItem(gui,rewardData));
        setItem(52,new Back(gui,rewardData));
        setItem(53,new Complete(gui,rewardData));
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
