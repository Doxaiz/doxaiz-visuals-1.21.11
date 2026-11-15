package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class TabListModule extends Module {

    public final BooleanSetting numericPing = add(new BooleanSetting("Числовой пинг (мс)", true));
    public final BooleanSetting coloredPing = add(new BooleanSetting("Цветовая градация пинга", true));
    public final BooleanSetting showHealth = add(new BooleanSetting("Здоровье игроков (HP)", true));
    public final BooleanSetting show3DHeads = add(new BooleanSetting("3D головы скинов", true));
    public final ModeSetting pingStyle = add(new ModeSetting("Стиль пинга", "Цветной текст", "Бейдж с заливкой", "Минимальный"));
    public final NumberSetting maxPlayers = add(new NumberSetting("Лимит игроков", 80.0f, 20.0f, 120.0f, 5.0f));

    public TabListModule() {
        super("Tab List", "Стильное оформление списка игроков (Tab): точный пинг в мс, HP игроков и 3D головы", Category.INTERFACE);
    }

    public static String formatPing(int latency) {
        TabListModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(TabListModule.class);
        if (mod == null || !mod.isEnabled() || !mod.numericPing.get()) return "";

        String color = "§a";
        if (mod.coloredPing.get()) {
            if (latency > 150) color = "§c";
            else if (latency > 80) color = "§e";
        }
        return color + latency + "ms";
    }
}
