package dev.felnull.pointed.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PointedUtilities {

    public static Integer formStringToInt(String string, Player player){
        int number;
        try{
            number = Integer.parseInt(string.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            player.sendMessage("数字を入力してください!");
            return null;
        }
        return number;
    }
    public static Integer formStringToInt(String string){
        int number;
        try{
            number = Integer.parseInt(string.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
        return number;
    }
    public static Integer formStringToInt(Component string, Player player){
        return formStringToInt(componentToString(string), player);
    }

    public static String componentToString(Component msg) {
        return ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.legacyAmpersand().serialize(msg));
    }
}
