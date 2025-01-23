package dev.felnull.pointed.gui.item.RewardSettingsGUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.gui.page.SetRewardItemPage;
import dev.felnull.pointed.util.ChatContentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class OpenSetRewardItem extends GUIItem {
    public RewardData rewardData;
    public OpenSetRewardItem(InventoryGUI gui, RewardData rewardData) {
        super(gui, new ItemStack(Material.CHEST));
        this.rewardData = rewardData;
        setDisplayName(ChatColor.translateAlternateColorCodes('&',"&6報酬アイテム登録画面を開く"));
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        gui.openPage(new SetRewardItemPage(gui,rewardData));
    }
}
