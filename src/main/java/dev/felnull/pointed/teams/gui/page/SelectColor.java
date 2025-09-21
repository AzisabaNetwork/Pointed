package dev.felnull.pointed.teams.gui.page;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.core.gui.PointedGUIPage;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.item.SelectColor.NamedColor;
import dev.felnull.pointed.teams.manager.TeamData;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SelectColor extends PointedGUIPage {
    TeamData teamData;
    TeamConfigGUI teamConfigGUI;
    public SelectColor(InventoryGUI gui, TeamData teamData, TeamConfigGUI teamConfigGUI) {
        super(gui, Util.f("&6チームカラー設定"), 2*9);
        this.teamData = teamData;
        this.teamConfigGUI = teamConfigGUI;
    }

    @Override
    public void setUp() {
        int i = 0;
        for(NamedTextColor namedTextColor : NamedTextColor.NAMES.values()) {
            setItem(i, new NamedColor(gui, teamConfigGUI, new ItemStack(woolOf(namedTextColor)), teamData, namedTextColor));
            i++;
        }
    }

    @Override
    public void back() {

    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    private static final Map<NamedTextColor, Material> COLOR_TO_WOOL = Map.ofEntries(
            Map.entry(NamedTextColor.BLACK, Material.BLACK_WOOL),
            Map.entry(NamedTextColor.DARK_BLUE, Material.BLUE_WOOL),       // 濃青を普通の青に寄せる
            Map.entry(NamedTextColor.BLUE, Material.LIGHT_BLUE_WOOL),      // 通常青は水色寄せにする例
            Map.entry(NamedTextColor.DARK_GREEN, Material.GREEN_WOOL),
            Map.entry(NamedTextColor.GREEN, Material.LIME_WOOL),
            Map.entry(NamedTextColor.DARK_RED, Material.RED_WOOL),
            Map.entry(NamedTextColor.RED, Material.RED_WOOL),
            Map.entry(NamedTextColor.GOLD, Material.ORANGE_WOOL),
            Map.entry(NamedTextColor.YELLOW, Material.YELLOW_WOOL),
            Map.entry(NamedTextColor.DARK_PURPLE, Material.PURPLE_WOOL),
            Map.entry(NamedTextColor.LIGHT_PURPLE, Material.MAGENTA_WOOL),
            Map.entry(NamedTextColor.AQUA, Material.CYAN_WOOL),
            Map.entry(NamedTextColor.DARK_AQUA, Material.CYAN_WOOL),
            Map.entry(NamedTextColor.GRAY, Material.LIGHT_GRAY_WOOL),
            Map.entry(NamedTextColor.DARK_GRAY, Material.GRAY_WOOL),
            Map.entry(NamedTextColor.WHITE, Material.WHITE_WOOL)
    );

    public static Material woolOf(NamedTextColor color) {
        return COLOR_TO_WOOL.getOrDefault(color, Material.WHITE_WOOL);
    }
}
