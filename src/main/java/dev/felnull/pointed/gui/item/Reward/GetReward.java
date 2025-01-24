package dev.felnull.pointed.gui.item.Reward;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.PointList;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import dev.felnull.pointed.fileio.RewardDataIO;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GetReward extends GUIItem {
    boolean canGetReward = false;
    RewardData rewardData;
    PlayerPointData playerPointData;
    int needPoint;
    public GetReward(InventoryGUI gui, RewardData rewardData, PlayerPointData playerPointData) {
        super(gui, new ItemStack(Material.COAL_BLOCK));
        this.rewardData = rewardData;
        setDisplayName(rewardData.displayName);
        this.playerPointData = playerPointData;
        int nowPoint = this.playerPointData.getPoint(PointList.EVENT_POINT.getName());
        int totalPoint = this.playerPointData.getTotalPoint(PointList.EVENT_POINT.getName());
        this.needPoint = rewardData.needPoint * (this.playerPointData.getnumberofGetReward(rewardData) + 1);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', "&6現在保有しているポイント数&f: " + nowPoint)));
        if(rewardData.repeatable || playerPointData.getnumberofGetReward(rewardData) == 0 ){
            lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', "&6消費ポイント数&f: " + needPoint)));
        }else {
            lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', "&6この報酬は受け取り済みです")));
        }

        if(rewardData.needMinPoint != null && rewardData.needMinPoint != 0){
            lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', "&6受け取りに必要な累計ポイント数&f: " + rewardData.needMinPoint)));
        }
        if(rewardData.repeatable){
            lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', "&6&l繰り返し受け取り可能!")));
        }
        setLore(lore);
        ItemMeta meta = itemStack.getItemMeta();

        if(nowPoint >= needPoint && totalPoint >= rewardData.needMinPoint){
            if(rewardData.repeatable){
                this.itemStack = new ItemStack(Material.ENDER_CHEST);
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                canGetReward = true;
            }else {
               if(this.playerPointData.getnumberofGetReward(rewardData) == 0){
                   this.itemStack = new ItemStack(Material.ENDER_CHEST);
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
            playerPointData.subtractPoint(PointList.EVENT_POINT.getName(), needPoint);
            PlayerPointDataIO.savePlayerPointData(playerPointData);
            gui.player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f----------------------"));
            gui.player.sendMessage(ChatColor.translateAlternateColorCodes('&', rewardData.displayName + "&6 の報酬を獲得!"));
            gui.player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f----------------------"));
            for(ItemStack item : itemStackList){
                playerInventory.addItem(item);
                ItemMeta meta = item.getItemMeta();
                gui.player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6" + meta.getDisplayName() + "&f*" + item.getAmount()));
            }
            gui.player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f----------------------"));
            gui.player.playSound(gui.player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f,1.0f);
            gui.currentPage.setUp();
        }
    }

}
