package dev.felnull.pointed.teams.manager.reward.data;

import dev.felnull.pointed.core.util.ColorUtil;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;

public class TeamData {
    private final String id;     // 内部ID (例: "RED")
    @Setter
    private String name;   // 表示名 (例: "赤チーム")
    private String color;  // 色コード (例: "&c")

    public TeamData(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public String id() { return id; }
    public String name() { return name; }

    public String color() { return color; }
    public void setColor(NamedTextColor namedTextColor) {
        this.color = ColorUtil.toLegacyCode(namedTextColor);
    }
}