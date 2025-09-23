package dev.felnull.pointed.teams.gui.item.EditRewards;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.EditRewardCommand;
import dev.felnull.pointed.teams.gui.page.EditRewards;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ReturnPage extends GUIItem {

    EditRewards page;

    public ReturnPage(InventoryGUI gui, EditRewards page) {
        super(gui, new ItemStack(Material.FEATHER));
        setDisplayName(Util.f("前のページへ"));
        this.page = page;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if(!(page.offset < 45)) {
            page.setPage(page.offset / page.limit);
        }
    }
}
