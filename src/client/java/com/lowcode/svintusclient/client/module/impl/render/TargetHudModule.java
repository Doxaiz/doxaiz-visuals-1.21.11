package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class TargetHudModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль карточки",
            "Современный (Modern)", "Кибер (Cyber)", "Компактный", "Стекло (Glass)", "Минимализм"));
    public final BooleanSetting showArmor = add(new BooleanSetting("Отображать броню и прочность", true));
    public final BooleanSetting showDistance = add(new BooleanSetting("Дистанция до цели", true));
    public final BooleanSetting smoothHp = add(new BooleanSetting("Плавная полоска здоровья", true));
    public final BooleanSetting damageLag = add(new BooleanSetting("Lag-полоса полученного урона", true));
    public final BooleanSetting hitSparks = add(new BooleanSetting("Искры при получении урона", true));
    public final BooleanSetting absorption = add(new BooleanSetting("Золотые сердца поглощения", true));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета полосы",
            "Градиент здоровья", "Акцент темы", "Радуга", "Пользовательский"));
    public final ColorSetting customColor = add(new ColorSetting("Кастомный цвет", 0xFF6C5CE7));
    public final NumberSetting scale = add(new NumberSetting("Масштаб", 1.0f, 0.75f, 1.4f, 0.05f));

    public TargetHudModule() {
        super("Target HUD", "Отображает стильную панель цели с аватаром, здоровьем, бронёй и дистанцией", Category.INTERFACE);
    }

    @Override
    public void onEnable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.targetHud.set(true);
        }
    }

    @Override
    public void onDisable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.targetHud.set(false);
        }
    }
}
