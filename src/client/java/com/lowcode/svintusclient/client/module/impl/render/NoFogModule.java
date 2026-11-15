package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class NoFogModule extends Module {

    public final BooleanSetting terrainFog = add(new BooleanSetting("Атмосферный туман", true));
    public final BooleanSetting waterFog = add(new BooleanSetting("Туман в воде", true));
    public final BooleanSetting lavaFog = add(new BooleanSetting("Туман в лаве", true));
    public final BooleanSetting darknessFog = add(new BooleanSetting("Эффект темноты", true));
    public final NumberSetting customDistance = add(new NumberSetting("Дистанция отсечения", 9999.0f, 64.0f, 10000.0f, 64.0f));

    public NoFogModule() {
        super("No Fog", "Полное отключение атмосферного и глубинного тумана", Category.UTILITIES);
    }
}
