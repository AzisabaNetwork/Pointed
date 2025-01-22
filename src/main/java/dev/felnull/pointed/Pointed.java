package dev.felnull.pointed;

import dev.felnull.pointed.commands.CreatePointedReward;
import dev.felnull.pointed.commands.CreatePointedRewardCompleter;
import dev.felnull.pointed.data.PlayerPointData;
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
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void setupCommand(){
        getCommand("pointed").setExecutor(new CreatePointedReward());
        getCommand("pointed").setTabCompleter(new CreatePointedRewardCompleter());
    }
}
