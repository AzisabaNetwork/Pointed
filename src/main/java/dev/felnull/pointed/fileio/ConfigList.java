package dev.felnull.pointed.fileio;

public enum ConfigList {
    CANUSEREWARDPAGE("CanUseRewardPage"),ISLOBBY("isLobby"),RANKING("ranking"),VIEWPOINT("ViewPoint");

    public String configName;

    ConfigList(String name){
        this.configName = name;
    }
}
