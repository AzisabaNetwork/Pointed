package dev.felnull.pointed.teams.gui.item.TeamConfigUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.manager.reward.data.TeamData;
import dev.felnull.pointed.teams.manager.TeamManager;
import net.kyori.adventure.sound.Sound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class Success extends GUIItem {
    TeamData teamData;
    public Success(InventoryGUI gui, TeamData teamData) {
        super(gui, new ItemStack(Material.ACACIA_SIGN));
        setDisplayName(Util.f(teamData.color() + teamData.name()));
        this.teamData = teamData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        TeamManager teamManager = Pointed.getInstance().getTeamManager();
        e.getWhoClicked().playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.HOSTILE, 1,1));
        teamManager.upsertTeam(teamData.id(), teamData.name(), teamData.color(), 1000, true);
        e.getWhoClicked().closeInventory();
        e.getWhoClicked().sendMessage("チーム情報を保存しました!" + teamData.name() + ChatColor.GRAY + "(ID: " + teamData.id() + ")");
    }
}
