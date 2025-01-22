package dev.felnull.pointed.data;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RewardData implements Comparable<RewardData> {
    public Integer rewardID;
    public String displayName;
    public Integer needPoint;

    public Integer needMinPoint;
    public boolean repeatable;

    @Getter
    private List<ItemStack> rewardList = new ArrayList<>();

    public RewardData(Integer rewardID, String displayName, Integer needPoint, Integer needMinPoint, boolean repeatable){
        this.rewardID = rewardID;
        this.displayName = displayName;
        this.needPoint = needPoint;
        this.needMinPoint = needMinPoint;
        this.repeatable = repeatable;
    }

    public RewardData(int rewardID, String displayName, int needPoint, int needMinPoint, boolean repeatable, List<ItemStack> rewardList){
        this.rewardID = rewardID;
        this.displayName = displayName;
        this.needPoint = needPoint;
        this.needMinPoint = needMinPoint;
        this.repeatable = repeatable;
        this.rewardList = rewardList;
    }

    public void addReward(ItemStack reward){
        rewardList.add(reward);
    }

    public void removeReward(int number){
        rewardList.remove(number);
    }

    public void setReward(List<ItemStack> itemStacks){
        rewardList = itemStacks;
    }

    @Override
    public int compareTo(@NotNull RewardData o) {
        return Integer.compare(this.rewardID, o.rewardID);
    }
}
