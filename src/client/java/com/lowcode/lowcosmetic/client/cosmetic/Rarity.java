package com.lowcode.lowcosmetic.client.cosmetic;

public enum Rarity {
    COMMON("common", "Обычная", 0xFFA3A3B8),
    UNCOMMON("uncommon", "Необычная", 0xFF4ADE80),
    RARE("rare", "Редкая", 0xFF60A5FA),
    EPIC("epic", "Эпическая", 0xFFC084FC),
    LEGENDARY("legendary", "Легендарная", 0xFFFBBF24);

    public final String id;
    public final String label;
    public final int color;

    Rarity(String id, String label, int color) {
        this.id = id;
        this.label = label;
        this.color = color;
    }

    public static Rarity byId(String id) {
        for (Rarity rarity : values()) {
            if (rarity.id.equalsIgnoreCase(id)) {
                return rarity;
            }
        }
        return COMMON;
    }
}
