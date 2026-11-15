package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;

public final class PerformanceModule extends Module implements TickListener {

    public final BooleanSetting autoGc = add(new BooleanSetting("Автоочистка памяти (Auto GC)", true));
    public final BooleanSetting particleLimiter = add(new BooleanSetting("Лимит частиц (Защита от лагов)", true));
    public final BooleanSetting fastEntities = add(new BooleanSetting("Оптимизация анимаций сущностей", true));
    public final BooleanSetting fastFont = add(new BooleanSetting("Кэширование шрифтов", true));

    private long lastGcTime = 0;
    private int lastFreedMb = 0;

    public PerformanceModule() {
        super("Performance", "Клиентские оптимизации: автоочистка RAM, лимит частиц и быстрые анимации", Category.SETTINGS);
        enableByDefault();
    }

    @Override
    public void onTick() {
        if (!isEnabled() || !autoGc.get()) return;
        long now = System.currentTimeMillis();
        if (now - lastGcTime < 30_000L) return;

        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;

        if (max > 0 && ((double) used / (double) max) > 0.85) {
            long before = used / (1024 * 1024);
            System.gc();
            long after = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            lastFreedMb = (int) Math.max(0, before - after);
            lastGcTime = now;
            if (lastFreedMb > 0) {
                com.lowcode.svintusclient.client.hud.Notifications.push("Очищено " + lastFreedMb + " MB RAM", false);
            }
        }
    }

    public int getLastFreedMb() {
        return lastFreedMb;
    }

    public boolean allowParticle(int count) {
        if (!isEnabled() || !particleLimiter.get()) return true;
        return count < 400;
    }

    public boolean shouldSkipFarEntity(double distSq) {
        if (!isEnabled() || !fastEntities.get()) return false;
        return distSq > (48.0 * 48.0);
    }

    public boolean isFastFont() {
        return isEnabled() && fastFont.get();
    }
}
