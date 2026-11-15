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

public final class PetsCosmeticModule extends Module implements TickListener {

    public final ModeSetting petType = add(new ModeSetting("Питомец", "Дракоша",
            "Котик",
            "Лисёнок",
            "Волчонок",
            "Тортик",
            "Пчёлка",
            "Пингвинёнок",
            "Алмазный питомец",
            "Редиска",
            "Демон",
            "Ангел"
    ));
    public final BooleanSetting selfOnly = add(new BooleanSetting("Только у себя", false));
    public final NumberSetting followDistance = add(new NumberSetting("Дистанция следования", 1.2f, 0.4f, 3.0f, 0.1f));
    public final NumberSetting bobbingSpeed = add(new NumberSetting("Скорость парения", 1.0f, 0.2f, 2.5f, 0.1f));
    public final BooleanSetting petSounds = add(new BooleanSetting("Звуки питомца", true));

    public float getFollowDistance() {
        return isEnabled() ? followDistance.get() : 1.2f;
    }

    public float getBobbingSpeed() {
        return isEnabled() ? bobbingSpeed.get() : 1.0f;
    }

    public boolean hasPetSounds() {
        return isEnabled() && petSounds.get();
    }

    public PetsCosmeticModule() {
        super("3D Pets", "Летающие 3D питомцы-спутники с плавной анимацией", Category.COSMETICS);
    }

    private int getIdForName(String name) {
        return switch (name) {
            case "Дракоша" -> 38;
            case "Котик" -> 39;
            case "Лисёнок" -> 40;
            case "Волчонок" -> 41;
            case "Тортик" -> 42;
            case "Пчёлка" -> 43;
            case "Пингвинёнок" -> 44;
            case "Алмазный питомец" -> 46;
            case "Редиска" -> 47;
            case "Демон" -> 48;
            case "Ангел" -> 49;
            default -> 38;
        };
    }

    private void updateEquipped() {
        if (!isEnabled()) return;
        int id = getIdForName(petType.get());
        Cosmetic cosmetic = CosmeticRegistry.get(id);
        if (cosmetic != null) {
            CosmeticState.equip(cosmetic);
        }
    }

    private String lastType = "";

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        String cur = petType.get();
        if (!cur.equals(lastType)) {
            lastType = cur;
            updateEquipped();
        }
    }

    @Override
    protected void onEnable() {
        lastType = petType.get();
        updateEquipped();
    }

    @Override
    protected void onDisable() {
        CosmeticState.unequip(CosmeticType.PET);
    }
}
