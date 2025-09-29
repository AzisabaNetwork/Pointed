package dev.felnull.pointed.teams.gui.item.EditRewards;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.EditRewards;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AddReward extends GUIItem {

    RewardAdminService adminService;
    EditRewards page;

    public AddReward(InventoryGUI gui, EditRewards page) {
        super(gui, new ItemStack(Material.WRITABLE_BOOK));
        setDisplayName(Util.f("新規リワードを作成"));
        this.page = page;
        this.adminService = Pointed.getInstance().getRewardAdminService();
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        openCreateRewardAnvil((Player) e.getWhoClicked());
    }

    public void openCreateRewardAnvil(Player player) {
        String current = "新規Reward";

        new AnvilGUI.Builder()
                .plugin(Pointed.getInstance())
                .title("新しいコマンドを入力")
                .text(Util.r(current)) // 初期表示（色コードは剥がす）
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String raw = Optional.ofNullable(state.getText()).orElse("").trim();
                    if (raw.isEmpty()) {
                        player.sendMessage(Util.f("&c入力してください"));
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("ID:名前"));
                    }

                    int colon = raw.indexOf(':');
                    if (colon < 0) {
                        player.sendMessage(Util.f("&c「ID:名前」の形式で入力してください"));
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("1:テストReward"));
                    }

                    String idPart = raw.substring(0, colon).trim();
                    String namePart = Util.r(raw.substring(colon + 1).trim());

                    int rewardId;
                    try {
                        rewardId = Integer.parseInt(idPart);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(Util.f("&cIDは整数で入力してください: &7{0}", idPart));
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("1:テストReward"));
                    }

                    if (namePart.isEmpty()) {
                        player.sendMessage(Util.f("&c名前が空です"));
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("名前を入力"));
                    }
                    if (namePart.length() > 50) {
                        player.sendMessage(Util.f("&c50文字以内で入力してください (&7{0}&c)", namePart.length()));
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("50文字以内"));
                    }

                    // DB登録は非同期
                    Bukkit.getScheduler().runTaskAsynchronously(Pointed.getInstance(), () -> {
                        String msg;
                        try {
                            adminService.createReward(rewardId, namePart, 100, 100, false, false);
                            msg = Util.f("&aリワード登録完了: ID &f{0}&a, 名前 &f{1}", rewardId, namePart);
                        } catch (SQLException e) {
                            msg = Util.f("&cリワード登録失敗: {0}", e.getMessage());
                        }

                        String finalMsg = msg;
                        Bukkit.getScheduler().runTask(Pointed.getInstance(), () -> {
                            player.sendMessage(finalMsg);
                            page.reopen(0);
                        });
                    });

                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }
}
