package dev.felnull.pointed.util;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.UUID;

public class Util {
    // メッセージをフォーマットして、&で色をつける
    public static String f(String text, Object... args) {
        return MessageFormat.format(ChatColor.translateAlternateColorCodes('&', text), args);
    }

    // 色を消す
    public static String r(String text) {
        return ChatColor.stripColor(text);
    }

    public static Component c(String text, Object... args){
        return Component.text( f(text, args));
    }
}
