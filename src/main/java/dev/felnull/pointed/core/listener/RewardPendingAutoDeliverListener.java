package dev.felnull.pointed.core.listener;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.UUID;

public class RewardPendingAutoDeliverListener implements Listener {
    private final RewardAdminService admin;

    public RewardPendingAutoDeliverListener(RewardAdminService admin) {
        this.admin = admin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!Pointed.getInstance().getConfig().getBoolean("reward", false)) {
            return;
        }
        UUID uuid = e.getPlayer().getUniqueId();

        // 非同期で起点 → メインで claim 実行 → 結果を受け取る
        Bukkit.getScheduler().runTaskAsynchronously(Pointed.getInstance(), () -> {
            try {
                // ★ claimPendingRewards を必ずメインスレッドで実行
                int[] res = Bukkit.getScheduler()
                        .callSyncMethod(Pointed.getInstance(), () -> admin.claimPendingRewards(uuid))
                        .get(); // 非同期側で待つのでサーバ主スレッドはここでは止まらない

                int delivered = res[0], remaining = res[1];

                if (delivered > 0 || remaining > 0) {
                    // メッセージ送信もメインで
                    Bukkit.getScheduler().runTask(Pointed.getInstance(), () -> {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p == null || !p.isOnline()) return;

                        if (delivered > 0) {
                            p.sendMessage(Util.f("&a{0}件の報酬を受け取りました。", delivered));
                        }
                        if (remaining > 0) {
                            p.sendMessage(Util.f("&eインベントリの空きが足りません（保留 {0} 件）。空けて /reward claim を実行してください。", remaining));
                        }
                    });
                }
            } catch (Exception ex) {
                Bukkit.getLogger().warning("[Pointed] claim on login failed: " + ex.getMessage());
            }
        });
    }
}