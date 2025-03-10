package dev.felnull.pointed.util;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static dev.felnull.pointed.fileio.PlayerPointDataIO.playerPointDataFolder;

public class RankingSystem {
    public static List<Map.Entry<OfflinePlayer, PlayerPointData>> allPlayerDataCache = new ArrayList<>();
    public static Calendar allPlayerDataSetTime;

    // すべてのプレイヤーデータをロード
    private static List<Map.Entry<OfflinePlayer, PlayerPointData>> loadAllPlayerData() {
        List<Map.Entry<OfflinePlayer, PlayerPointData>> playerDataList = new ArrayList<>();
        File folder = playerPointDataFolder;
        List<UUID> uuids = new ArrayList<>();

        //対象のフォルダがない場合は生成
        PlayerPointDataIO.initSaveSettings(playerPointDataFolder);

        if(folder.listFiles() != null) {
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    try {
                        // ファイル名から拡張子を除いた部分をUUIDとしてパース
                        String fileName = file.getName().replace(".yml", "");
                        UUID uuid = UUID.fromString(fileName);
                        uuids.add(uuid);
                    } catch (IllegalArgumentException e) {
                        Bukkit.getLogger().warning(file.getName() + " はUUIDとして無効にゃ！");
                    } catch (Exception e) {
                        Bukkit.getLogger().warning(file.getName() + " の読み込み中にエラー発生にゃ〜: " + e.getMessage());
                    }
                }
            }
        }else {
            Bukkit.getLogger().info("PointDataが１つもないにゃ!");
        }

        // プレイヤーリストを取得（例: すべてのオフラインプレイヤー）
        for (UUID playerUUID : uuids) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
            PlayerPointData playerPointData = PlayerPointDataIO.loadPlayerPointData(player);  // データをロード
            playerDataList.add(new AbstractMap.SimpleEntry<>(player, playerPointData));
        }
        return playerDataList;
    }

    // ランキングを取得して表示
    public static void getRankingList(Consumer<List<Map.Entry<OfflinePlayer, PlayerPointData>>> callback) {
        // 非同期でデータ処理
        CompletableFuture.supplyAsync(() -> {
            //読み込み
            List<Map.Entry<OfflinePlayer, PlayerPointData>> playerDataList = loadAllPlayerData();

            // ソート
            playerDataList.sort((entry1, entry2) -> {
                int points1 = entry1.getValue().getTotalPoint(PointList.EVENT_POINT.getName());
                int points2 = entry2.getValue().getTotalPoint(PointList.EVENT_POINT.getName());
                return Integer.compare(points2, points1); // 降順にソート
            });
            allPlayerDataCache = playerDataList;
            allPlayerDataSetTime = Calendar.getInstance();
            return playerDataList; // ソート済みリストを返す
        }).thenAcceptAsync(callback);
    }

    public static void getMyRanking(Player player){
        if(allPlayerDataCache.isEmpty()){
            loadAllPlayerData();
        }
        int rank = 1;
        for (Map.Entry<OfflinePlayer, PlayerPointData> entry : allPlayerDataCache) {
            if(entry.getKey() == player){
                int totalPoints = entry.getValue().getTotalPoint(PointList.EVENT_POINT.getName());
                player.sendMessage("現在の順位: " + rank + " 累計戦果: " + String.valueOf(totalPoints));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
            }
            rank++;
        }
    }

    public static void displayRanking(Player onlinePlayer){
        // ランキングを表示（上位10人を表示など）
        int rank = 1;
        int myRank = 0;
        int myTotalPoint = 0;

        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l--------[&b&l戦果ランキング&f&l]--------"));

        for (Map.Entry<OfflinePlayer, PlayerPointData> entry : allPlayerDataCache) {
            OfflinePlayer player = entry.getKey();
            if(rank >= 8 && !(onlinePlayer.equals(player))){
                rank++;
                continue;
            }
            PlayerPointData playerPointData = entry.getValue();
            int totalPoints = playerPointData.getTotalPoint(PointList.EVENT_POINT.getName());

            if(onlinePlayer.equals(player) && rank >= 8){
                myRank = rank;
                myTotalPoint = totalPoints;
            }else {
                if(onlinePlayer.equals(player)){
                    myRank = rank;
                    myTotalPoint = totalPoints;
                }
                // ランキングをチャットに表示
                onlinePlayer.sendMessage(String.format("第" + rank + "位: %-12s- 累計戦果数:%-3s", player.getName(), totalPoints));
            }

            // ランキングの順位をインクリメント
            rank++;
        }
        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
        onlinePlayer.sendMessage(String.format("第" + myRank + "位: %-12s- 累計戦果数:%-3s", onlinePlayer.getName(), myTotalPoint));
        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&',   "&f" + allPlayerDataSetTime.get(Calendar.HOUR_OF_DAY) + "&f時" + allPlayerDataSetTime.get(Calendar.MINUTE) + "&f分更新"));
        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
    }

    public static void broadcastRanking(){
        // ランキングを表示（上位10人を表示など）
        int rank = 1;

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f&l--------[&b&l戦果ランキング&f&l]--------"));

        for (Map.Entry<OfflinePlayer, PlayerPointData> entry : allPlayerDataCache) {
            if(rank >= 8){
                break;
            }
            OfflinePlayer player = entry.getKey();
            PlayerPointData playerPointData = entry.getValue();
            int totalPoints = playerPointData.getTotalPoint(PointList.EVENT_POINT.getName());

            // ランキングをチャットに表示
            Bukkit.broadcastMessage(String.format("第" + rank + "位: %-12s- 累計戦果数:%-3s", player.getName(), totalPoints));

            // ランキングの順位をインクリメント
            rank++;
        }
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',   "&f" + allPlayerDataSetTime.get(Calendar.HOUR_OF_DAY) + "&f時" + allPlayerDataSetTime.get(Calendar.MINUTE) + "&f分更新"));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
    }
}
