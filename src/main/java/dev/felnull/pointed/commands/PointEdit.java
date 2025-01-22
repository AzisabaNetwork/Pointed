package dev.felnull.pointed.commands;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PointEdit implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        OfflinePlayer targetOfflinePlayer = targetPlayer;
        if(targetPlayer == null){
            targetOfflinePlayer = Bukkit.getOfflinePlayer(args[1]);
            if(!targetOfflinePlayer.hasPlayedBefore()){
                sender.sendMessage("対象のプレイヤーは存在しません");
                return true;
            }
        }
        PlayerPointData playerPointData = PlayerPointDataIO.loadPlayerPointData(targetOfflinePlayer);
        int number;
        try{
            number = Integer.parseInt(args[2].replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            sender.sendMessage("数字を入力してください!!");
            return true;
        }
        switch (args[0]){
            case "add":
                playerPointData.addPoint(PointList.EVENT_POINT.getName(), number);
                break;
            case "subtract":
                playerPointData.subtractPoint(PointList.EVENT_POINT.getName(), number);
                break;
            case "set":
                playerPointData.setPoint(PointList.EVENT_POINT.getName(), number);
                break;
        }
        return true;
    }
}
