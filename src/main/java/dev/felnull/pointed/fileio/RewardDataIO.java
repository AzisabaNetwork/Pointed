package dev.felnull.pointed.fileio;

import dev.felnull.pointed.Pointed;
import dev.felnull.pointed.data.RewardData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RewardDataIO {
    static File playerPointDataFolder = new File("shared", "PointedReward");
    static String displayNameSection = ".DisplayName";
    static String needPointSection = ".NeedPoint";
    static String needMinPointSection = ".NeedMinPoint";
    static String repeatableSection = ".Repeatable";
    static String itemStackSection = ".ItemStack";


    public static boolean saveReward(RewardData rewardData) {
        initSaveReward(playerPointDataFolder);
        File playerPointDataFile = new File(playerPointDataFolder, "PointedReward" + ".yml");
        if(!playerPointDataFile.exists()) {
            try {
                playerPointDataFile.createNewFile(); // 新規ファイルを生成
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileConfiguration rewardDataYamlFile = YamlConfiguration.loadConfiguration(playerPointDataFile);

        //データ追加
        rewardDataYamlFile.set(rewardData.rewardID + displayNameSection, rewardData.displayName);
        rewardDataYamlFile.set(rewardData.rewardID + needPointSection, rewardData.needPoint);
        rewardDataYamlFile.set(rewardData.rewardID + needMinPointSection, rewardData.needMinPoint);
        rewardDataYamlFile.set(rewardData.rewardID + repeatableSection, rewardData.repeatable);
        rewardDataYamlFile.set(rewardData.rewardID + itemStackSection, rewardData.getRewardList());




        //データ書き込み
        try {
            rewardDataYamlFile.save(playerPointDataFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void initSaveReward(File settingsFolder) {
        //指定されたフォルダがなかったら生成
        if(!settingsFolder.exists()) {
            if(!settingsFolder.mkdirs()){
                return;
            }
        }
        return;
    }

    public static List<RewardData> loadRewards() {
        File rewardDataFile = new File(playerPointDataFolder, "PointedReward" + ".yml");

        FileConfiguration rewardDataYaml = YamlConfiguration.loadConfiguration(rewardDataFile);

        Set<String> keys = rewardDataYaml.getKeys(false);
        List<RewardData> rewardDataList = new ArrayList<>();

        for(String key : keys){
            String displayName = rewardDataYaml.getString(key + displayNameSection, "名無しの報酬");
            int needPoint = rewardDataYaml.getInt(key + needPointSection, 0);
            int needMinPoint = rewardDataYaml.getInt(key + needMinPointSection, 0);
            boolean repeatable = rewardDataYaml.getBoolean(key + repeatableSection, false);
            List<ItemStack> rewardList = (List<ItemStack>) rewardDataYaml.getList(key + itemStackSection);

            rewardDataList.add(new RewardData(Integer.parseInt(key), displayName, needPoint, needMinPoint, repeatable, rewardList));
        }

        Collections.sort(rewardDataList);

        return rewardDataList;
    }
}
