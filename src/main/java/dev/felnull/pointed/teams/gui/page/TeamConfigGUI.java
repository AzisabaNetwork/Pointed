package dev.felnull.pointed.teams.gui.page;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.bettergui.listener.GUIClickListener;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.gui.PointedGUIPage;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.item.TeamConfigUI.DisplayName;
import dev.felnull.pointed.teams.gui.item.TeamConfigUI.Member;
import dev.felnull.pointed.teams.gui.item.TeamConfigUI.SelectColor;
import dev.felnull.pointed.teams.gui.item.TeamConfigUI.TeamPreview;
import dev.felnull.pointed.teams.manager.reward.data.TeamData;
import dev.felnull.pointed.teams.manager.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class TeamConfigGUI extends PointedGUIPage {
    TeamData teamData;
    TeamManager teamManager;
    private boolean closed = false;
    public TeamConfigGUI(InventoryGUI gui, String teamID) {
        super(gui, Util.f("&6TeamConfig-" + teamID), 6*9);
        this.teamManager = Pointed.getInstance().getTeamManager();
        this.teamData = teamManager.loadTeam(teamID)
                .orElse(new TeamData(teamID, "新規チーム", "&7"));
    }

    @Override
    public void setUp() {
        setItem(19, new DisplayName(gui, teamData, this));
        setItem(20, new SelectColor(gui, teamData, this));
        setItem(21, new Member(gui, teamData));
        setItem(25, new TeamPreview(gui, teamData));
    }

    @Override
    public void back() {

    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        HandlerList.unregisterAll(listener);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    /** 再登録して再表示 */
    public void reopen() {
        // リスナー再生成・再登録
        this.listener = new GUIClickListener(this);
        Bukkit.getPluginManager().registerEvents(listener, Pointed.getInstance());

        // 中身を組み立て直す
        this.setUp();
        this.gui.player.openInventory(this.inventory);
        this.closed = false;
    }
}
