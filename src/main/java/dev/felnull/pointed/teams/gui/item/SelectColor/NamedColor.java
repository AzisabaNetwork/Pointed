package dev.felnull.pointed.teams.gui.item.SelectColor;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.teams.gui.page.TeamConfigGUI;
import dev.felnull.pointed.teams.manager.reward.data.TeamData;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class NamedColor extends GUIItem {
    TeamConfigGUI page;
    TeamData teamData;
    NamedTextColor namedTextColor;
    public NamedColor(InventoryGUI gui, TeamConfigGUI page, ItemStack itemStack, TeamData teamData, NamedTextColor namedTextColor) {
        super(gui, itemStack);
        setDisplayName(namedTextColor.toString() + "を選ぶ!");
        this.teamData = teamData;
        this.page = page;
        this.namedTextColor = namedTextColor;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        this.teamData.setColor(namedTextColor);
        this.page.reopen();
    }
}
