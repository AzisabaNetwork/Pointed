package dev.felnull.pointed.gui.item.RewardSettingsGUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.fileio.RewardDataIO;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class Complete extends GUIItem {
    RewardData rewardData;
    public Complete(InventoryGUI gui, RewardData rewardData) {
        super(gui, new ItemStack(Material.GREEN_STAINED_GLASS));
        setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6完成!"));
        this.rewardData = rewardData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        Player p = gui.player;
        if(rewardData.rewardID == null){
            p.sendMessage("RewardIDを設定してください");
            return;
        }else if(rewardData.needPoint == null){
            p.sendMessage("必要ポイント数を設定してください");
            return;
        }else if(rewardData.displayName == null){
            p.sendMessage("DisplayNameを設定してください");
            return;
        }


        if(RewardDataIO.saveReward(rewardData)){
            p.sendMessage("リワードデータを正常に保存しました");
            gui.currentPage.close();
        }else {
            p.sendMessage("リワードデータが正常に保存されませんでしたもう一度行うまたは異常がないかを確認してください");
        }

    }
}
