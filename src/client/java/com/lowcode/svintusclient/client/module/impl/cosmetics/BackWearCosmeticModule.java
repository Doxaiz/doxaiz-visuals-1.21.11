package com.lowcode.svintusclient.client.module.impl.cosmetics;

import com.lowcode.lowcosmetic.client.cosmetic.Cosmetic;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticRegistry;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticState;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticType;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class BackWearCosmeticModule extends Module implements TickListener {

    public final ModeSetting itemType = add(new ModeSetting("Предмет", "Катана за спиной"
    ));
    public final BooleanSetting selfOnly = add(new BooleanSetting("Только у себя", false));
    public final NumberSetting scale = add(new NumberSetting("Масштаб", 1.0f, 0.5f, 2.0f, 0.05f));
    public final BooleanSetting dynamicSway = add(new BooleanSetting("Покачивание при беге", true));
    public final NumberSetting zOffset = add(new NumberSetting("Смещение Z", 0.0f, -0.2f, 0.2f, 0.02f));

    public float getZOffset() {
        return isEnabled() ? zOffset.get() : 0.0f;
    }

    public boolean hasDynamicSway() {
        return isEnabled() && dynamicSway.get();
    }

    public BackWearCosmeticModule() {
        super("Back Accessories", "3D катана и аксессуары за спиной", Category.COSMETICS);
    }

    private int getIdForName(String name) {
        return switch (name) {
            case "Катана за спиной" -> 27;
            default -> 27;
        };
    }

    private void updateEquipped() {
        if (!isEnabled()) return;
        int id = getIdForName(itemType.get());
        Cosmetic cosmetic = CosmeticRegistry.get(id);
        if (cosmetic != null) {
            CosmeticState.equip(cosmetic);
        }
    }

    private String lastType = "";

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        String cur = itemType.get();
        if (!cur.equals(lastType)) {
            lastType = cur;
            updateEquipped();
        }
    }

    @Override
    protected void onEnable() {
        lastType = itemType.get();
        updateEquipped();
    }

    @Override
    protected void onDisable() {
        CosmeticState.unequip(CosmeticType.BODYWEAR);
        CosmeticState.unequip(CosmeticType.WINGS);
    }
}
