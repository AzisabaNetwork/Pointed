package dev.felnull.pointed;

import dev.felnull.pointed.commands.*;
import dev.felnull.pointed.database.Db;
import dev.felnull.pointed.database.TableInitializer;
import dev.felnull.pointed.database.api.PointServiceImpl;
import dev.felnull.pointed.listener.ChatListener;
import dev.felnull.pointed.util.ChatReader;
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

    @Override
    public void onEnable() {
        instance = this;
        FileConfiguration conf = getConfig();
        Db.init(conf.getString("database.host"), conf.getInt("database.port"), conf.getString("database.database"), conf.getString("database.user"), conf.getString("database.pass"));
        this.chatReader = new ChatReader();
        Bukkit.getLogger().info("Pointedが動作を開始しました");
        setupCommand();
        setupListener();
        saveDefaultConfig();
        setupPlugin();
        TableInitializer.initTables();
    }

    @Override
    public void onDisable() {
        for(BukkitTask task : taskList){
            task.cancel();
        }
        Db.close();
    }

    public void setupCommand(){
        getCommand("pt").setExecutor(new PtCommand(new PointServiceImpl(Db.get(), zoneId), this, zoneId) {
        });
    }
    public void setupListener(){
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
    }
    public void setupPlugin(){
    }
}
