package dev.felnull.pointed.teams.gui.page;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.bettergui.listener.GUIClickListener;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.gui.PointedGUIPage;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.item.EditRewardCommand.*;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import dev.felnull.pointed.teams.manager.reward.data.RewardCommandRow;
import dev.felnull.pointed.teams.manager.reward.data.RewardDetail;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

public class EditRewardCommand extends PointedGUIPage {
    RewardAdminService rewardAdminService;
    private boolean closed = false;
    RewardDetail rewardDetail;
    EditRewards editRewardsPage;
    public int invStartPosition;
    private final int DISPLAY_SLOT_COUNT = 45;
    private int MAX_SLOT_COUNT;

    public EditRewardCommand(InventoryGUI gui, int id, EditRewards editRewardsPage) {
        super(gui, Util.f("リワードID:" + id), 6*9);
        this.rewardAdminService = Pointed.getInstance().getRewardAdminService();
        try {
            this.rewardDetail = Pointed.getInstance().rewardAdminService.getRewardDetail(id, false);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        this.editRewardsPage = editRewardsPage;
        this.invStartPosition = 0;
        this.MAX_SLOT_COUNT = rewardDetail.commands.size();
    }

    @Override
    public void setUp() {
        try {
            this.rewardDetail = Pointed.getInstance().rewardAdminService.getRewardDetail(rewardDetail.id, false);
        } catch (SQLException ex) {
            gui.player.closeInventory();
            gui.player.sendMessage(NamedTextColor.DARK_RED + "報酬の読み込みに失敗");
        }
        List<RewardCommandRow> rCRList = rewardDetail.commands;
        if(rCRList.isEmpty()){
            setItem(0, new CreateCommand(gui, this, rewardDetail.id));
            return;
        }

        int displaySlots = 45; // 6行中、下5行＝5*9スロット（0-8はボタンなど用）
        for (int i = 0; i < displaySlots; i++) {
            int slot = invStartPosition + i;
            Bukkit.getLogger().info("now: " + slot);
            if(rCRList.size() <= slot){
                break;
            }
            setItem(i, new CommandRow(gui, rCRList.get(slot), this, rewardDetail.id));
        }
        if(invStartPosition / 45 >= 1){
            setItem(45, new ReturnPage(gui, this));
        }
        setItem(52, new BackPage(gui, this));
        if(rCRList.size() - invStartPosition <= 1){
            setItem(53, new NextPage(gui, this));
        }

    }

    @Override
    public void back() {
        editRewardsPage.reopen(0);
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

    public void changeSlotStartPosition(int page) {
        if(page < 0){
            return;
        }
        this.invStartPosition = page * 9 * 5;

        int maxStart = MAX_SLOT_COUNT - DISPLAY_SLOT_COUNT;
        if (this.invStartPosition > maxStart) {
            this.invStartPosition = maxStart;
        } else if (this.invStartPosition < 0) {
            this.invStartPosition = 0;
        }

        this.setUp();
    }
}
