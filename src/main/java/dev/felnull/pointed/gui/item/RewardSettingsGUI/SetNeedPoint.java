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

public class SetNeedPoint extends GUIItem {
    RewardData rewardData;

    public SetNeedPoint(InventoryGUI gui, RewardData rewardData) {
        super(gui, new ItemStack(Material.NETHER_STAR));
        setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6受け取りに必要なPoint数を設定する(数字のみ)"));
        if(rewardData.needPoint != null){
            List<Component> lore = new ArrayList<>();
                lore.add(Component.text("[現在設定されている値]:").color(NamedTextColor.GRAY).append(Component.text(rewardData.needPoint)));
            setLore(lore);
        }
        this.rewardData = rewardData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        gui.player.sendMessage("設定したい必要ポイントを入力してください");
        Pointed.instance.getChatReader().registerNextChat(gui.player, ChatContentType.NEED_POINT, rewardData);
    }
}
