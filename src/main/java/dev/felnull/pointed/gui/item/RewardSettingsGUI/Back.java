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

public class Back extends GUIItem {
    public Back(InventoryGUI gui, RewardData rewardData) {
        super(gui, new ItemStack(Material.GREEN_STAINED_GLASS));
        setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6キャンセル"));
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        gui.currentPage.back();
    }
}
