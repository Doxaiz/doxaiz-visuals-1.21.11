package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;

public final class CoordsHudModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль панели",
            "Стеклянная карта (Modern Glass)", "Капсула (Glass Pill)", "Компактный чип", "Неоновая полоса (Neon Strip)"));
    public final BooleanSetting showNether = add(new BooleanSetting("Координаты Незера (X/8, Z/8)", true));
    public final BooleanSetting showBiome = add(new BooleanSetting("Текущий биом", true));
    public final BooleanSetting showDirection = add(new BooleanSetting("Сторона света", true));
    public final ModeSetting directionFormat = add(new ModeSetting("Формат стороны света",
            "Русский полный (Север, Юг...)", "Русский краткий (С, Ю, З, В)", "Английский краткий (N, S, W, E)", "Английский полный (North...)"));
    public final BooleanSetting showDegrees = add(new BooleanSetting("Градусы поворота (Yaw)", true));
    public final BooleanSetting showSpeed = add(new BooleanSetting("Скорость перемещения (BPS)", true));

    public CoordsHudModule() {
        super("Coords HUD", "Стильная панель с координатами X Y Z, биомом, стороной света и скоростью", Category.INTERFACE);
    }

    @Override
    public void onEnable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.coordinates.set(true);
        }
    }

    @Override
    public void onDisable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.coordinates.set(false);
        }
    }
}
