package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.holyworld.HolyWorldApiService;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public final class EventsModule extends Module implements TickListener {

    public final BooleanSetting showHud = add(new BooleanSetting("Отображать в HUD", true));
    public final NumberSetting maxEvents = add(new NumberSetting("Максимум событий", 4.0f, 1.0f, 10.0f, 1.0f));
    public final BooleanSetting autoScroll = add(new BooleanSetting("Анимация прокрутки", true));
    public final BooleanSetting soundNotification = add(new BooleanSetting("Звуковое уведомление", true));

    private int lastCount = 0;

    public EventsModule() {
        super("HolyWorld Events", "Отображение активных ивентов сервера HolyWorld (Аирдропы, Боссы, Мистики)", Category.HOLYWORLD);
    }

    @Override
    public void onEnable() {
        HolyWorldApiService.startPolling();
        lastCount = HolyWorldApiService.getEvents().size();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        int c = HolyWorldApiService.getEvents().size();
        if (soundNotification.get() && c > lastCount && lastCount > 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.2f);
            }
        }
        lastCount = c;
    }

    public void renderHud(DrawContext context, float x, float y) {
        if (!isEnabled() || !showHud.get()) return;

        List<HolyWorldApiService.HwEvent> events = HolyWorldApiService.getEvents();
        float w = 150f;
        float headerH = 14f;
        int max = maxEvents.get().intValue();
        int count = Math.min(events.size(), max);
        float h = headerH + (count == 0 ? 14f : count * 12f + 4f);

        Render2D.rect(context, x, y, w, h, 6f, 0xAA111422);
        Render2D.outline(context, x, y, w, h, 6f, 0.8f, 0x44FFFFFF);
        Fonts.SEMIBOLD.draw(context, "HolyWorld Ивенты", x + 6f, y + 3.5f, 5.8f, 0xFF4FC3F7);

        if (!HolyWorldApiService.isApiOnline) {
            Fonts.REGULAR.draw(context, "API: Offline / Поиск...", x + 6f, y + headerH + 2f, 5.2f, 0xFF888888);
            return;
        }

        if (events.isEmpty()) {
            Fonts.REGULAR.draw(context, "Нет активных событий", x + 6f, y + headerH + 2f, 5.2f, 0xFFAAAAAA);
            return;
        }

        float curY = y + headerH + 2f;
        for (int i = 0; i < count; i++) {
            HolyWorldApiService.HwEvent ev = events.get(i);
            String title = ev.name();
            if (autoScroll.get() && Fonts.REGULAR.width(title, 5.2f) > w - 18f) {
                int shift = (int) ((System.currentTimeMillis() / 400L) % (title.length() + 3));
                String rotated = title + "   " + title;
                if (shift < rotated.length()) {
                    title = rotated.substring(shift);
                }
            }
            Fonts.REGULAR.draw(context, "• " + Fonts.REGULAR.trim(title, 5.2f, w - 14f), x + 6f, curY, 5.2f, 0xFFEEEEEE);
            curY += 12f;
        }
    }
}
