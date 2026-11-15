package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunksAnimationModule extends Module {

    public final ModeSetting mode = add(new ModeSetting("Режим анимации", "Всплывание снизу (Rise Up)", "Падение сверху (Fall Down)", "Мягкое появление (Fade Pop)", "Горизонт (Slide In)"));
    public final NumberSetting duration = add(new NumberSetting("Длительность (мс)", 450.0f, 150.0f, 1200.0f, 50.0f));
    public final NumberSetting height = add(new NumberSetting("Высота сдвига", 16.0f, 4.0f, 40.0f, 2.0f));
    public final ModeSetting easing = add(new ModeSetting("Тип кривой", "Экспонента (Expo Out)", "Синус (Sine Out)", "Отскок (Bounce Out)"));

    private final Map<Long, Long> chunkLoadTimes = new ConcurrentHashMap<>();

    public ChunksAnimationModule() {
        super("Chunks Animation", "Плавная кинематографичная анимация появления загружаемых чанков мира", Category.VISUALS);
    }

    public void onChunkLoad(int chunkX, int chunkZ) {
        if (!isEnabled()) return;
        long key = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
        chunkLoadTimes.put(key, System.currentTimeMillis());
    }

    public float getChunkOffsetY(int chunkX, int chunkZ) {
        if (!isEnabled()) return 0.0f;
        long key = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
        Long loadTime = chunkLoadTimes.get(key);
        if (loadTime == null) return 0.0f;

        long elapsed = System.currentTimeMillis() - loadTime;
        float dur = duration.get();
        if (elapsed >= dur) {
            chunkLoadTimes.remove(key);
            return 0.0f;
        }

        float progress = elapsed / dur;
        float eased = applyEasing(progress);
        float h = height.get();

        String m = mode.get();
        if ("Падение сверху (Fall Down)".equals(m)) {
            return h * (1.0f - eased);
        } else {
            // Всплывание снизу
            return -h * (1.0f - eased);
        }
    }

    private float applyEasing(float x) {
        String e = easing.get();
        if ("Синус (Sine Out)".equals(e)) {
            return (float) Math.sin((x * Math.PI) / 2.0);
        } else if ("Отскок (Bounce Out)".equals(e)) {
            float n1 = 7.5625f;
            float d1 = 2.75f;
            if (x < 1 / d1) {
                return n1 * x * x;
            } else if (x < 2 / d1) {
                return n1 * (x -= 1.5f / d1) * x + 0.75f;
            } else if (x < 2.5 / d1) {
                return n1 * (x -= 2.25f / d1) * x + 0.9375f;
            } else {
                return n1 * (x -= 2.625f / d1) * x + 0.984375f;
            }
        }
        // Экспонента
        return x == 1.0f ? 1.0f : (float) (1.0 - Math.pow(2.0, -10.0 * x));
    }
}
