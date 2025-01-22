package dev.felnull.pointed.gui.page;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.bettergui.listener.GUIClickListener;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.gui.PointedGUIPage;
import dev.felnull.pointed.listener.SetRewardItemPageListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SetRewardItemPage extends PointedGUIPage {

    GUIClickListener listener;
    RewardData rewardData;
    public SetRewardItemPage(InventoryGUI gui, RewardData rewardData) {
        super(gui, ChatColor.translateAlternateColorCodes('&', "&6報酬アイテム設定GUI アイテムを入れてね"), 6*9);
        this.rewardData = rewardData;
        HandlerList.unregisterAll(super.listener);
        this.listener = new SetRewardItemPageListener(this);//このページ専用リスナー起動
        Bukkit.getPluginManager().registerEvents(this.listener, Pointed.instance);
    }

    @Override
    public void setUp() {
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
        HandlerList.unregisterAll(this.listener);
        List<ItemStack> itemList = new ArrayList<>();
        for(ItemStack item : this.inventory.getContents()){
            if (item != null && item.getType() != Material.AIR) { // アイテムがnullまたは空でない場合
                itemList.add(item);
            }
        }
        new BukkitRunnable(){
            @Override
            public void run(){
                gui.openPage(new RewardSettingsGUI(gui, rewardData, itemList));
            }
        }.runTaskLater(Pointed.instance, 2L);


    }

    @Override
    public void back() {

    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
