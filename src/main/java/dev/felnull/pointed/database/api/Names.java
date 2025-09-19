package dev.felnull.pointed.database.api;

public final class Names {
    private static String PREFIX = "";

    public static void init(String prefix) {
        PREFIX = (prefix == null) ? "" : prefix;
    }

    /** 物理テーブル名（バッククォート付き） */
    public static String t(String base) {
        return "`" + PREFIX + base + "`";
    }

    /** インデックス/制約名にもプレフィックスを付ける */
    public static String idx(String base) {
        return PREFIX + base;
    }

    /** information_schema 用にバッククォートを外す */
    public static String unquote(String x) {
        if (x == null) return null;
        if (x.length() >= 2 && x.startsWith("`") && x.endsWith("`")) {
            return x.substring(1, x.length() - 1);
        }
        return x;
    }

    /** 物理テーブル名（バッククォートなし文字列） */
    public static String phys(String base) {
        return PREFIX + base;
    }

    private Names() {}
}