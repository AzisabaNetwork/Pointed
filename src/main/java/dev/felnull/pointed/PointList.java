package dev.felnull.pointed;

import lombok.Getter;

public enum PointList {
    EVENT_POINT("Event_Point", "event"),
    NORMAL_POINT("Normal_Point", "normal");
    @Getter
    private final String name;
    @Getter
    private final String allies;

    PointList(String name, String allies) {
        this.name = name;
        this.allies = allies;
    }

    public static PointList fromAllies(String allies) {
        for (PointList point : PointList.values()) {
            if (point.getAllies().equalsIgnoreCase(allies)) {
                return point;
            }
        }
        throw new IllegalArgumentException("そんな allies は存在しないにゃ: " + allies);
    }
}
