package dev.felnull.pointed.core.util;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;

public class ColorUtil {
    private static final Map<Character, NamedTextColor> LEGACY_TO_NAMED = new HashMap<>();
    private static final Map<NamedTextColor, String> NAMED_TO_LEGACY = new HashMap<>();
    static {
        LEGACY_TO_NAMED.put('0', NamedTextColor.BLACK);
        LEGACY_TO_NAMED.put('1', NamedTextColor.DARK_BLUE);
        LEGACY_TO_NAMED.put('2', NamedTextColor.DARK_GREEN);
        LEGACY_TO_NAMED.put('3', NamedTextColor.DARK_AQUA);
        LEGACY_TO_NAMED.put('4', NamedTextColor.DARK_RED);
        LEGACY_TO_NAMED.put('5', NamedTextColor.DARK_PURPLE);
        LEGACY_TO_NAMED.put('6', NamedTextColor.GOLD);
        LEGACY_TO_NAMED.put('7', NamedTextColor.GRAY);
        LEGACY_TO_NAMED.put('8', NamedTextColor.DARK_GRAY);
        LEGACY_TO_NAMED.put('9', NamedTextColor.BLUE);
        LEGACY_TO_NAMED.put('a', NamedTextColor.GREEN);
        LEGACY_TO_NAMED.put('b', NamedTextColor.AQUA);
        LEGACY_TO_NAMED.put('c', NamedTextColor.RED);
        LEGACY_TO_NAMED.put('d', NamedTextColor.LIGHT_PURPLE);
        LEGACY_TO_NAMED.put('e', NamedTextColor.YELLOW);
        LEGACY_TO_NAMED.put('f', NamedTextColor.WHITE);

        // 逆引きマップを自動で生成
        for (var entry : LEGACY_TO_NAMED.entrySet()) {
            NAMED_TO_LEGACY.put(entry.getValue(), "&" + entry.getKey());
        }
    }

    public static NamedTextColor fromLegacyCode(String legacy) {
        if (legacy == null || legacy.length() < 2 || legacy.charAt(0) != '&') return null;
        char code = Character.toLowerCase(legacy.charAt(1));
        return LEGACY_TO_NAMED.get(code);
    }

    public static String toLegacyCode(NamedTextColor color) {
        return NAMED_TO_LEGACY.getOrDefault(color, "&f");
    }
}