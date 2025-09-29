package dev.felnull.pointed;

import dev.felnull.pointed.core.commands.*;
import dev.felnull.pointed.core.database.Db;
import dev.felnull.pointed.core.database.TableInitializer;
import dev.felnull.pointed.core.database.Names;
import dev.felnull.pointed.core.database.api.PointService;
import dev.felnull.pointed.core.database.api.PointServiceImpl;
import dev.felnull.pointed.core.listener.ChatListener;
import dev.felnull.pointed.core.util.ChatReader;
import dev.felnull.pointed.teams.database.TeamTableInitializer;
import dev.felnull.pointed.teams.manager.TeamManager;
import dev.felnull.pointed.teams.manager.TeamManagerImpl;
import dev.felnull.pointed.teams.manager.reward.RankingService;
import dev.felnull.pointed.teams.manager.reward.RewardAdminService;
import dev.felnull.pointed.teams.manager.reward.impl.RankingServiceImpl;
import dev.felnull.pointed.teams.manager.reward.impl.RewardAdminServiceImpl;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class Pointed extends JavaPlugin {

    @Getter
    public static Pointed instance;
    @Getter
    public ChatReader chatReader;
    public static List<BukkitTask> taskList = new ArrayList<>();
    ZoneId zoneId = ZoneId.of(getConfig().getString("timezone", "Asia/Tokyo"));
    @Getter
    public PointServiceImpl pointService;
    @Getter
    public TeamManager teamManager;
    @Getter
    public RankingService rankingService;
    @Getter
    public RewardAdminService rewardAdminService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        FileConfiguration conf = getConfig();
        String prefix = conf.getString("database.table_prefix", "pointed_");
        Names.init(prefix);
        Db.init(conf.getString("database.host"), conf.getInt("database.port"), conf.getString("database.database"), conf.getString("database.user"), conf.getString("database.pass"));
        this.chatReader = new ChatReader();
        this.pointService = new PointServiceImpl(Db.get(), zoneId);
        this.teamManager = new TeamManagerImpl();
        this.rankingService = new RankingServiceImpl(Db.get());
        this.rewardAdminService = new RewardAdminServiceImpl(Db.get(), rankingService);

        Bukkit.getLogger().info("Pointedが動作を開始しました");
        setupCommand();
        setupListener();

        setupPlugin();
        TableInitializer.initTables();
        TeamTableInitializer.initTables();
    }

    @Override
    public void onDisable() {
        for(BukkitTask task : taskList){
            task.cancel();
        }
        Db.close();
    }

    public void setupCommand(){
        getCommand("pt").setExecutor(new PtCommand(pointService, zoneId));
        getCommand("ptteam").setExecutor(new PtTeam());
    }
    public void setupListener(){
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
    }
    public void setupPlugin(){
    }
}
