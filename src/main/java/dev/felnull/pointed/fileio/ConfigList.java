package dev.felnull.pointed.fileio;

public enum ConfigList {
    CANUSEREWARDPAGE("CanUseRewardPage"),ISLOBBY("isLobby");

    public String configName;

    ConfigList(String name){
        this.configName = name;
    }
}
