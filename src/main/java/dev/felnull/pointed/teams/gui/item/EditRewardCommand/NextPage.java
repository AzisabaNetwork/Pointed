package dev.felnull.pointed.teams.gui.item.EditRewardCommand;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.EditRewardCommand;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class NextPage extends GUIItem {

    EditRewardCommand page;

    public NextPage(InventoryGUI gui, EditRewardCommand page) {
        super(gui, new ItemStack(Material.FEATHER));
        setDisplayName(Util.f("次のページへ"));
        this.page = page;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        int nowPage = 0;
        if(!(page.invStartPosition <= 0)){
            nowPage = page.invStartPosition / 45;
        }
        page.changeSlotStartPosition(nowPage + 1);
    }
}
