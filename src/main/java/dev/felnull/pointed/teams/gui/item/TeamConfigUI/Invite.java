package dev.felnull.pointed.teams.gui.item.TeamConfigUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.manager.TeamData;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class Invite extends GUIItem {
    TeamData teamData;
    public Invite(InventoryGUI gui, TeamData teamData) {
        super(gui, new ItemStack(Material.ACACIA_SIGN));
        setDisplayName(Util.f(teamData.color() + teamData.name()));
        this.teamData = teamData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.getWhoClicked().playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.HOSTILE, 1,1));
    }
}
