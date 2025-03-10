package dev.felnull.pointed.fileio;

import dev.felnull.pointed.PointList;
import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.PlayerPointData;
import dev.felnull.pointed.data.RewardData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerPointDataIO {
    public static File playerPointDataFolder = new File("shared", "PlayerPointData");
    public static String pointSection = "Point.";
    public static String obtainedNumberSection = "ObtainedNumber.";
    public static String heldPointSection = ".heldpoint";
    public static String totalPointSection = ".total";

    public static void savePlayerPointData(PlayerPointData playerPointData) {
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
            playerPointDataYamlFile.set(pointSection + key + heldPointSection, playerPointData.getPoint(key));
            playerPointDataYamlFile.set(pointSection + key + totalPointSection, playerPointData.getTotalPoint(key));
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

    public static PlayerPointData loadPlayerPointData(OfflinePlayer player, PointList pointList) {
        File rewardDataFile = new File(playerPointDataFolder, String.valueOf(player.getUniqueId()) + ".yml");
        if(!rewardDataFile.exists()){
            savePlayerPointData(new PlayerPointData(player, pointList.getName(), 0));
            Bukkit.getLogger().info(player.getName() + "のPlayerPointDataが存在しないため生成しました");
        }

        FileConfiguration rewardDataYaml = YamlConfiguration.loadConfiguration(rewardDataFile);

        Set<String> keys = rewardDataYaml.getConfigurationSection("Point").getKeys(false);
        Map<String,Integer> pointMap = new HashMap<>();
        Map<String,Integer> totalPointMap = new HashMap<>();
        Map<Integer, Integer> numberofGetReward = new HashMap<>();

        for(String key : keys) {
            int point = rewardDataYaml.getInt(pointSection + key + heldPointSection);
            int totalPoint = rewardDataYaml.getInt(pointSection + key + totalPointSection);
            pointMap.put(key, point);
            totalPointMap.put(key, totalPoint);
        }

        List<RewardData> rewardDataList = RewardDataIO.loadRewards();
        for(RewardData rewardData : rewardDataList){
            numberofGetReward.put(rewardData.rewardID, rewardDataYaml.getInt(obtainedNumberSection + rewardData.rewardID));
        }
        PlayerPointData playerPointData = new PlayerPointData(player, pointMap, numberofGetReward, totalPointMap);
        Pointed.instance.playerPlayerPointDataCache.put(player, playerPointData);
        return playerPointData;
    }
}
