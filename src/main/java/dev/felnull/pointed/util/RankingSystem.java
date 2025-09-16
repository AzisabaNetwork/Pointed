package dev.felnull.pointed.util;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.database.dataio.PointTypeDao;
import dev.felnull.pointed.database.dataio.RankingDao;
import dev.felnull.pointed.database.dataio.RankingEntry;
import dev.felnull.pointed.database.dataio.SubjectRepository;
import dev.felnull.pointed.fileio.ConfigList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;


public class RankingSystem {
    public static volatile List<RankingEntry> allPlayerDataCache = new ArrayList<>();
    public static volatile Calendar allPlayerDataSetTime;

    private static final int TOP_LIMIT = 10;

    public static void getRankingList(Consumer<List<RankingEntry>> callback) {
        CompletableFuture<List<RankingEntry>> cf = CompletableFuture.supplyAsync(() -> {
            try {
                int pointTypeId = PointTypeDao.ensurePointType(PointList.EVENT_POINT.getName());
                List<RankingEntry> list = RankingDao.topNByTotal(pointTypeId, TOP_LIMIT); // 上位10だけ取得
                allPlayerDataCache = list;
                allPlayerDataSetTime = Calendar.getInstance();
                return list;
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.<RankingEntry>emptyList();
            }
        });

        cf.thenAccept(new Consumer<List<RankingEntry>>() {
            @Override public void accept(final List<RankingEntry> list) {
                Bukkit.getScheduler().runTask(Pointed.getInstance(), new Runnable() {
                    @Override public void run() { callback.accept(list); }
                });
            }
        });
    }
    public static void getMyRanking(final Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(Pointed.getInstance(), new Runnable() {
            @Override public void run() {
                try {
                    long sid = SubjectRepository.ensurePlayer(player.getUniqueId(), player.getName());
                    int pointTypeId = PointTypeDao.ensurePointType(Pointed.getInstance().getConfig().getString(ConfigList.VIEW_RANKING.configName));
                    int[] rt = RankingDao.myRankAndTotal(sid, pointTypeId); // [0]=rank, [1]=total
                    final int rank = rt[0];
                    final int total = rt[1];

                    Bukkit.getScheduler().runTask(Pointed.getInstance(), new Runnable() {
                        @Override public void run() {
                            player.sendMessage("現在の順位: " + rank + " 累計ポイント: " + total);
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f&l---------------------------"));
                        }
                    });
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void displayRanking(final Player onlinePlayer) {
        // キャッシュが無ければ更新してから表示
        if (allPlayerDataCache.isEmpty()) {
            getRankingList(new Consumer<List<RankingEntry>>() {
                @Override public void accept(List<RankingEntry> list) { displayRanking(onlinePlayer); }
            });
            return;
        }

        onlinePlayer.sendMessage(cc("&f&l--------[&b&l" + Pointed.getInstance().getConfig().getString(ConfigList.VIEW_RANKING.configName) + "ランキング&f&l]--------"));

        int rank = 1;
        for (RankingEntry e : allPlayerDataCache) {
            if (rank > TOP_LIMIT) break; // 上位10まで
            String name = e.name() != null ? e.name() : resolveName(e.playerUuid());
            onlinePlayer.sendMessage(String.format("第%d位: %-12s- 累計ポイント数:%-3d", rank, name, e.total()));
            rank++;
        }

        // 自分の正確な順位はDBで算出（TOP10内でも表示してOKならそのまま出す）
        Bukkit.getScheduler().runTaskAsynchronously(Pointed.getInstance(), new Runnable() {
            @Override public void run() {
                try {
                    long sid = SubjectRepository.ensurePlayer(onlinePlayer.getUniqueId(), onlinePlayer.getName());
                    int pointTypeId = PointTypeDao.ensurePointType(PointList.EVENT_POINT.getName());
                    final int[] rt = RankingDao.myRankAndTotal(sid, pointTypeId);
                    final int myRank = rt[0];
                    final int myTotal = rt[1];

                    Bukkit.getScheduler().runTask(Pointed.getInstance(), new Runnable() {
                        @Override public void run() {
                            onlinePlayer.sendMessage(cc("&f&l---------------------------"));
                            onlinePlayer.sendMessage(String.format("第%d位: %-12s- 累計ポイント数:%-3d",
                                    myRank, onlinePlayer.getName(), myTotal));
                            onlinePlayer.sendMessage(cc("&f&l---------------------------"));
                            if (allPlayerDataSetTime != null) {
                                onlinePlayer.sendMessage(cc("&f" + allPlayerDataSetTime.get(Calendar.HOUR_OF_DAY)
                                        + "&f時" + allPlayerDataSetTime.get(Calendar.MINUTE) + "&f分更新"));
                                onlinePlayer.sendMessage(cc("&f&l---------------------------"));
                            }
                        }
                    });
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void broadcastRanking() {
        if (allPlayerDataCache.isEmpty()) {
            getRankingList(new Consumer<List<RankingEntry>>() {
                @Override public void accept(List<RankingEntry> list) { broadcastRanking(); }
            });
            return;
        }

        Bukkit.broadcastMessage(cc("&f&l--------[&b&l" + Pointed.getInstance().getConfig().getString(ConfigList.VIEW_RANKING.configName) + "ランキング&f&l]--------"));
        int rank = 1;
        for (RankingEntry e : allPlayerDataCache) {
            if (rank > TOP_LIMIT) break;
            String name = e.name() != null ? e.name() : resolveName(e.playerUuid());
            Bukkit.broadcastMessage(String.format("第%d位: %-12s- 累計ポイント数:%-3d", rank, name, e.total()));
            rank++;
        }
        Bukkit.broadcastMessage(cc("&f&l---------------------------"));
        if (allPlayerDataSetTime != null) {
            Bukkit.broadcastMessage(cc("&f" + allPlayerDataSetTime.get(Calendar.HOUR_OF_DAY)
                    + "&f時" + allPlayerDataSetTime.get(Calendar.MINUTE) + "&f分更新"));
            Bukkit.broadcastMessage(cc("&f&l---------------------------"));
        }
    }

    private static String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static String resolveName(UUID uuid) {
        if (uuid == null) return "unknown";
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "unknown";
    }
}
