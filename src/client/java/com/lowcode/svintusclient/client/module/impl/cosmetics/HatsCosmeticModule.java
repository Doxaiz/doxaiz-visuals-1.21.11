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

public final class HatsCosmeticModule extends Module implements TickListener {

    public final ModeSetting hatType = add(new ModeSetting("Шляпа", "Сияющий нимб",
            "Тёмная шляпа",
            "Лиственная шапка",
            "Маска Бродяги",
            "Шапка Пингвина",
            "Шляпа Флюгера",
            "Шапка Медвежонка",
            "Шляпа Броненосца",
            "Шляпа Верблюда",
            "Шапка Курицы",
            "Шапка Жабки",
            "Шапка Овечки"
    ));
    public final BooleanSetting selfOnly = add(new BooleanSetting("Только у себя", false));
    public final NumberSetting yOffset = add(new NumberSetting("Смещение по высоте", 0.0f, -0.3f, 0.4f, 0.02f));
    public final NumberSetting scale = add(new NumberSetting("Масштаб", 1.0f, 0.5f, 2.0f, 0.05f));
    public final BooleanSetting rotate = add(new BooleanSetting("Плавное вращение", false));

    public boolean shouldRotate() {
        return isEnabled() && rotate.get();
    }

    public HatsCosmeticModule() {
        super("Hats & Masks", "3D головные уборы, маски и сияющие нимбы", Category.COSMETICS);
    }

    private int getIdForName(String name) {
        return switch (name) {
            case "Тёмная шляпа" -> 50;
            case "Сияющий нимб" -> 51;
            case "Лиственная шапка" -> 52;
            case "Маска Бродяги" -> 54;
            case "Шапка Пингвина" -> 55;
            case "Шляпа Флюгера" -> 56;
            case "Шапка Медвежонка" -> 57;
            case "Шляпа Броненосца" -> 58;
            case "Шляпа Верблюда" -> 59;
            case "Шапка Курицы" -> 60;
            case "Шапка Жабки" -> 61;
            case "Шапка Овечки" -> 62;
            default -> 51;
        };
    }

    private void updateEquipped() {
        if (!isEnabled()) return;
        int id = getIdForName(hatType.get());
        Cosmetic cosmetic = CosmeticRegistry.get(id);
        if (cosmetic != null) {
            CosmeticState.equip(cosmetic);
        }
    }

    private String lastType = "";

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        String cur = hatType.get();
        if (!cur.equals(lastType)) {
            lastType = cur;
            updateEquipped();
        }
    }

    @Override
    protected void onEnable() {
        lastType = hatType.get();
        updateEquipped();
    }

    @Override
    protected void onDisable() {
        CosmeticState.unequip(CosmeticType.HAT);
    }
}
