package dev.felnull.pointed.core.commands;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.core.database.api.PointServiceImpl;
import dev.felnull.pointed.core.database.data.RankRow;
import dev.felnull.pointed.core.database.api.PointService;
import dev.felnull.pointed.core.util.Util;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class PtCommand implements CommandExecutor, TabCompleter {
    private final PointService svc;
    private final ZoneId zoneId;

    public PtCommand(PointService svc, ZoneId zoneId) {
        this.svc = svc;
        this.zoneId = zoneId;
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Util.f("&e/pt <getnow|gettotal|get|add|sub|set|rank>"));
            return true;
        }

        if (!sender.hasPermission("pointed.admin")) {
            sender.sendMessage(Util.f("&c権限がありません。"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "getnow" -> {
                // /pt getnow <type> <key|playerName|uuid> <scope>
                if (args.length < 4) {
                    sender.sendMessage(Util.f("&e/pt getnow <タイプ> <キー|プレイヤー名|UUID> <スコープ>"));
                    return true;
                }
                String type = args[1];
                String subjectKey = resolveSubjectKey(type, args[2]);
                String scope = args[3];
                long v = svc.getNowPoint(type, subjectKey, scope);
                sender.sendMessage(Util.f("&a現在ポイント &7({0}): &b{1}", scope, v));
            }
            case "gettotal" -> {
                // /pt gettotal <type> <key|playerName|uuid> <scope>
                if (args.length < 4) {
                    sender.sendMessage(Util.f("&e/pt gettotal <タイプ> <キー|プレイヤー名|UUID> <スコープ>"));
                    return true;
                }
                String type = args[1];
                String subjectKey = resolveSubjectKey(type, args[2]);
                String scope = args[3];
                long v = svc.getTotalPoint(type, subjectKey, scope);
                sender.sendMessage(Util.f("&a累計獲得ポイント &7({0}): &b{1}", scope, v));
            }
            case "get" -> {
                // /pt get <type> <key|playerName|uuid> <scope>
                if (args.length < 4) {
                    sender.sendMessage(Util.f("&e/pt get <タイプ> <キー|プレイヤー名|UUID> <スコープ>"));
                    return true;
                }
                String type = args[1];
                String subjectKey = resolveSubjectKey(type, args[2]);
                String scope = args[3];
                long[] v = svc.getNowAndTotal(type, subjectKey, scope);
                sender.sendMessage(Util.f("&a現在/累計 &7({0}): &b{1} / {2}", scope, v[0], v[1]));
            }
            case "add" -> {
                // /pt add <type> <key|playerName|uuid> <scope> <amount>
                if (!sender.hasPermission("pointed.admin")) {
                    sender.sendMessage(Util.f("&c権限がありません。"));
                    return true;
                }
                if (args.length < 5) {
                    sender.sendMessage(Util.f("&e/pt add <タイプ> <キー|プレイヤー名|UUID> <スコープ> <追加量>"));
                    return true;
                }
                String type = args[1];
                String subjectKey = resolveSubjectKey(type, args[2]);
                String scope = args[3];
                long amt = Long.parseLong(args[4]);
                // 表示名: PLAYER のときは元の引数（名前かUUID文字列）を name に入れておく
                String nameForDisplay = args[2];
                svc.ensureAccount(type, subjectKey, scope, nameForDisplay);
                long[] v = svc.add(type, subjectKey, scope, amt);
                sender.sendMessage(Util.f("&a{0}に{1} ポイントを追加しました。 &7({2}) &r現在/累計: &b{3} / {4}",
                        nameForDisplay, amt, scope, v[0], v[1]));
            }
            case "sub" -> {
                // /pt sub <type> <key|playerName|uuid> <scope> <amount>
                if (!sender.hasPermission("pointed.admin")) {
                    sender.sendMessage(Util.f("&c権限がありません。"));
                    return true;
                }
                if (args.length < 5) {
                    sender.sendMessage(Util.f("&e/pt sub <タイプ> <キー|プレイヤー名|UUID> <スコープ> <減算量>"));
                    return true;
                }
                String type = args[1];
                String subjectKey = resolveSubjectKey(type, args[2]);
                String scope = args[3];
                long amt = Long.parseLong(args[4]);
                svc.ensureAccount(type, subjectKey, scope, args[2]);
                boolean ok = svc.subtract(type, subjectKey, scope, amt);
                sender.sendMessage(ok
                        ? Util.f("&a{0}の{1} ポイントを消費しました。 &7({2})", args[2], amt, scope)
                        : Util.f("&c残高不足で処理できません。 &7({0})", scope));
            }
            case "set" -> {
                // /pt set <type> <key|playerName|uuid> <scope> <newNow>
                if (!sender.hasPermission("pointed.admin")) {
                    sender.sendMessage(Util.f("&c権限がありません。"));
                    return true;
                }
                if (args.length < 5) {
                    sender.sendMessage(Util.f("&e/pt set <タイプ> <キー|プレイヤー名|UUID> <スコープ> <新しい残高>"));
                    return true;
                }
                String type = args[1];
                String subjectKey = resolveSubjectKey(type, args[2]);
                String scope = args[3];
                long nv = Long.parseLong(args[4]);
                svc.ensureAccount(type, subjectKey, scope, args[2]);
                long[] v = svc.set(type, subjectKey, scope, nv);
                sender.sendMessage(Util.f("&a{0}の残高を {1} に設定しました。 &7({2}) &r現在/累計: &b{3} / {4}",
                        args[2],nv, scope, v[0], v[1]));
            }
            case "rank" -> {
                if (args.length < 4) {
                    sender.sendMessage(Util.f("&e/pt rank <daily|weekly|global> <PLAYER|TEAM|SYSTEM> <スコープ> [...]"));
                    return true;
                }
                String mode = args[1].toLowerCase();
                String subjectType = args[2].toUpperCase();
                String scope = args[3];

                java.util.function.BiFunction<Integer,Integer,Integer> calcFetch = (base, extra) -> Math.max(1, base + extra);

                switch (mode) {
                    case "daily" -> {
                        LocalDate day = (args.length >= 5) ? LocalDate.parse(args[4]) : LocalDate.now(zoneId);
                        int limit = (args.length >= 6) ? Integer.parseInt(args[5]) : 10;
                        int fetchLimit = calcFetch.apply(limit, 100);
                        List<RankRow> rows = svc.getDailyTop(subjectType, scope, day, fetchLimit);

                        sender.sendMessage(Util.f("&6&lデイリーランキング &7({0} / {1})", scope, day));

                        long prevGained = Long.MIN_VALUE;
                        int shown = 0;
                        int rankNum = 0;

                        for (RankRow r : rows) {
                            if (prevGained != r.gained) {
                                rankNum = shown + 1;
                                prevGained = r.gained;
                            }
                            sender.sendMessage(formatRankLine(rankNum, r));
                            shown++;
                        }
                    }

                    case "weekly" -> {
                        LocalDate end = (args.length >= 6) ? LocalDate.parse(args[5]) : LocalDate.now(zoneId);
                        LocalDate start = (args.length >= 5) ? LocalDate.parse(args[4]) : end.minusDays(6);
                        int limit = (args.length >= 7) ? Integer.parseInt(args[6]) : 10;
                        int fetchLimit = calcFetch.apply(limit, 100);
                        List<RankRow> rows = svc.getWeeklyTop(subjectType, scope, start, end, fetchLimit);

                        sender.sendMessage(Util.f("&d&lウィークリーランキング &7({0} ~ {1} / {2})", start, end, scope));

                        long prevGained = Long.MIN_VALUE;
                        int shown = 0;
                        int rankNum = 0;

                        for (RankRow r : rows) {
                            if (prevGained != r.gained) {
                                if (shown >= limit) break;
                                rankNum = shown + 1;
                                prevGained = r.gained;
                            }
                            sender.sendMessage(formatRankLine(rankNum, r));
                            shown++;
                        }
                    }

                    case "global" -> {
                        int limit = (args.length >= 5) ? Integer.parseInt(args[4]) : 10;
                        int fetchLimit = calcFetch.apply(limit, 100);
                        List<RankRow> rows = svc.getGlobalTop(subjectType, scope, fetchLimit);

                        sender.sendMessage(Util.f("&b&l全期間ランキング &7({0})", scope));

                        long prevGained = Long.MIN_VALUE;
                        int shown = 0;
                        int rankNum = 0;

                        for (RankRow r : rows) {
                            if (prevGained != r.gained) {
                                if (shown >= limit) break;
                                rankNum = shown + 1;
                                prevGained = r.gained;
                            }
                            sender.sendMessage(formatRankLine(rankNum, r));
                            shown++;
                        }
                    }
                    default -> sender.sendMessage(Util.f("&e/pt rank <daily|weekly|global> <PLAYER|TEAM|SYSTEM> <スコープ> [...]"));
                }
            }
            case "scope" -> {
                PointServiceImpl pointService = Pointed.getInstance().getPointService();
                sender.sendMessage("====Scope一覧====");
                for(String scope : pointService.listAllScopes()){
                    sender.sendMessage("・ " + scope);
                }
                sender.sendMessage("=================");
            }

        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of("getnow", "gettotal", "get", "add", "sub", "set", "rank", "scope");
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