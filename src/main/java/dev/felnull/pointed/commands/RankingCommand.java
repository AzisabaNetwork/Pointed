package dev.felnull.pointed.commands;

import dev.felnull.pointed.util.RankingSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RankingCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof ConsoleCommandSender){
            sender.sendMessage("このコマンドはプレイヤー専用です");
            return true;
        }
        RankingSystem.displayRanking((Player) sender);
        return true;
    }
}
