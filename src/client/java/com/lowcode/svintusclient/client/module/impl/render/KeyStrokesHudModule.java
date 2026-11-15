package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;

public final class KeyStrokesHudModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль клавиш",
            "Акрил (Acrylic)", "Ваниль", "Неоновый контур", "Тёмный (Dark)"));
    public final ModeSetting layout = add(new ModeSetting("Набор клавиш",
            "Полный набор (+ CPS)", "WASD + Мышь (ЛКМ/ПКМ)", "WASD + Пробел", "Только WASD"));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Акцент темы", "Хрома-волна", "Свой цвет"));
    public final ColorSetting customColor = add(new ColorSetting("Кастомный цвет", 0xFF05D9E8));
    public final BooleanSetting showCps = add(new BooleanSetting("Счётчик кликов (CPS)", true));
    public final BooleanSetting showSpace = add(new BooleanSetting("Клавиша Пробела", true));

    public KeyStrokesHudModule() {
        super("KeyStrokes", "Отображение нажатий клавиш WASD, Пробела и кликов мыши (CPS)", Category.INTERFACE);
    }

    @Override
    public void onEnable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.keystrokes.set(true);
        }
    }

    @Override
    public void onDisable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.keystrokes.set(false);
        }
    }
}
