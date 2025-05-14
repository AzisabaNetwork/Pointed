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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainPointedCommand implements CommandExecutor {
    Map<CommandSender, Boolean> areYouSure = new HashMap<>();
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
                }else if (args.length == 4) {
                    Player player = Bukkit.getPlayer(args[1]);
                    OfflinePlayer offlinePlayer;
                    if(player == null){
                        offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                        if(!offlinePlayer.hasPlayedBefore()){
                            sender.sendMessage("存在しないプレイヤーです");
                        }
                    }else {
                        offlinePlayer = player;
                    }
                    PlayerPointData playerPointData = PlayerPointDataIO.loadPlayerPointData(offlinePlayer);
                    Integer number = PointedUtilities.formStringToInt(args[3]);
                    if(number == null) {
                        number = 0;
                    }
                    switch (args[2]){
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
                boolean cURP = !Pointed.canUseRewardPage;

                if(cURP){
                    sender.sendMessage("リワードの受け取りを許可しました");
                }else {
                    sender.sendMessage("リワードの受け取りを無効化しました");
                }
                Pointed.canUseRewardPage = cURP;
                config.set(ConfigList.CANUSEREWARDPAGE.configName, cURP);
                Pointed.getInstance().saveConfig();

                break;
            case "toggleRanking":
                boolean useRanking = !Pointed.ranking;

                if(useRanking){
                    sender.sendMessage("ランキング機能を有効化しました");
                    new ClockMachine().rankingUpdaterTaskStarter();
                }else {
                    sender.sendMessage("ランキング機能を無効化しました");
                    for(BukkitTask task: Pointed.taskList){
                        task.cancel();
                    }
                }
                Pointed.ranking = useRanking;
                config.set(ConfigList.RANKING.configName, useRanking);
                Pointed.getInstance().saveConfig();
                break;
            case "reset":
                if(areYouSure.containsKey(sender)){
                    if(areYouSure.get(sender)){
                        PlayerPointDataIO.deleteYmlFiles(PlayerPointDataIO.playerPointDataFolder);
                    }else {
                        sender.sendMessage("PlayerPointDataを本当に削除してもいいですか？　Yesならもう一度コマンドを10秒以内に入力してください");
                    }
                }else {
                    areYouSure.put(sender, true);
                    sender.sendMessage("PlayerPointDataを本当に削除してもいいですか？　Yesならもう一度コマンドを10秒以内に入力してください");
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // ここに10秒後に行いたい処理を書くにゃ
                        areYouSure.remove(sender);
                        sender.sendMessage("10秒が経過したので削除を取り消します");
                    }
                }.runTaskLater(Pointed.instance, 20L * 10);
                break;
        }


        return false;
    }
}
