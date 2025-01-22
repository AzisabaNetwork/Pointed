package dev.felnull.pointed.commands;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MyPoint implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof ConsoleCommandSender){
            sender.sendMessage("このコマンドはプレイヤー専用です");
            return true;
        }

        Player player = (Player) sender;
        PlayerPointData playerPointData = PlayerPointDataIO.loadPlayerPointData(player);
        sender.sendMessage(player.getName() + "の保有ポイント: " + playerPointData.getPoint(PointList.EVENT_POINT.getName()));
        return true;
    }
}
