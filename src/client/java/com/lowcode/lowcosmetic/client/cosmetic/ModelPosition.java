package com.lowcode.lowcosmetic.client.cosmetic;

public enum ModelPosition {
    FREE,
    BODY,
    HEAD,
    ABOVE_HEAD,
    RIGHT_ARM,
    LEFT_ARM,
    RIGHT_LEG,
    LEFT_LEG;

    public static ModelPosition byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : BODY;
    }
}
