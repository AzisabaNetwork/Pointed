package dev.felnull.pointed.gui.item.Reward;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.PointList;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import dev.felnull.pointed.fileio.RewardDataIO;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GetReward extends GUIItem {
    boolean canGetReward = false;
    RewardData rewardData;
    PlayerPointData playerPointData;
    public GetReward(InventoryGUI gui, RewardData rewardData, PlayerPointData playerPointData) {
        super(gui, new ItemStack(Material.ENDER_CHEST));
        this.rewardData = rewardData;
        setDisplayName(rewardData.displayName);
        ItemMeta meta = itemStack.getItemMeta();

        this.playerPointData = playerPointData;
        int nowPoint = this.playerPointData.getPoint(PointList.EVENT_POINT.getName());

        if(nowPoint >= rewardData.needPoint * (this.playerPointData.getnumberofGetReward(rewardData) + 1) && nowPoint >= rewardData.needMinPoint){

            if(rewardData.repeatable){
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                canGetReward = true;

            }else {
               if(this.playerPointData.getnumberofGetReward(rewardData) == 0){
                   meta.addEnchant(Enchantment.DURABILITY, 1, true);
                   canGetReward = true;

               }else {
                   canGetReward = false;

               }
            }
        }else {
            canGetReward = false;
        }

        itemStack.setItemMeta(meta);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        Inventory playerInventory = gui.player.getInventory();
        List<ItemStack> itemStackList = rewardData.getRewardList();

        int emptySlots = 0;
        // インベントリ内の各スロットを確認
        for (ItemStack item : playerInventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++; // 空スロットをカウント
            }
        }

        if(!(emptySlots >= itemStackList.size())){
            gui.player.sendMessage("インベントリの空きが" + (itemStackList.size() - emptySlots)  + "マス足りません");
            return;
        }

        if(canGetReward){
            playerPointData.addnumberofGetReward(rewardData);
            playerPointData.subtractPoint(PointList.EVENT_POINT.getName(), rewardData.needPoint);
            PlayerPointDataIO.savePlayerPointData(playerPointData);
            for(ItemStack item : itemStackList){
                playerInventory.addItem(item);
            }
            gui.player.sendMessage(ChatColor.translateAlternateColorCodes('&', rewardData.displayName + "&6の報酬を獲得!"));
            gui.player.playSound(gui.player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f,1.0f);
        }
    }

}
