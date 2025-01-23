package dev.felnull.pointed.data;

import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PlayerPointData {
    public OfflinePlayer offlinePlayer;
    private Map<String,Integer> pointMap = new HashMap<>();
    //累計ポイント数
    private Map<String, Integer> totalPointMap = new HashMap<>();
    private Map<Integer, Integer> numberOfGetReward = new HashMap<>();

    public PlayerPointData(OfflinePlayer player, String pointName, int initNumber){
        this.offlinePlayer = player;
        this.pointMap.put(pointName, initNumber);
        numberOfGetReward.clear();
    }

    public PlayerPointData(OfflinePlayer player, Map<String,Integer> pointMap, Map<Integer, Integer> numberofGetReward, Map<String,Integer> totalPointMap){
        this.offlinePlayer = player;
        this.pointMap = pointMap;
        this.numberOfGetReward = numberofGetReward;
        this.totalPointMap = totalPointMap;
    }

    //返り値は更新後の値
    public int addPoint(String pointName, int addNumber){
        pointMap.compute(pointName, (k, beforeNumber) -> (beforeNumber == null ? 0 : beforeNumber) + addNumber);
        totalPointMap.compute(pointName, (k, beforeNumber) -> (beforeNumber == null ? 0 : beforeNumber) + addNumber);
        return pointMap.get(pointName);
    }

    public int subtractPoint(String pointName, int addNumber){
        pointMap.compute(pointName, (k, beforeNumber) -> (beforeNumber == null ? 0 : beforeNumber) - addNumber);
        return pointMap.get(pointName);
    }

    public int getPoint(String pointName){
        return pointMap.get(pointName);
    }

    public int getTotalPoint(String pointName){
        return totalPointMap.getOrDefault(pointName, 0);
    }

    public int setPoint(String pointName, int setNumber){
        pointMap.put(pointName, setNumber);
        return pointMap.get(pointName);
    }

    public int setTotalPoint(String pointName, int setNumber){
        totalPointMap.put(pointName, setNumber);
        return totalPointMap.get(pointName);
    }

    //指定した報酬を何回受け取ったか
    public int getnumberofGetReward(RewardData rewardData){
        return numberOfGetReward.getOrDefault(rewardData.rewardID, 0);
    }

    public void addnumberofGetReward(RewardData rewardData){
        numberOfGetReward.put(rewardData.rewardID, numberOfGetReward.getOrDefault(rewardData.rewardID, 0) + 1);
    }

    public void resetNumberofGetReward(RewardData rewardData){
        numberOfGetReward.put(rewardData.rewardID, 0);
    }

    public Set<String> getPointMapKeys(){
        return pointMap.keySet();
    }
}
