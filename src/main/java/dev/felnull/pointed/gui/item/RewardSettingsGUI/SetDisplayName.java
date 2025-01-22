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

public class SetDisplayName extends GUIItem {
    RewardData rewardData;

    public SetDisplayName(InventoryGUI gui, RewardData rewardData) {
        super(gui, new ItemStack(Material.NAME_TAG));
        setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6DisplayNameを設定する"));
        if(rewardData.displayName != null){
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("[現在設定されているDisplayName]:").color(NamedTextColor.GRAY).append(Component.text(rewardData.displayName)));
            setLore(lore);
        }
        this.rewardData = rewardData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        gui.player.sendMessage("表示したい名前を入力してください");
        Pointed.instance.getChatReader().registerNextChat(gui.player, ChatContentType.DISPLAY_NAME, rewardData);
    }
}
