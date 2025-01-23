package dev.felnull.pointed.util;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.felnull.pointed.fileio.PlayerPointDataIO.loadPlayerPointData;

public class RankingSystem {
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
        // 非同期でデータをロードし、ソート
        CompletableFuture<List<Map.Entry<OfflinePlayer, PlayerPointData>>> future = CompletableFuture.supplyAsync(() -> {
            // データをロード
            List<Map.Entry<OfflinePlayer, PlayerPointData>> playerDataList = RankingSystem.loadAllPlayerData();

            // ソート
            playerDataList.sort((entry1, entry2) -> {
                int points1 = entry1.getValue().getTotalPoint(PointList.EVENT_POINT.getName());
                int points2 = entry2.getValue().getTotalPoint(PointList.EVENT_POINT.getName());
                return Integer.compare(points2, points1); // 降順にソート
            });

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

    public static void displayRanking(Player onlinePlayer){
        // ランキングを表示（上位10人を表示など）
        int rank = 1;

        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l--------[&b&l戦果ランキング&f&l]--------"));

        for (Map.Entry<OfflinePlayer, PlayerPointData> entry : getRankingList()) {
            if(rank >= 8){
                break;
            }
            OfflinePlayer player = entry.getKey();
            PlayerPointData playerPointData = entry.getValue();
            int totalPoints = playerPointData.getTotalPoint(PointList.EVENT_POINT.getName());

            // ランキングをチャットに表示
            onlinePlayer.sendMessage(String.format("第%2d位: %-12s- 累計戦果数:%-3s", rank, player.getName(), totalPoints));

            // ランキングの順位をインクリメント
            rank++;
        }
        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
    }
}
