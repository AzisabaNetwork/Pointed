package dev.felnull.pointed.util;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RankingSystem {
    public static List<Map.Entry<OfflinePlayer, PlayerPointData>> allPlayerDataCache = new ArrayList<>();
    public static Calendar allPlayerDataSetTime;

    // すべてのプレイヤーデータをロード
    private static List<Map.Entry<OfflinePlayer, PlayerPointData>> loadAllPlayerData() {
        List<Map.Entry<OfflinePlayer, PlayerPointData>> playerDataList = new ArrayList<>();

        // プレイヤーリストを取得（例: すべてのオフラインプレイヤー）
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerPointData playerPointData = PlayerPointDataIO.loadPlayerPointData(player);  // データをロード
            playerDataList.add(new AbstractMap.SimpleEntry<>(player, playerPointData));
        }
        return playerDataList;
    }

    // ランキングを取得して表示
    public static List<Map.Entry<OfflinePlayer, PlayerPointData>> getRankingList() {

        // 非同期でデータ処理
        CompletableFuture<List<Map.Entry<OfflinePlayer, PlayerPointData>>> future = CompletableFuture.supplyAsync(() -> {
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
        });
        try {
            // 非同期処理の完了を待機し、結果を取得
            return future.get(); // get()で結果を取得（同期的に待機）
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Collections.emptyList(); // エラー時は空のリストを返す
        }
    }

    public static void getMyRanking(Player player){
        if(allPlayerDataCache.isEmpty()){
            loadAllPlayerData();
        }
        for (Map.Entry<OfflinePlayer, PlayerPointData> entry : allPlayerDataCache) {
            
        }

    }

    public static void displayRanking(Player onlinePlayer){
        // ランキングを表示（上位10人を表示など）
        int rank = 1;

        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l--------[&b&l戦果ランキング&f&l]--------"));

        for (Map.Entry<OfflinePlayer, PlayerPointData> entry : allPlayerDataCache) {
            if(rank >= 8){
                break;
            }
            OfflinePlayer player = entry.getKey();
            PlayerPointData playerPointData = entry.getValue();
            int totalPoints = playerPointData.getTotalPoint(PointList.EVENT_POINT.getName());

            // ランキングをチャットに表示
            onlinePlayer.sendMessage(String.format("第%1d 位: %-12s- 累計戦果数:%-3s", rank, player.getName(), totalPoints));

            // ランキングの順位をインクリメント
            rank++;
        }
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
            Bukkit.broadcastMessage(String.format("第%1d 位: %-12s- 累計戦果数:%-3s", rank, player.getName(), totalPoints));

            // ランキングの順位をインクリメント
            rank++;
        }
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',   "&f" + allPlayerDataSetTime.get(Calendar.HOUR_OF_DAY) + "&f時" + allPlayerDataSetTime.get(Calendar.MINUTE) + "&f分更新"));
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
    }
}
