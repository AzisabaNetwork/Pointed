package dev.felnull.pointed.commands;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.PointList;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.data.RewardData;
import dev.felnull.pointed.fileio.ConfigList;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import dev.felnull.pointed.fileio.RewardDataIO;
import dev.felnull.pointed.gui.page.RewardSettingsGUI;
import dev.felnull.pointed.task.ClockMachine;
import dev.felnull.pointed.util.PointedUtilities;
import dev.felnull.pointed.util.RankingSystem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MainPointedCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {


        if(args.length == 0){
            return true;
        }
        FileConfiguration config = Pointed.getInstance().getConfig();
        switch (args[0]){

            case "create":
                if(sender instanceof ConsoleCommandSender){
                    Bukkit.getLogger().warning("このコマンドはプレイヤー専用です!");
                    return false;
                }
                InventoryGUI gui = new InventoryGUI((Player) sender);
                if(args.length == 1) {
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
            case "point":
                if(args.length == 1) {
                    sender.sendMessage("プレイヤー名を指定してください");
                }else if (args.length == 5) {
                    PointList pointList;
                    try {
                        pointList = PointList.fromAllies(args[1]);
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(e.getMessage());
                        return true;
                    }
                    Player player = Bukkit.getPlayer(args[2]);
                    OfflinePlayer offlinePlayer;
                    if (player == null) {
                        offlinePlayer = Bukkit.getOfflinePlayer(args[2]);
                        if (!offlinePlayer.hasPlayedBefore()) {
                            sender.sendMessage("存在しないプレイヤーです");
                        }
                    } else {
                        offlinePlayer = player;
                    }
                    PlayerPointData playerPointData = PlayerPointDataIO.loadPlayerPointData(offlinePlayer, pointList);
                    Integer number = PointedUtilities.formStringToInt(args[4]);
                    if (number == null) {
                        number = 0;
                    }
                    switch (args[3]) {
                        case "add":
                            playerPointData.addPoint(PointList.EVENT_POINT.getName(), number);
                            sender.sendMessage(offlinePlayer.getName() + "のポイントを" + number + "追加しました");
                            break;
                        case "subtract":
                            playerPointData.subtractPoint(PointList.EVENT_POINT.getName(), number);
                            sender.sendMessage(offlinePlayer.getName() + "のポイントを" + number + "削除しました");
                            break;
                        case "set":
                            playerPointData.setPoint(PointList.EVENT_POINT.getName(), number);
                            sender.sendMessage(offlinePlayer.getName() + "のポイントを" + number + "に設定しました");
                            break;
                        default:
                            sender.sendMessage("add,subtract,setのどれかを選んでください");
                            break;
                    }
                    PlayerPointDataIO.savePlayerPointData(playerPointData);

                }else {
                    sender.sendMessage("{/pointed point プレイヤー名 [add,subtract,set] 数字}で入力してください");
                }
                break;
            case "debug":
                if(args.length == 1){
                    Player player = (Player) sender;
                    RankingSystem.getRankingList(ranking -> {RankingSystem.broadcastRanking();});

                }
                break;
            case "toggle":
                if(args.length == 1){
                    sender.sendMessage("変更したいポイント名を入力してください");
                }else {
                    PointList pointList;
                    try {
                        pointList = PointList.fromAllies(args[1]);
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(e.getMessage());
                        return true;
                    }
                    boolean cURP = !Pointed.canUseRewardPage.get(pointList);

                    if(cURP){
                        sender.sendMessage(pointList.getAllies() + "のリワードの受け取りを許可しました");
                    }else {
                        sender.sendMessage(pointList.getAllies() + "のリワードの受け取りを無効化しました");
                    }
                    Pointed.canUseRewardPage.put(pointList,cURP);
                    config.set(ConfigList.CANUSEREWARDPAGE.configName + "." + pointList.getName(), cURP);
                    Pointed.getInstance().saveConfig();
                }

                break;
            case "toggleRanking":
                if(args.length == 1){
                    sender.sendMessage("変更したいポイント名を入力してください");
                }else {
                    PointList pointList;
                    try {
                        pointList = PointList.fromAllies(args[1]);
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(e.getMessage());
                        return true;
                    }
                    boolean useRanking = !Pointed.ranking.get(pointList);

                    if (useRanking) {
                        sender.sendMessage(pointList.getAllies() + "のランキング機能を有効化しました");
                        new ClockMachine().rankingUpdaterTaskStarter();
                    } else {
                        sender.sendMessage(pointList.getAllies() + "のランキング機能を無効化しました");
                        for (BukkitTask task : Pointed.taskList) {
                            task.cancel();
                        }
                    }
                    Pointed.ranking.put(pointList, useRanking);
                    config.set(ConfigList.RANKING.configName + "." + pointList.getName(), useRanking);
                    Pointed.getInstance().saveConfig();
                }
        }


        return false;
    }
}
