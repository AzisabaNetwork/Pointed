package dev.felnull.pointed.teams.gui.item.TeamConfigUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.manager.reward.data.TeamData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class Cancel extends GUIItem {
    TeamData teamData;
    public Cancel(InventoryGUI gui, TeamData teamData) {
        super(gui, new ItemStack(Material.ACACIA_SIGN));
        setDisplayName(Util.f(teamData.color() + teamData.name()));
        this.teamData = teamData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.getWhoClicked().playSound(Sound.sound(org.bukkit.Sound.UI_TOAST_OUT, Sound.Source.HOSTILE, 1,1));
    }
}
