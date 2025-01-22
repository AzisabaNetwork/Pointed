package dev.felnull.pointed;

import dev.felnull.pointed.commands.MainPointedCommand;
import dev.felnull.pointed.commands.CreatePointedRewardCompleter;
import dev.felnull.pointed.commands.MyPoint;
import dev.felnull.pointed.commands.OpenReward;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.listener.ChatListener;
import dev.felnull.pointed.listener.CommonListener;
import dev.felnull.pointed.util.ChatReader;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class Pointed extends JavaPlugin {

    @Getter
    public static Pointed instance;
    @Getter
    public ChatReader chatReader;
    public Map<OfflinePlayer, PlayerPointData> playerPlayerPointDataCache = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        this.chatReader = new ChatReader();
        Bukkit.getLogger().info("Pointedが動作を開始しました");
        setupCommand();
        setupListener();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void setupCommand(){
        getCommand("pointed").setExecutor(new MainPointedCommand());
        getCommand("pointed").setTabCompleter(new CreatePointedRewardCompleter());
        getCommand("ptreward").setExecutor(new OpenReward());
        getCommand("mypoint").setExecutor(new MyPoint());
    }
    public void setupListener(){
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CommonListener(),this);
    }
}
