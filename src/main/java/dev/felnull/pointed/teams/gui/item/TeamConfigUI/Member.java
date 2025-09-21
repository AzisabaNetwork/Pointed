package dev.felnull.pointed.teams.gui.item.TeamConfigUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.manager.TeamData;
import dev.felnull.pointed.teams.manager.TeamManager;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Member extends GUIItem {
    TeamData teamData;
    public Member(InventoryGUI gui, TeamData teamData) {
        super(gui, new ItemStack(Material.ACACIA_SIGN));
        setDisplayName(Util.f(teamData.color() + teamData.name()));
        this.teamData = teamData;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.getWhoClicked().playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.HOSTILE, 1,1));
        e.getWhoClicked().sendMessage(teamData.name() + "所属メンバー");
        for(OfflinePlayer player : Pointed.getInstance().getTeamManager().listMemberPlayers(teamData.id())){
            if(player.getName() != null){
                e.getWhoClicked().sendMessage(player.getName());
            }
        }
    }
}
