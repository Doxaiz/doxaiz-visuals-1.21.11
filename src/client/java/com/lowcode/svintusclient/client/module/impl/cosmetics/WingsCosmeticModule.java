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

public final class WingsCosmeticModule extends Module implements TickListener {

    public final ModeSetting wingType = add(new ModeSetting("Модель", "Крылья Неона",
            "Тёмные крылья",
            "Крылья Гуля",
            "Крылья Рокера",
            "Крылья Воты",
            "Крылья Флюгера",
            "Крылья Трезубца",
            "Пасхальные крылья",
            "Крылья Ангела",
            "Крылья Дракончика",
            "Синие крылья дракона",
            "Крылья Паука",
            "Вулканические крылья",
            "Катана за спиной"
    ));
    public final BooleanSetting selfOnly = add(new BooleanSetting("Только у себя", false));
    public final NumberSetting flapSpeed = add(new NumberSetting("Скорость взмахов", 1.0f, 0.2f, 2.5f, 0.1f));
    public final NumberSetting scale = add(new NumberSetting("Масштаб", 1.0f, 0.5f, 2.0f, 0.05f));
    public final BooleanSetting glow = add(new BooleanSetting("Неоновое свечение", true));

    public float getFlapSpeed() {
        return isEnabled() ? flapSpeed.get() : 1.0f;
    }

    public WingsCosmeticModule() {
        super("Wings", "3D анимированные крылья за спиной персонажа", Category.COSMETICS);
    }

    private int getIdForName(String name) {
        return switch (name) {
            case "Крылья Неона" -> 14;
            case "Тёмные крылья" -> 15;
            case "Крылья Гуля" -> 16;
            case "Крылья Рокера" -> 17;
            case "Крылья Воты" -> 18;
            case "Крылья Флюгера" -> 19;
            case "Крылья Трезубца" -> 20;
            case "Пасхальные крылья" -> 21;
            case "Крылья Ангела" -> 22;
            case "Крылья Дракончика" -> 23;
            case "Синие крылья дракона" -> 24;
            case "Крылья Паука" -> 25;
            case "Вулканические крылья" -> 26;
            case "Катана за спиной" -> 27;
            default -> 14;
        };
    }

    private void updateEquipped() {
        if (!isEnabled()) return;
        int id = getIdForName(wingType.get());
        Cosmetic cosmetic = CosmeticRegistry.get(id);
        if (cosmetic != null) {
            CosmeticState.equip(cosmetic);
        }
    }

    private String lastType = "";

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        String cur = wingType.get();
        if (!cur.equals(lastType)) {
            lastType = cur;
            updateEquipped();
        }
    }

    @Override
    protected void onEnable() {
        lastType = wingType.get();
        updateEquipped();
    }

    @Override
    protected void onDisable() {
        CosmeticState.unequip(CosmeticType.WINGS);
    }
}
