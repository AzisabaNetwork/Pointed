package dev.felnull.pointed.teams.gui.page;

import dev.felnull.bettergui.core.GUIPage;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.bettergui.listener.GUIClickListener;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.gui.PointedGUIPage;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.item.EditRewards.AddReward;
import dev.felnull.pointed.teams.gui.item.EditRewards.NextPage;
import dev.felnull.pointed.teams.gui.item.EditRewards.ReturnPage;
import dev.felnull.pointed.teams.gui.item.EditRewards.RewardInfo;
import dev.felnull.pointed.teams.gui.item.TeamConfigUI.DisplayName;
import dev.felnull.pointed.teams.gui.item.TeamConfigUI.Member;
import dev.felnull.pointed.teams.gui.item.TeamConfigUI.SelectColor;
import dev.felnull.pointed.teams.gui.item.TeamConfigUI.TeamPreview;
import dev.felnull.pointed.teams.manager.TeamManager;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import dev.felnull.pointed.teams.manager.reward.data.RewardDetail;
import dev.felnull.pointed.teams.manager.reward.data.RewardSummary;
import dev.felnull.pointed.teams.manager.reward.data.TeamData;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

public class EditRewards extends PointedGUIPage {
    RewardAdminService rewardAdminService = Pointed.getInstance().getRewardAdminService();
    private boolean closed = false;
    public int offset;
    public int limit = 5*9;
    public int count;
    public EditRewards(InventoryGUI gui) throws SQLException {
        super(gui, Util.f("リワード一覧"), 6*9);
        this.offset = 0;
        this.count = rewardAdminService.countRewards(false, null);
    }

    @Override
    public void setUp() {
        inventory.clear();
        try {
            List<RewardSummary> rewardSummaries = rewardAdminService.listRewards(null, null, "name_asc", limit, offset);
            int i = 0;
            for(RewardSummary rewardSummary : rewardSummaries){
                setItem(i, new RewardInfo(gui, rewardSummary, this));
                i++;
            }

            setItem(45, new ReturnPage(gui,this));
            setItem(52, new AddReward(gui, this));
            setItem(53, new NextPage(gui, this));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public void setPage(int page){
        this.offset = 5 * 9 * page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    /** 再登録して再表示 */
    public void reopen(int page) {
        // リスナー再生成・再登録
        this.listener = new GUIClickListener(this);
        Bukkit.getPluginManager().registerEvents(listener, Pointed.getInstance());

        // 中身を組み立て直す
        setPage(page);
        this.setUp();
        this.gui.player.openInventory(this.inventory);
        this.closed = false;
    }
}
