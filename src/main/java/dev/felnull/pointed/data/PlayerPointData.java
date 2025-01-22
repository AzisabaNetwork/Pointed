package dev.felnull.pointed.data;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlayerPointData {
    public OfflinePlayer offlinePlayer;
    private Map<String,Integer> pointMap = new HashMap<>();
    private Map<RewardData, Integer> numberofGetReward = new HashMap<>();

    public PlayerPointData(OfflinePlayer player, String pointName, int initNumber){
        this.offlinePlayer = player;
        this.pointMap.put(pointName, initNumber);
        numberofGetReward.clear();
    }

    public PlayerPointData(OfflinePlayer player, Map<String,Integer> pointMap, Map<RewardData, Integer> numberofGetReward){
        this.offlinePlayer = player;
        this.pointMap = pointMap;
        this.numberofGetReward = numberofGetReward;
    }

    //返り値は更新後の値
    public int addPoint(String pointName, int addNumber){
        pointMap.compute(pointName, (k, beforeNumber) -> (beforeNumber == null ? 0 : beforeNumber) + addNumber);
        return pointMap.get(pointName);
    }

    public int subtractPoint(String pointName, int addNumber){
        pointMap.compute(pointName, (k, beforeNumber) -> (beforeNumber == null ? 0 : beforeNumber) - addNumber);
        return pointMap.get(pointName);
    }

    public int getPoint(String pointName){
        return pointMap.get(pointName);
    }

    public int setPoint(String pointName, int setNumber){
        pointMap.put(pointName, setNumber);
        return pointMap.get(pointName);
    }
    //指定した報酬を何回受け取ったか
    public int getnumberofGetReward(RewardData rewardData){
        return numberofGetReward.getOrDefault(rewardData, 0);
    }

    public void addnumberofGetReward(RewardData rewardData){
        numberofGetReward.compute(rewardData, (k, beforeNumber) -> (beforeNumber == null ? 0 : beforeNumber) + 1);
    }

    public void resetNumberofGetReward(RewardData rewardData){
        numberofGetReward.put(rewardData, 0);
    }

    public Set<String> getPointMapKeys(){
        return pointMap.keySet();
    }
}
