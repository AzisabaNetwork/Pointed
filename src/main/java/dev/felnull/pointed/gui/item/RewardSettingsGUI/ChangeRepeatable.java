package dev.felnull.pointed.gui.item.RewardSettingsGUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.util.ChatContentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ChangeRepeatable extends GUIItem {
    RewardData rewardData;

    public ChangeRepeatable(InventoryGUI gui, RewardData rewardData) {
        super(gui, new ItemStack(Material.ANVIL));
        setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6繰り返し受け取りが可能か"));
        ItemMeta meta = itemStack.getItemMeta();
        if(rewardData.repeatable){
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
        }else {
            meta.removeEnchant(Enchantment.DURABILITY);
        }
        itemStack.setItemMeta(meta);

        this.rewardData = rewardData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        rewardData.repeatable = !rewardData.repeatable;
        gui.currentPage.setUp();
    }
}
