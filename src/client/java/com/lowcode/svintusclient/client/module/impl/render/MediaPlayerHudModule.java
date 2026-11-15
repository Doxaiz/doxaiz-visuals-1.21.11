package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class MediaPlayerHudModule extends Module implements TickListener {

    public final ModeSetting style = add(new ModeSetting("Стиль плеера", "Стеклянная капсула (Glass)", "Минималистичный (Minimal)", "Компактный (Compact Pill)"));
    public final BooleanSetting showProgress = add(new BooleanSetting("Полоса прогресса трека", true));
    public final NumberSetting customX = add(new NumberSetting("Позиция X", 20.0f, 0.0f, 1000.0f, 5.0f));
    public final NumberSetting customY = add(new NumberSetting("Позиция Y", 70.0f, 0.0f, 700.0f, 5.0f));
    public final ColorSetting accentColor = add(new ColorSetting("Цвет акцента", 0xFF1DB954)); // Spotify green

    private String currentTrack = "Doxaiz - Ambient Beats";
    private String currentArtist = "Svintus Lo-Fi";

    public MediaPlayerHudModule() {
        super("Media Player", "Стеклянный медиаплеер с отображением текущего трека (из NewHudJava)", Category.INTERFACE);
    }

    @Override
    public void onEnable() {
        setTrack("Doxaiz - Ambient Beats", "Svintus Lo-Fi");
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
    }

    public void setTrack(String track, String artist) {
        if (track != null) this.currentTrack = track;
        if (artist != null) this.currentArtist = artist;
    }

    public void render(DrawContext context) {
        if (!isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        float x = customX.get();
        float y = customY.get();
        String st = style.get();

        float w = "Компактный (Compact Pill)".equals(st) ? 120f : 145f;
        float h = showProgress.get() ? 32f : 24f;

        if ("Минималистичный (Minimal)".equals(st)) {
            Render2D.rect(context, x, y, w, h, 4f, 0xCC141419);
            Render2D.outline(context, x, y, w, h, 4f, 1f, 0x30FFFFFF);
        } else {
            HudDraw.glass(context, x, y, w, h, 6f);
        }

        // Music note / playback badge
        float iconSize = 16f;
        float iconX = x + 6f;
        float iconY = y + 4f;
        Render2D.rect(context, iconX, iconY, iconSize, iconSize, 3f, 0x25FFFFFF);
        Fonts.REGULAR.draw(context, "▶", iconX + 4.5f, iconY + 4f, 7.0f, accentColor.getEffectiveColor());

        // Track title and artist
        float textX = iconX + iconSize + 6f;
        Fonts.SEMIBOLD.draw(context, Fonts.SEMIBOLD.trim(currentTrack, 6.2f, w - 35f), textX, y + 5.5f, 6.2f, Theme.TEXT);
        Fonts.REGULAR.draw(context, Fonts.REGULAR.trim(currentArtist, 5.2f, w - 35f), textX, y + 14f, 5.2f, Theme.TEXT_MUTE);

        // Progress bar
        if (showProgress.get()) {
            float barX = x + 6f;
            float barY = y + h - 5.5f;
            float barW = w - 12f;
            float barH = 2.0f;

            // Animated fake progress
            float prog = (float) ((System.currentTimeMillis() / 80L) % 240L) / 240.0f;
            Render2D.rect(context, barX, barY, barW, barH, 1.0f, 0x30FFFFFF);
            Render2D.rect(context, barX, barY, barW * prog, barH, 1.0f, accentColor.getEffectiveColor());
        }
    }
}
