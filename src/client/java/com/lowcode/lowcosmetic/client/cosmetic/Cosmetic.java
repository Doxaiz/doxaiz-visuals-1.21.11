package com.lowcode.lowcosmetic.client.cosmetic;

import net.minecraft.util.Identifier;

public record Cosmetic(int id, String name, CosmeticType type, Rarity rarity, Identifier texture, CosmeticModel model) {
    public boolean isCape() {
        return false;
    }
}
