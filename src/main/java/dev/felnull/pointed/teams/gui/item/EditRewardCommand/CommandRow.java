package dev.felnull.pointed.teams.gui.item.EditRewardCommand;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.EditRewardCommand;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import dev.felnull.pointed.teams.manager.reward.data.RewardCommandRow;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class CommandRow extends GUIItem {
    RewardCommandRow rewardCommandRow;
    EditRewardCommand page;
    RewardAdminService adminService;
    int rewardID;
    public CommandRow(InventoryGUI gui, RewardCommandRow rewardCommandRow, EditRewardCommand page, int rewardID) {
        super(gui, new ItemStack(Material.COMMAND_BLOCK));
        this.rewardCommandRow = rewardCommandRow;
        this.page = page;
        this.adminService = Pointed.getInstance().getRewardAdminService();
        this.rewardID = rewardID;
        if(!rewardCommandRow.enabled){
            this.itemStack = new ItemStack(Material.BARRIER);
        }
        setDisplayName("Priority: " + rewardCommandRow.idx);
        setLore(Arrays.asList(
                Component.text("Command: " + rewardCommandRow.commandText),
                Util.c("Active: " + rewardCommandRow.enabled),
                Util.c("Press: [LeftClick] UpdateCMD"),
                Util.c("[RightClick] RemoveCMD"),
                Util.c("[Shift + LeftClick] AddPriority"),
                Util.c("[Shift + RightClick] SubtractPriority"),
                Util.c("[F] ToggleEnabled"),
                Util.c("[Q] AddNewCommand")
        ));
    }

    @Override
    public void onLeftClick(InventoryClickEvent e) {
        openUpdateCMDAnvil((Player) e.getWhoClicked());
    }

    public void openUpdateCMDAnvil(Player player) {
        String current = rewardCommandRow.commandText;

        new AnvilGUI.Builder()
                .plugin(Pointed.getInstance())
                .title("新しいコマンドを入力")
                .text(Util.r(current)) // 初期表示（色コードは剥がす）
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();

                    String inputRaw = state.getText().trim();

                    // バリデーション（例）
                    if (inputRaw.isEmpty()) {
                        player.sendMessage(Util.f("&c空のコマンドは設定できません"));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("もう一度"));
                    }
                    if (inputRaw.length() > 50) {
                        player.sendMessage(Util.f("&c50文字以内で入力してください (&7{0}&c)", inputRaw.length()));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("50文字以内!"));
                    }

                    try {
                        adminService.updateCommandText(rewardID, rewardCommandRow.idx, inputRaw);
                        player.sendMessage(Util.f("&a実行するコマンドを &f{0} &aに変更しました", inputRaw));
                    } catch (SQLException e) {
                        player.sendMessage(Util.f("&a実行するコマンドを &f{0} &aに変更できませんでした...", inputRaw));
                        throw new RuntimeException(e);
                    }
                    page.reopen();
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

    @Override
    public void onRightClick(InventoryClickEvent e) {
        try {
            adminService.removeCommand(rewardID, rewardCommandRow.idx);
            e.getWhoClicked().sendMessage(rewardCommandRow.idx + "を削除しました!");
        } catch (SQLException ex) {
            e.getWhoClicked().sendMessage(rewardCommandRow.idx + "を削除できませんでした...");
            throw new RuntimeException(ex);
        }
        page.setUp();
    }

    @Override
    public void onShiftLeftClick(InventoryClickEvent e) {
        if(rewardCommandRow.idx <= 0){
            return;
        }
        try {
            adminService.moveCommand(rewardID, rewardCommandRow.idx, rewardCommandRow.idx - 1);
            e.getWhoClicked().sendMessage((rewardCommandRow.idx - 1) + "に変更しました");
        } catch (SQLException ex) {
            e.getWhoClicked().sendMessage((rewardCommandRow.idx - 1) + "に変更できませんでした...");
            throw new RuntimeException(ex);
        }
        page.setUp();
    }

    @Override
    public void onShiftRightClick(InventoryClickEvent e) {
        try {
            adminService.moveCommand(rewardID, rewardCommandRow.idx, rewardCommandRow.idx + 1);
            e.getWhoClicked().sendMessage((rewardCommandRow.idx + 1) + "に変更しました");
        } catch (SQLException ex) {
            e.getWhoClicked().sendMessage((rewardCommandRow.idx + 1) + "に変更できませんでした...");
            throw new RuntimeException(ex);
        }
        page.setUp();
    }

    @Override
    public void onOffhandClick(InventoryClickEvent e) {
        try {
            adminService.setCommandEnabled(rewardID, rewardCommandRow.idx, !rewardCommandRow.enabled);
            e.getWhoClicked().sendMessage(!rewardCommandRow.enabled + "に変更しました");
        } catch (SQLException ex) {
            e.getWhoClicked().sendMessage(!rewardCommandRow.enabled + "に変更できませんでした...");
            throw new RuntimeException(ex);
        }
        page.setUp();
    }

    @Override
    public void onDropClick(InventoryClickEvent e) {
       openAddCommandAnvil((Player) e.getWhoClicked());
    }

    public void openAddCommandAnvil(Player player) {
        String current = rewardCommandRow.commandText;

        new AnvilGUI.Builder()
                .plugin(Pointed.getInstance())
                .title("新しいコマンドを入力")
                .text(Util.r(current)) // 初期表示（色コードは剥がす）
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();

                    String inputRaw = state.getText().trim();

                    // バリデーション（例）
                    if (inputRaw.isEmpty()) {
                        player.sendMessage(Util.f("&c空のコマンドは設定できません"));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("もう一度"));
                    }
                    if (inputRaw.length() > 50) {
                        player.sendMessage(Util.f("&c50文字以内で入力してください (&7{0}&c)", inputRaw.length()));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("50文字以内!"));
                    }

                    try {
                        adminService.addCommand(rewardID, inputRaw, null);
                        player.sendMessage(Util.f("&a実行するコマンドを &f{0} &aに変更しました", inputRaw));
                    } catch (SQLException e) {
                        player.sendMessage(Util.f("&a実行するコマンドを &f{0} &aに変更できませんでした...", inputRaw));
                        throw new RuntimeException(e);
                    }
                    page.reopen();
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }

}
