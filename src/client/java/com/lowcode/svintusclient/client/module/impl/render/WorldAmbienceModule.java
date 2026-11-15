package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

public final class WorldAmbienceModule extends Module implements TickListener {

    public final ModeSetting timeMode = add(new ModeSetting("Время суток",
            "Ванильное", "День", "Закат", "Ночь", "Полночь", "Кастомное"));
    public final NumberSetting customTime = add(new NumberSetting("Время (тики)", 6000.0f, 0.0f, 24000.0f, 500.0f));
    public final ModeSetting weather = add(new ModeSetting("Погода",
            "Ванильная", "Ясно", "Дождь", "Гроза"));

    public final ModeSetting fogMode = add(new ModeSetting("Режим тумана",
            "Без тумана", "Кастомный", "Неоновая дымка", "Ванильный"));
    public final NumberSetting fogDistance = add(new NumberSetting("Дистанция тумана", 45.0f, 5.0f, 200.0f, 5.0f));
    public final ColorSetting fogColor = add(new ColorSetting("Цвет тумана", 0xFF6C5CE7));

    public final BooleanSetting hideScreenFire = add(new BooleanSetting("Скрыть огонь на экране", true));

    public WorldAmbienceModule() {
        super("World Ambience", "Управление окружением: время суток, дистанция тумана и огонь на экране", Category.VISUALS);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        String tm = timeMode.get();
        if (!"Ванильное".equals(tm)) {
            long t = switch (tm) {
                case "День" -> 1000L;
                case "Закат" -> 12000L;
                case "Ночь" -> 13000L;
                case "Полночь" -> 18000L;
                case "Кастомное" -> customTime.get().longValue();
                default -> -1L;
            };
            if (t >= 0) {
                mc.world.setTime(mc.world.getTime(), t, false);
            }
        }

        String w = weather.get();
        if ("Ясно".equals(w)) {
            mc.world.setRainGradient(0.0f);
            mc.world.setThunderGradient(0.0f);
        } else if ("Дождь".equals(w)) {
            mc.world.setRainGradient(1.0f);
            mc.world.setThunderGradient(0.0f);
        } else if ("Гроза".equals(w)) {
            mc.world.setRainGradient(1.0f);
            mc.world.setThunderGradient(1.0f);
        }
    }
}
