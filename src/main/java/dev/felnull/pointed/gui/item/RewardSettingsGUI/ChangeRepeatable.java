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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChangeRepeatable extends GUIItem {
    RewardData rewardData;

    public ChangeRepeatable(InventoryGUI gui, RewardData rewardData) {
        super(gui, new ItemStack(Material.ANVIL));
        setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6繰り返し受け取りが可能か"));
        if(rewardData.displayName != null){
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("[現在設定されている値]:").color(NamedTextColor.GRAY).append(Component.text(rewardData.repeatable)));
            setLore(lore);
        }
        this.rewardData = rewardData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        rewardData.repeatable = !rewardData.repeatable;
        gui.currentPage.setUp();
    }
}
