package dev.felnull.pointed.teams.gui.item.EditRewardCommand;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.EditRewardCommand;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BackPage extends GUIItem {

    EditRewardCommand page;

    public BackPage(InventoryGUI gui, EditRewardCommand page) {
        super(gui, new ItemStack(Material.FEATHER));
        setDisplayName(Util.f("前のページへ"));
        this.page = page;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        page.back();
    }
}
