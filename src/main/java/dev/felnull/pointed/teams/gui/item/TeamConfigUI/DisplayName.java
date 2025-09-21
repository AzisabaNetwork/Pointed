package dev.felnull.pointed.teams.gui.item.TeamConfigUI;

import dev.felnull.bettergui.core.GUIItem;
import dev.felnull.bettergui.core.GUIPage;
import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.TeamConfigGUI;
import dev.felnull.pointed.teams.manager.TeamData;
import dev.felnull.pointed.teams.manager.TeamManager;
import net.kyori.adventure.sound.Sound;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DisplayName extends GUIItem {
    private TeamConfigGUI page;
    private TeamData teamData;
    public DisplayName(InventoryGUI gui, TeamData teamData, TeamConfigGUI page) {
        super(gui, new ItemStack(Material.ACACIA_SIGN));
        setDisplayName(Util.f("表示名を変更!"));
        this.teamData = teamData;
        this.page = page;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.getWhoClicked().playSound(Sound.sound(org.bukkit.Sound.UI_BUTTON_CLICK, Sound.Source.HOSTILE, 1,1));
        openTeamNameAnvil(gui.player);
    }

    public void openTeamNameAnvil(Player player) {
        String current = teamData.name();

        new AnvilGUI.Builder()
                .plugin(Pointed.getInstance())
                .title("新しいチーム名を入力")
                .text(Util.r(current)) // 初期表示（色コードは剥がす）
                .onClick((slot, state) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return java.util.Collections.emptyList();

                    String inputRaw = state.getText().trim();

                    // バリデーション（例）
                    if (inputRaw.isEmpty()) {
                        player.sendMessage(Util.f("&c空の名前は設定できません"));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("もう一度"));
                    }
                    if (inputRaw.length() > 32) {
                        player.sendMessage(Util.f("&c32文字以内で入力してください (&7{0}&c)", inputRaw.length()));
                        return java.util.List.of(AnvilGUI.ResponseAction.replaceInputText("32文字以内!"));
                    }

                    teamData.setName(Util.r(inputRaw));

                    player.sendMessage(Util.f("&aチーム名を &f{0} &aに変更しました", inputRaw));
                    page.reopen();
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .open(player);
    }
}
