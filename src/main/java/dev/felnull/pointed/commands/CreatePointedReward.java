package dev.felnull.pointed.commands;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.fileio.RewardDataIO;
import dev.felnull.pointed.gui.page.RewardSettingsGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreatePointedReward implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof ConsoleCommandSender){
            Bukkit.getLogger().warning("このコマンドはプレイヤー専用です!");
            return false;
        }

        InventoryGUI gui = new InventoryGUI((Player) sender);
        switch (args[0]){

            case "create":
                if(args[1].isEmpty()) {
                    gui.openPage(new RewardSettingsGUI(gui));
                }else {

                    int rewardID;
                    try{
                        rewardID = Integer.parseInt(args[1].replaceAll("[^0-9]", ""));
                    } catch (NumberFormatException e) {
                        sender.sendMessage("数字を入力してください!!");
                        break;
                    }

                    List<RewardData> rewardDataList = RewardDataIO.loadRewards();
                    for(RewardData rewardData : rewardDataList){
                        if(rewardData.rewardID == rewardID){
                            gui.openPage(new RewardSettingsGUI(gui, rewardData));
                            break;
                        }
                    }
                }
                break;
        }


        return false;
    }
}
