package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;

public final class PotionsHudModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль карточек",
            "Стекло (Glass)", "Ванильный", "Минимализм", "Неон"));
    public final BooleanSetting showAmplifier = add(new BooleanSetting("Уровень (Римские цифры)", true));
    public final BooleanSetting showDuration = add(new BooleanSetting("Время действия (MM:SS)", true));
    public final BooleanSetting hideVanilla = add(new BooleanSetting("Скрыть ванильные иконки", true));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Цвет эффекта", "Акцент темы", "Радуга", "Пользовательский"));
    public final ColorSetting customColor = add(new ColorSetting("Кастомный цвет", 0xFF6C5CE7));
    public final BooleanSetting blinkWarning = add(new BooleanSetting("Мигание при < 10 сек", true));

    public PotionsHudModule() {
        super("Potions HUD", "Отображение активных эффектов зелий, таймеров и уровней с кастомными стилями", Category.INTERFACE);
    }

    @Override
    public void onEnable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.potions.set(true);
        }
    }

    @Override
    public void onDisable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.potions.set(false);
        }
    }
}
