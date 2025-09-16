package dev.felnull.pointed.listener;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.database.dataio.SubjectPointsDao;
import dev.felnull.pointed.database.dataio.SubjectRepository;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.sql.SQLException;
/*
public class CommonListener implements Listener {
    //ログイン時PlayerPointData生成orキャッシュへ
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) throws SQLException {
        OfflinePlayer offlinePlayer = e.getPlayer();
        if(!Pointed.instance.playerPlayerPointDataCache.containsKey(offlinePlayer)){
            PlayerPointDataIO.loadPlayerPointData(offlinePlayer);
            SubjectPointsDao.loadOneByName(SubjectRepository.ensurePlayer(offlinePlayer.getUniqueId(), offlinePlayer.getName()), );
        }
    }

}

 */
