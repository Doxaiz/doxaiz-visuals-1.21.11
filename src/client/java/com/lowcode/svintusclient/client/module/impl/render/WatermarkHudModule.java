package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;

public final class WatermarkHudModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль ватермарки",
            "Стильный чип", "Классический текст", "Неоновый бейдж"));
    public final BooleanSetting nick = add(new BooleanSetting("Имя игрока", true));
    public final BooleanSetting fps = add(new BooleanSetting("Счётчик FPS", true));
    public final BooleanSetting ping = add(new BooleanSetting("Пинг сервера", true));
    public final BooleanSetting tps = add(new BooleanSetting("TPS сервера", false));
    public final BooleanSetting ram = add(new BooleanSetting("ОЗУ (RAM)", false));
    public final BooleanSetting time = add(new BooleanSetting("Реальное время", true));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Акцент темы", "Неон", "Хрома", "Свой цвет"));
    public final ColorSetting customColor = add(new ColorSetting("Кастомный цвет", 0xFF05D9E8));

    public WatermarkHudModule() {
        super("Watermark", "Стильная плашка клиента: ник, FPS, TPS, пинг, память (RAM) и время", Category.INTERFACE);
        enableByDefault();
    }

    @Override
    public void onEnable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.watermark.set(true);
        }
    }

    @Override
    public void onDisable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.watermark.set(false);
        }
    }
}
