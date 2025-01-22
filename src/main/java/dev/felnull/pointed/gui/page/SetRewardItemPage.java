package dev.felnull.pointed.gui.page;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.gui.PointedGUIPage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SetRewardItemPage extends PointedGUIPage {

    RewardData rewardData;
    public SetRewardItemPage(InventoryGUI gui, RewardData rewardData) {
        super(gui, ChatColor.translateAlternateColorCodes('&', "&6報酬アイテム設定GUI アイテムを入れてね"), 6*9);
        this.rewardData = rewardData;
    }

    @Override
    public void setUp() {
        HandlerList.unregisterAll(super.listener);

        int index = 0;
        for(ItemStack item : rewardData.getRewardList()){
            setItem(index, item);
            index++;
            if(index == 54){
                break;
            }
        }
    }

    @Override
    public void close() {
        super.close();

        List<ItemStack> itemList = new ArrayList<>();
        for(ItemStack item : this.inventory.getContents()){
            if (item != null && item.getType() != Material.AIR) { // アイテムがnullまたは空でない場合
                itemList.add(item);
            }
        }
        gui.openPage(new RewardSettingsGUI(gui, rewardData, itemList));

    }

    @Override
    public void back() {

    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
