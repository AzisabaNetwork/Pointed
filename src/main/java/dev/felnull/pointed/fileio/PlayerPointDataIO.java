package dev.felnull.pointed.fileio;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.data.RewardData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerPointDataIO {
    static File playerPointDataFolder = new File(SharedDir.dir, "PlayerPointData");
    static String pointSection = "Point.";
    static String obtainedNumberSection = "ObtainedNumber.";

    public static void saveSettings(PlayerPointData playerPointData) {
        initSaveSettings(playerPointDataFolder);
        File playerPointDataFile = new File(playerPointDataFolder, String.valueOf(playerPointData.offlinePlayer.getUniqueId()) + ".yml");
        if(!playerPointDataFile.exists()) {
            try {
                playerPointDataFile.createNewFile(); // 新規ファイルを生成
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileConfiguration playerPointDataYamlFile = YamlConfiguration.loadConfiguration(playerPointDataFile);

        //データ追加
        for(String key: playerPointData.getPointMapKeys()){
            playerPointDataYamlFile.set(pointSection + key, playerPointData.getPoint(key));
        }
        List<RewardData> rewardDataList = RewardDataIO.loadRewards();
        for(RewardData rewardData : rewardDataList){
            playerPointDataYamlFile.set(obtainedNumberSection + rewardData.rewardID, playerPointData.getnumberofGetReward(rewardData));
        }


        //データ書き込み
        try {
            playerPointDataYamlFile.save(playerPointDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initSaveSettings(File settingsFolder) {
        //指定されたフォルダがなかったら生成
        if(!settingsFolder.exists()) {
            if(!settingsFolder.mkdirs()){
                return;
            }
        }
        return;
    }

    public static PlayerPointData loadPlayerPointData(OfflinePlayer player) {
        File rewardDataFile = new File(playerPointDataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        if(!rewardDataFile.exists()){
            saveSettings(new PlayerPointData(player, PointList.EVENT_POINT.getName(), 0));
            Bukkit.getLogger().info(player.getName() + "のPlayerPointDataが存在しないため生成しました");
        }

        FileConfiguration rewardDataYaml = YamlConfiguration.loadConfiguration(rewardDataFile);

        Set<String> keys = rewardDataYaml.getConfigurationSection("Point").getKeys(false);
        Map<String,Integer> pointMap = new HashMap<>();
        Map<RewardData, Integer> numberofGetReward = new HashMap<>();

        for(String key : keys) {
            int point = rewardDataYaml.getInt(pointSection + key);
            pointMap.put(key,point);
        }

        List<RewardData> rewardDataList = RewardDataIO.loadRewards();
        for(RewardData rewardData : rewardDataList){
            numberofGetReward.put(rewardData, rewardDataYaml.getInt(obtainedNumberSection + rewardData.rewardID));
        }
        PlayerPointData playerPointData = new PlayerPointData(player, pointMap, numberofGetReward);
        Pointed.instance.playerPlayerPointDataCache.put(player, playerPointData);
        return playerPointData;
    }
}
