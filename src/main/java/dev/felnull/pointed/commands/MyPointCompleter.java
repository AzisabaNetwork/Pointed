package dev.felnull.pointed.commands;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.fileio.RewardDataIO;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyPointCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("pointed")){
            if(args.length == 1){
                for(PointList pointList : PointList.values()){
                    suggestions.add(pointList.getAllies());
                }
            }
        }
        return suggestions;
    }
}
