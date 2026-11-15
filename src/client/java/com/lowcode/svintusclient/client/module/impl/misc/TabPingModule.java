package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class TabPingModule extends Module {

    public final BooleanSetting showNumber = add(new BooleanSetting("Числовой пинг", true));
    public final BooleanSetting colorCode = add(new BooleanSetting("Цветовая шкала", true));
    public final NumberSetting updateInterval = add(new NumberSetting("Обновление (сек)", 2.0f, 1.0f, 10.0f, 1.0f));

    public TabPingModule() {
        super("Пинг в Tab", "Отображение точного пинга игроков в миллисекундах в списке Tab", Category.UTILITIES);
    }

    private long lastUpdate = 0;

    public boolean shouldUpdate() {
        long now = System.currentTimeMillis();
        long intervalMs = (long) (updateInterval.get() * 1000L);
        if (now - lastUpdate >= intervalMs) {
            lastUpdate = now;
            return true;
        }
        return false;
    }
}
