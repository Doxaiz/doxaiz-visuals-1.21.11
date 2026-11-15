package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.holyworld.HolyWorldApiService;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.gui.DrawContext;

public final class WipeTrackerModule extends Module {

    public final BooleanSetting showBadge = add(new BooleanSetting("Плашка вайпа в HUD", true));
    public final BooleanSetting compactMode = add(new BooleanSetting("Компактный режим", false));
    public final NumberSetting manualDays = add(new NumberSetting("Дней с вайпа (ручной)", 14.0f, 0.0f, 90.0f, 1.0f));
    public final BooleanSetting autoFetchApi = add(new BooleanSetting("Синхронизация с API", true));
    public final NumberSetting badgeAlpha = add(new NumberSetting("Прозрачность плашки", 0.85f, 0.2f, 1.0f, 0.05f));

    public WipeTrackerModule() {
        super("Wipe Tracker", "Отображение количества дней с последнего вайпа текущей анархии HolyWorld", Category.HOLYWORLD);
    }

    @Override
    public void onEnable() {
        if (autoFetchApi.get()) {
            HolyWorldApiService.startPolling();
        }
    }

    public void renderHud(DrawContext context, float x, float y) {
        if (!isEnabled() || !showBadge.get()) return;

        int days = manualDays.get().intValue();
        if (autoFetchApi.get() && HolyWorldApiService.isApiOnline && HolyWorldApiService.daysSinceWipe > 0) {
            days = HolyWorldApiService.daysSinceWipe;
        }
        int bgAlpha = (int) (badgeAlpha.get() * 255f);
        int bg = (bgAlpha << 24) | 0x111422;
        boolean compact = compactMode.get();
        float w = compact ? 70f : 110f;
        float h = 20f;

        Render2D.rect(context, x, y, w, h, 5f, bg);
        Render2D.outline(context, x, y, w, h, 5f, 0.8f, 0x44FFFFFF);

        String text = compact ? ("Вайп: " + days + " дн.") : ("Дней с вайпа: " + days);
        Fonts.SEMIBOLD.draw(context, text, x + 6f, y + 6f, 5.4f, 0xFF00CEC9);
    }
}
