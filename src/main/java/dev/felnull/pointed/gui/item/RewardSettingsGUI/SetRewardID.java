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

public class SetRewardID extends GUIItem {
    RewardData rewardData;

    public SetRewardID(InventoryGUI gui, RewardData rewardData) {
        super(gui, new ItemStack(Material.OAK_SIGN));
        setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6RewardIDを設定する(数字のみ)"));
        if(rewardData.rewardID != null){
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("[現在設定されているID]:").color(NamedTextColor.GRAY).append(Component.text(rewardData.rewardID)));
            setLore(lore);
        }
        this.rewardData = rewardData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        gui.player.sendMessage("設定したいRewardIDを0-53の範囲で入力してください");
        Pointed.instance.getChatReader().registerNextChat(gui.player, ChatContentType.REWARD_ID, rewardData);
    }
}
