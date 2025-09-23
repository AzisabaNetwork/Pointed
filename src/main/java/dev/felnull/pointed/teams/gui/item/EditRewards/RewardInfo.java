package dev.felnull.pointed.teams.gui.item.EditRewards;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.EditRewardCommand;
import dev.felnull.pointed.teams.gui.page.EditRewards;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import dev.felnull.pointed.teams.manager.reward.data.RewardDetail;
import dev.felnull.pointed.teams.manager.reward.data.RewardSummary;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RewardInfo extends GUIItem {
    RewardAdminService adminService;
    RewardSummary rewardSummary;
    EditRewards editRewardsPage;
    public RewardInfo(InventoryGUI gui, RewardSummary rewardSummary, EditRewards editRewardsPage) {
        super(gui, new ItemStack(Material.COMMAND_BLOCK));
        this.rewardSummary = rewardSummary;
        this.editRewardsPage = editRewardsPage;
        this.adminService = Pointed.getInstance().getRewardAdminService();
        if(!rewardSummary.active){
            this.itemStack = new ItemStack(Material.BARRIER);
        }
        setDisplayName(Util.f("ID: " + rewardSummary.id + " Display: " + rewardSummary.displayName));
        LocalDateTime ldt = rewardSummary.updatedAt.toLocalDateTime();
        String formatted = ldt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        setLore(Arrays.asList(
                Util.c("Update: " + formatted),
                Util.c("Active: " + rewardSummary.active),
                Util.c("Command: *" + rewardSummary.commandCount),
                Util.c("Repeatable: " + rewardSummary.repeatable),
                Util.c("Press: [LeftClick] EditCommand"),
                Util.c("[MiddleClick] ToggleActive"),
                Util.c("[RightClick] EditDisplayName"),
                Util.c("[Shift + LeftClick] EditNeedPoint"),
                Util.c("[Shift + RightClick] EditNeedMinTotal"),
                Util.c("[F] ToggleRepeatable"),
                Util.c("[Q] RemoveReward")
        ));
    }

    @Override
    public void onLeftClick(InventoryClickEvent e) {
        gui.openPage(new EditRewardCommand(gui, rewardSummary.id, editRewardsPage));
    }

    @Override
    public void onMiddleClick(InventoryClickEvent e) {
        try {
            adminService.updateReward(rewardSummary.id, rewardSummary.displayName, rewardSummary.needPoint, rewardSummary.needMinTotal, rewardSummary.repeatable, !rewardSummary.active);
            e.getWhoClicked().sendMessage(!rewardSummary.active + "に変更しました");
        } catch (SQLException ex) {
            e.getWhoClicked().sendMessage(!rewardSummary.active + "に変更できませんでした...");
            throw new RuntimeException(ex);
        }
        editRewardsPage.setUp();
    }

    @Override
    public void onRightClick(InventoryClickEvent e) {
        openChangeDisplayNameAnvil((Player) e.getWhoClicked());
    }

    public void openChangeDisplayNameAnvil(Player player) {
        String current = rewardSummary.displayName;

        new AnvilGUI.Builder()
                .plugin(Pointed.getInstance())
                .title("新しいリワード名を入力")
                .text(Util.r(current)) // 初期表示（色コードは剥がす）
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();

                    String inputRaw = state.getText().trim();

                    // バリデーション（例）
                    if (inputRaw.isEmpty()) {
                        player.sendMessage(Util.f("&c空のリワード名は設定できません"));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("もう一度"));
                    }
                    if (inputRaw.length() > 50) {
                        player.sendMessage(Util.f("&c50文字以内で入力してください (&7{0}&c)", inputRaw.length()));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("50文字以内!"));
                    }

                    try {
                        adminService.updateReward(rewardSummary.id, inputRaw, rewardSummary.needPoint, rewardSummary.needMinTotal, rewardSummary.repeatable, rewardSummary.active);
                        player.sendMessage(rewardSummary.displayName + "に変更しました");
                    } catch (SQLException ex) {
                        player.sendMessage(rewardSummary.displayName + "に変更できませんでした...");
                        throw new RuntimeException(ex);
                    }
                    editRewardsPage.reopen(0);
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    @Override
    public void onShiftLeftClick(InventoryClickEvent e) {
        openEditNeedPointAnvil((Player) e.getWhoClicked());
    }

    public void openEditNeedPointAnvil(Player player) {
        String current = String.valueOf(rewardSummary.needPoint);

        new AnvilGUI.Builder()
                .plugin(Pointed.getInstance())
                .title("必要ポイント数を入力")
                .text(Util.r(current))
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();

                    String inputRaw = state.getText().trim();

                    if (inputRaw.isEmpty()) {
                        player.sendMessage(Util.f("&c空の数字は設定できません"));
                        return List.of(
                                AnvilGUI.ResponseAction.replaceInputText("もう一度"),
                                AnvilGUI.ResponseAction.run(() ->
                                        player.setItemOnCursor(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR))
                                )
                        );
                    }
                    if (inputRaw.length() > 50) { // UI向けガード
                        player.sendMessage(Util.f("&c50文字以内で入力してください (&7{0}&c)", inputRaw.length()));
                        return List.of(
                                AnvilGUI.ResponseAction.replaceInputText("50文字以内!"),
                                AnvilGUI.ResponseAction.run(() ->
                                        player.setItemOnCursor(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR))
                                )
                        );
                    }

                    final int needPoint;
                    try {
                        needPoint = Integer.parseInt(inputRaw);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Util.f("&c数字を入力してください"));
                        return List.of(
                                AnvilGUI.ResponseAction.replaceInputText("数字だけ！"),
                                AnvilGUI.ResponseAction.run(() ->
                                        player.setItemOnCursor(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR))
                                )
                        );
                    }

                    try {
                        adminService.updateReward(
                                rewardSummary.id,
                                rewardSummary.displayName,
                                needPoint,
                                rewardSummary.needMinTotal,
                                rewardSummary.repeatable,
                                rewardSummary.active
                        );
                        player.sendMessage(needPoint + "に変更しました");
                    } catch (SQLException ex) {
                        player.sendMessage(needPoint + "に変更できませんでした...");
                        throw new RuntimeException(ex);
                    }

                    editRewardsPage.reopen(0);
                    return java.util.List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    @Override
    public void onShiftRightClick(InventoryClickEvent e) {
        openEditNeedMinPointAnvil((Player) e.getWhoClicked());
    }

    public void openEditNeedMinPointAnvil(Player player) {
        String current = String.valueOf(rewardSummary.needPoint);

        new AnvilGUI.Builder()
                .plugin(Pointed.getInstance())
                .title("必要ポイント数を入力")
                .text(Util.r(current)) // 初期表示（色コードは剥がす）
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String inputRaw = state.getText().trim();

                    // バリデーション（例）
                    if (inputRaw.isEmpty()) {
                        player.sendMessage(Util.f("&c空の数字は設定できません"));
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("もう一度"));
                    }
                    if (inputRaw.length() > 50) {
                        player.sendMessage(Util.f("&c50文字以内で入力してください (&7{0}&c)", inputRaw.length()));
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("50文字以内!"));
                    }
                    int needMinPoint = 0;
                    try {
                        needMinPoint = Integer.parseInt(inputRaw);
                    } catch (NumberFormatException e) {
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("数字を入力してください"));
                    }

                    try {
                        adminService.updateReward(rewardSummary.id, rewardSummary.displayName, rewardSummary.needPoint, needMinPoint, rewardSummary.repeatable, rewardSummary.active);
                        player.sendMessage(needMinPoint + "に変更しました");
                    } catch (SQLException ex) {
                        player.sendMessage(needMinPoint + "に変更できませんでした...");
                        throw new RuntimeException(ex);
                    }
                    editRewardsPage.reopen(0);
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    @Override
    public void onOffhandClick(InventoryClickEvent e) {
        try {
            adminService.updateReward(rewardSummary.id, rewardSummary.displayName, rewardSummary.needPoint, rewardSummary.needMinTotal, !rewardSummary.repeatable, rewardSummary.active);
            e.getWhoClicked().sendMessage(!rewardSummary.repeatable + "に変更しました");
        } catch (SQLException ex) {
            e.getWhoClicked().sendMessage(!rewardSummary.repeatable + "に変更できませんでした...");
            throw new RuntimeException(ex);
        }
        editRewardsPage.setUp();
    }

    @Override
    public void onDropClick(InventoryClickEvent e) {
        try {
            adminService.deleteReward(rewardSummary.id, false);
            e.getWhoClicked().sendMessage("リワードの削除に成功しました");
        } catch (SQLException ex) {
            e.getWhoClicked().sendMessage("リワードの削除に失敗しました...");
            throw new RuntimeException(ex);
        }
        editRewardsPage.setUp();
    }
}
