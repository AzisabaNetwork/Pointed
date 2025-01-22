package dev.felnull.pointed.listener;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.fileio.PlayerPointDataIO;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class CommonListener implements Listener {
    //ログイン時PlayerPointData生成orキャッシュへ
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e){
        OfflinePlayer offlinePlayer = e.getPlayer();
        if(!Pointed.instance.playerPlayerPointDataCache.containsKey(offlinePlayer)){
            PlayerPointDataIO.loadPlayerPointData(offlinePlayer);
        }
    }
}
