package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;

public final class CooldownsHudModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль кулдаунов",
            "Полосы (Bars)", "Компактный", "Сетка иконок", "Неон"));
    public final BooleanSetting showProgressBar = add(new BooleanSetting("Полоса прогресса", true));
    public final ModeSetting trackedSet = add(new ModeSetting("Отслеживать",
            "Все PvP предметы", "Только Перл и Яблоки", "Все с кулдауном"));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Акцент темы", "Градиент", "Золотой", "Свой цвет"));
    public final ColorSetting customColor = add(new ColorSetting("Кастомный цвет", 0xFF00D2D3));
    public final BooleanSetting showSeconds = add(new BooleanSetting("Цифровой таймер", true));
    public final BooleanSetting soundOnReady = add(new BooleanSetting("Звук при готовности", true));

    public CooldownsHudModule() {
        super("Cooldowns HUD", "Индикатор кулдаунов ключевых PvP предметов: перлы, яблоки, щиты, булава", Category.INTERFACE);
    }

    @Override
    public void onEnable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.cooldowns.set(true);
        }
    }

    @Override
    public void onDisable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.cooldowns.set(false);
        }
    }
}
