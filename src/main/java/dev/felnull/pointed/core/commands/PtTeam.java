package dev.felnull.pointed.core.commands;

import dev.felnull.bettergui.core.InventoryGUI;
import dev.felnull.pointed.core.database.api.PointService;
import dev.felnull.pointed.core.database.data.RankRow;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.gui.page.TeamConfigGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class PtTeam implements CommandExecutor, TabCompleter {
    private final Plugin plugin;

    public PtTeam(Plugin plugin) {
        this.plugin = plugin;
    }

    // type=PLAYER のときだけ <名前 or UUID> を UUID文字列に正規化して返す
    // それ以外の type は key をそのまま返す
    private static String resolveSubjectKey(String type, String keyOrName) {
        if (!"PLAYER".equalsIgnoreCase(type)) {
            return keyOrName; // 任意文字列キーをそのまま使用
        }
        // PLAYER の場合: UUIDっぽければそのまま、そうでなければ名前→UUID
        try {
            UUID uuid = UUID.fromString(keyOrName);
            return uuid.toString();
        } catch (IllegalArgumentException ignore) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(keyOrName);
            UUID uuid = op.getUniqueId();
            return uuid.toString();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Util.f("&e/ptteam <set> <scope> <teamID>"));
            return true;
        }

        if (!sender.hasPermission("pointed.admin")) {
            sender.sendMessage(Util.f("&c権限がありません。"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "set" -> {
                if(args.length < 2){
                    sender.sendMessage(Util.f("&e/ptteam <set> <teamID>"));
                    return true;
                }
                if(!(sender instanceof Player)){
                    sender.sendMessage(Util.f("Playerのみが使用可能なコマンドです"));
                    return true;
                }
                InventoryGUI gui = new InventoryGUI((Player) sender);
                gui.openPage(new TeamConfigGUI(gui, args[1]));
            }
            default -> sender.sendMessage(Util.f("/ptteam <set> <teamID>"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("set");
        }
        // サブコマンド毎の type 補完
        if (args.length == 2 && List.of("getnow","gettotal","get","add","sub","set").contains(args[0].toLowerCase())) {
            return List.of("PLAYER","TEAM","SYSTEM");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("rank")) {
            return List.of("daily", "weekly", "global");
        }
        return List.of();
    }

    // 共通メソッド
    private String formatRankLine(int rankNum, RankRow r) {
        String color = switch (rankNum) {
            case 1 -> "&6"; // 金
            case 2 -> "&7"; // 銀
            case 3 -> "&c"; // 赤
            default -> "&e"; // 黄
        };
        return Util.f(color + "#{0} &b{1} &f+{2}", rankNum, r.name, r.gained);
    }
}