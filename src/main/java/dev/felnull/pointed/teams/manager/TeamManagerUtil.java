package dev.felnull.pointed.teams.manager;

import dev.felnull.pointed.core.util.Util;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeamManagerUtil {
    // "▌"　"■"
    private static final String BLOCK = "▌";

    /**
     * チャット用の割合バーを返す（色付き）
     *
     * @param points チーム名 -> ポイント
     * @param width  バー全体の長さ（推奨 20〜40）
     * @param colorByTeam チーム名 -> "&c" や "&a" などのカラーコード
     * @param order 表示順。nullならキーで昇順
     */
    public static String renderBar(Map<String, Long> points, int width,
                                   Map<String, String> colorByTeam,
                                   List<String> order) {
        if (width < 1) width = 1;

        long sum = points.values().stream().mapToLong(Long::longValue).sum();
        if (sum <= 0) return Util.f("&7[&8" + BLOCK.repeat(width) + "&7]");

        List<Map.Entry<String, Long>> list = new ArrayList<>();
        if (order != null && !order.isEmpty()) {
            for (String k : order) {
                if (points.containsKey(k)) list.add(Map.entry(k, points.get(k)));
            }
            for (var e : points.entrySet()) {
                if (!order.contains(e.getKey())) list.add(e);
            }
        } else {
            list.addAll(points.entrySet());
            list.sort(Map.Entry.comparingByKey());
        }

        StringBuilder sb = new StringBuilder("&7[");
        int used = 0;
        int center = width / 2;

        for (int i = 0; i < list.size(); i++) {
            var e = list.get(i);
            double ratio = (double) e.getValue() / sum;
            int len = (i == list.size() - 1)
                    ? (width - used)
                    : Math.max(0, (int) Math.round(ratio * width));
            used += len;
            if (len <= 0) continue;

            String color = colorByTeam.getOrDefault(e.getKey(), "&f");
            for (int j = 0; j < len; j++) {
                int pos = used - len + j;
                if (pos == center) {
                    sb.append("&7|"); // 中央線
                } else {
                    sb.append(color).append(BLOCK);
                }
            }
        }
        sb.append("&7]");
        return Util.f(sb.toString());
    }

    /**
     * 凡例（チームごとの割合）
     */
    public static String renderHanrei(Map<String, Long> points,
                                      Map<String, String> colorByTeam,
                                      List<String> order) {
        long sum = points.values().stream().mapToLong(Long::longValue).sum();
        if (sum <= 0) return Util.f("&7(no data)");

        List<String> parts = new ArrayList<>();
        Iterable<String> keys = (order != null && !order.isEmpty())
                ? order
                : points.keySet().stream().sorted().toList();

        for (String team : keys) {
            if (!points.containsKey(team)) continue;
            long v = points.get(team);
            double pct = 100.0 * v / sum;
            String color = colorByTeam.getOrDefault(team, "&f");
            parts.add(color + team + " " + Math.round(pct) + "%&7");
        }
        return Util.f(String.join("  ", parts));
    }

    /** Component 版 */
    public static Component renderBarComponent(Map<String, Long> points, int width,
                                               Map<String, String> colorByTeam,
                                               List<String> order) {
        return Util.c(renderBar(points, width, colorByTeam, order));
    }

    public static Component renderHanreiComponent(Map<String, Long> points,
                                                  Map<String, String> colorByTeam,
                                                  List<String> order) {
        return Util.c(renderHanrei(points, colorByTeam, order));
    }
}
