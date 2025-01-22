package dev.felnull.pointed.commands;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.gui.page.RewardPage;
import dev.felnull.pointed.gui.page.RewardSettingsGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class OpenReward implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof ConsoleCommandSender){
            Bukkit.getLogger().warning("このコマンドはプレイヤー専用です!");
            return false;
        }
        InventoryGUI gui = new InventoryGUI((Player) sender);
        gui.openPage(new RewardPage(gui));
        return true;
    }
}
