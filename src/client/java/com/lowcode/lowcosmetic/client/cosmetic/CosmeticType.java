package com.lowcode.lowcosmetic.client.cosmetic;

import com.lowcode.lowcosmetic.client.gui.Icons;
import net.minecraft.util.Identifier;

public enum CosmeticType {
    WINGS("wings", "Крылья", "Крылья", Icons.WINGS),
    HAT("hat", "Шляпы", "Шляпа", Icons.HAT),
    BODYWEAR("bodywear", "Одежда", "Одежда", Icons.BODYWEAR),
    PET("pet", "Питомцы", "Питомец", Icons.PET);

    public final String id;
    public final String plural;
    public final String singular;
    public final Identifier icon;

    CosmeticType(String id, String plural, String singular, Identifier icon) {
        this.id = id;
        this.plural = plural;
        this.singular = singular;
        this.icon = icon;
    }

    public static CosmeticType byId(String id) {
        for (CosmeticType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
