package dev.felnull.pointed.core.listener;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import org.bukkit.Bukkit;
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
        if(!(Pointed.getInstance().getConfig().getBoolean("reward", false))){
            return;
        }
        UUID uuid = e.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(Pointed.getInstance(), () -> {
            try {
                int[] res = admin.claimPendingRewards(uuid);
                int delivered = res[0], remaining = res[1];
                if (delivered > 0 || remaining > 0) {
                    Bukkit.getScheduler().runTask(Pointed.getInstance(), () -> {
                        if (delivered > 0) e.getPlayer().sendMessage(Util.f("&a{0}件の報酬を受け取りました。", delivered));
                        if (remaining > 0) e.getPlayer().sendMessage(Util.f("&eインベントリの空きが足りません（保留 {0} 件）。空けて /reward claim を実行してください。", remaining));
                    });
                }
            } catch (SQLException ex) {
                Bukkit.getLogger().warning("[Pointed] claim on login failed: " + ex.getMessage());
            }
        });
    }
}