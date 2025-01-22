package dev.felnull.pointed;

import lombok.Getter;

public enum PointList {
    EVENT_POINT("Event_Point");
    @Getter
    private final String name;

    PointList(String name) {
        this.name = name;
    }
}
