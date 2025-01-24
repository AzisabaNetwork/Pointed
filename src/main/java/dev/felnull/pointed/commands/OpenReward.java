package dev.felnull.pointed.commands;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.gui.page.RewardPage;
import dev.felnull.pointed.gui.page.RewardSettingsGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
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
            return true;
        }

        Player player = (Player) sender;
        //Lobbyではない場合無効化
        if(!sender.hasPermission("pointed.command.admin")){
            if(!Pointed.isLobby){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lロビーでのみ使用可能です"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
            if(!Pointed.canUseRewardPage){
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&l現在報酬受け取りが制限されています"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return true;
            }
        }else{
            if(!Pointed.canUseRewardPage){
                sender.sendMessage("注意!!現在リワードページを制限中です");
                sender.sendMessage("運営のため制限を解除しました");
                sender.sendMessage("本来は制限がかかっているため注意してください");
            }else if(!Pointed.isLobby){
                sender.sendMessage("注意!!ロビー以外での報酬受け取りを制限中です");
                sender.sendMessage("運営のため制限を解除しました");
                sender.sendMessage("本来は制限がかかっているため注意してください");
            }

        }

        InventoryGUI gui = new InventoryGUI((Player) sender);
        gui.openPage(new RewardPage(gui));
        return true;
    }
}
