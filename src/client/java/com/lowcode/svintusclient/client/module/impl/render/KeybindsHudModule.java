package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;

public final class KeybindsHudModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль панели",
            "Стекло (Glass)", "Компактный", "Минимализм", "Неон"));
    public final BooleanSetting onlyActive = add(new BooleanSetting("Только включённые", true));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Акцент темы", "Белый", "Неоновый циановый", "Свой цвет"));
    public final ColorSetting customColor = add(new ColorSetting("Кастомный цвет", 0xFFFFA502));
    public final BooleanSetting showKeyName = add(new BooleanSetting("Отображать клавишу", true));
    public final BooleanSetting showCategory = add(new BooleanSetting("Раздел модуля", false));

    public boolean shouldShowKeyName() {
        return isEnabled() && showKeyName.get();
    }

    public boolean shouldShowCategory() {
        return isEnabled() && showCategory.get();
    }

    public KeybindsHudModule() {
        super("Keybinds HUD", "Виджет активных клавиш и биндов модулей на экране", Category.INTERFACE);
    }

    @Override
    public void onEnable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.keybinds.set(true);
        }
    }

    @Override
    public void onDisable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.keybinds.set(false);
        }
    }
}
