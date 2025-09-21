package dev.felnull.pointed.teams.gui.item.TeamConfigUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.TeamConfigGUI;
import dev.felnull.pointed.teams.manager.TeamData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class SelectColor extends GUIItem {
    TeamData teamData;
    TeamConfigGUI teamConfigGUI;
    public SelectColor(InventoryGUI gui, TeamData teamData, TeamConfigGUI teamConfigGUI) {
        super(gui, new ItemStack(Material.ACACIA_SIGN));
        setDisplayName(Util.f("チームカラーを変更する"));
        this.teamData = teamData;
        this.teamConfigGUI = teamConfigGUI;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.getWhoClicked().playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.HOSTILE, 1,1));
        gui.openPage(new dev.felnull.pointed.teams.gui.page.SelectColor(gui, teamData, teamConfigGUI));
    }
}
