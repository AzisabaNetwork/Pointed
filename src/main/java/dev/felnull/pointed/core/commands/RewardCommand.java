package dev.felnull.pointed.core.commands;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class RewardCommand implements CommandExecutor {
    private final RewardAdminService admin;

    public RewardCommand(RewardAdminService admin) {
        this.admin = admin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Player only"); return true; }
        if(!(Pointed.getInstance().getConfig().getBoolean("reward", false))){
            sender.sendMessage("ロビー鯖でのみ有効です");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("claim")) {
            Bukkit.getScheduler().runTaskAsynchronously(Pointed.getInstance(), () -> {
                try {
                    // メインスレッドで claimPendingRewards を実行して結果を受け取る
                    int[] res = Bukkit.getScheduler()
                            .callSyncMethod(Pointed.getInstance(), () -> admin.claimPendingRewards(p.getUniqueId()))
                            .get(); // ここは今の非同期スレッドで待つのでサーバはブロックしない

                    int delivered = res[0], remaining = res[1];
                    // プレイヤーへのメッセージ送信はメインに戻す
                    Bukkit.getScheduler().runTask(Pointed.getInstance(), () -> {
                        if (delivered == 0 && remaining == 0) {
                            p.sendMessage(Util.f("&7受け取れる報酬はありません。"));
                        } else {
                            if (delivered > 0) p.sendMessage(Util.f("&a{0}件受け取り完了。", delivered));
                            if (remaining > 0) p.sendMessage(Util.f("&e空き不足で {0} 件保留です。", remaining));
                        }
                    });
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Pointed.getInstance(), () ->
                            p.sendMessage(Util.f("&cエラー: {0}", e.getMessage())));
                }
            });
            return true;
        }
        // ヘルプなど
        sender.sendMessage("/reward claim");
        return true;
    }
}