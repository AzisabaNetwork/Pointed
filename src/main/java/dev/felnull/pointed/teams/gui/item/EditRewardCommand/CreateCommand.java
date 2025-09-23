package dev.felnull.pointed.teams.gui.item.EditRewardCommand;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.EditRewardCommand;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;

public class CreateCommand extends GUIItem {

    EditRewardCommand page;
    int rewardID;

    public CreateCommand(InventoryGUI gui, EditRewardCommand page, int rewardId) {
        super(gui, new ItemStack(Material.REPEATING_COMMAND_BLOCK));
        setDisplayName(Util.f("新しくコマンドを割り当て"));
        this.page = page;
        this.rewardID = rewardId;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        openAddCMDAnvil((Player) e.getWhoClicked());
    }

    public void openAddCMDAnvil(Player player) {
        RewardAdminService adminService = Pointed.getInstance().getRewardAdminService();
        String current = "新規";

        new AnvilGUI.Builder()
                .plugin(Pointed.getInstance())
                .title("新しいコマンドを入力")
                .text(Util.r(current)) // 初期表示（色コードは剥がす）
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();

                    String inputRaw = state.getText().trim();

                    // バリデーション（例）
                    if (inputRaw.isEmpty()) {
                        player.sendMessage(Util.f("&c空のリワードは設定できません"));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("もう一度"));
                    }
                    if (inputRaw.length() > 50) {
                        player.sendMessage(Util.f("&c50文字以内で入力してください (&7{0}&c)", inputRaw.length()));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("50文字以内!"));
                    }

                    try {
                        adminService.addCommand(rewardID, inputRaw, null);
                        player.sendMessage(Util.f("&f{0} &aで登録しました", inputRaw));
                    } catch (SQLException e) {
                        player.sendMessage(Util.f("&f{0} &aで登録できませんでした...", inputRaw));
                        throw new RuntimeException(e);
                    }
                    page.reopen();
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }
}
