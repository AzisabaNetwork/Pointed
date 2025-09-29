package dev.felnull.pointed.core.commands;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.database.data.RankRow;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.EditRewards;
import dev.felnull.pointed.teams.gui.page.TeamConfigGUI;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PtTeam implements CommandExecutor, TabCompleter {

    public PtTeam() {
    }



    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Util.f("&e/ptteam <set> <scope> <teamID>"));
            return true;
        }

        if (!sender.hasPermission("pointed.admin")) {
            sender.sendMessage(Util.f("&c権限がありません。"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "set" -> {
                if (args.length < 2) {
                    sender.sendMessage(Util.f("&e/ptteam <set> <teamID>"));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Util.f("Playerのみが使用可能なコマンドです"));
                    return true;
                }
                InventoryGUI gui = new InventoryGUI((Player) sender);
                gui.openPage(new TeamConfigGUI(gui, args[1]));
            }
            case "reward" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(Util.f("Playerのみが使用可能なコマンドです"));
                    return true;
                }
                InventoryGUI gui = new InventoryGUI((Player) sender);
                try {
                    gui.openPage(new EditRewards(gui));
                } catch (SQLException e) {
                    sender.sendMessage("guiを開けませんでした、、");
                    throw new RuntimeException(e);
                }
            }
            case "forcegive" -> {
                if(args.length < 3){
                    sender.sendMessage("引数が足りません");
                    return true;
                }

                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayerIfCached(args[1]);
                if(targetPlayer == null){
                    sender.sendMessage("指定されたプレイヤーは存在しません");
                    return true;
                }

                RewardAdminService adminService = Pointed.getInstance().getRewardAdminService();
                try {
                    adminService.dispatchManual(targetPlayer.getUniqueId(), targetPlayer.getName(), Integer.parseInt(args[2]));
                } catch (NumberFormatException e) {
                    sender.sendMessage("数字で入力してください!!!! : " + args[2]);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
            case "checkreward" -> {
                if(args.length < 2){
                    sender.sendMessage("引数が足りません");
                    return true;
                }
                RewardAdminService adminService = Pointed.getInstance().getRewardAdminService();
                try {
                    if(adminService.rewardExists(Integer.parseInt(args[1]))){
                        sender.sendMessage("指定したリワードは存在します");
                    }else {
                        sender.sendMessage("指定したリワードは存在しません...");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("rewardIDを数字で入力してください!!!! : " + args[1]);
                }
            }
            default -> sender.sendMessage(Util.f("/ptteam <set> <teamID>"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of("set", "reward", "forcegive");
        }
        // サブコマンド毎の type 補完
        if (args.length == 2 && List.of("getnow", "gettotal", "get", "add", "sub", "set").contains(args[0].toLowerCase())) {
            return List.of("PLAYER", "TEAM", "SYSTEM");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rank")) {
            return List.of("daily", "weekly", "global");
        }
        return List.of();
    }
}
