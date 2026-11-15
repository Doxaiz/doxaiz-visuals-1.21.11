package com.lowcode.svintusclient.client.gui;

import com.lowcode.svintusclient.client.Config;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.client.ClickGuiModule;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.render.icon.Icons;
import com.lowcode.svintusclient.client.util.Anim;
import com.lowcode.svintusclient.client.util.Colors;
import com.lowcode.svintusclient.client.util.I18n;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public final class PreferencesPanel {

    private final Anim scroll = new Anim(16f);
    private float contentH = 0f;

    // Interactive slider dragging state: 0 = none, 1 = glass, 2 = dim, 3 = scale, 4 = theme preset
    private int draggingSlider = 0;

    @FunctionalInterface
    private interface ClickConsumer {
        void accept(float x, float y);
    }

    private static final class ClickArea {
        final float x, y, w, h;
        final ClickConsumer action;

        ClickArea(float x, float y, float w, float h, ClickConsumer action) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.action = action;
        }

        ClickArea(float x, float y, float w, float h, Runnable action) {
            this(x, y, w, h, (cx, cy) -> action.run());
        }

        boolean contains(float mx, float my) {
            return Widgets.inside(mx, my, x, y, w, h);
        }
    }

    private final List<ClickArea> clickAreas = new ArrayList<>();

    // Slider track bounds for mouseDragged
    private float sliderTrackX, sliderTrackW;
    private float glassTrackY, dimTrackY, scaleTrackY;
    private float themeTrackX, themeTrackY, themeTrackW;

    public void render(DrawContext ctx, float x, float y, float w, float h, float mx, float my, boolean inBody) {
        clickAreas.clear();

        ClickGuiModule guiMod = ModuleManager.CLICK_GUI;

        float maxScroll = Math.max(0f, contentH - h);
        float currentScroll = clamp(scroll.get(), 0f, maxScroll);

        Render2D.pushScissor(ctx, x - 4f, y, w + 8f, h);

        float curY = y - currentScroll + 6f;
        float cardGap = 8f;
        float cardW = w - 8f;
        float cardX = x + 4f;

        // 1. Language Card
        curY = drawLanguageCard(ctx, cardX, curY, cardW, mx, my, inBody) + cardGap;

        // 2. Theme Preset Card
        curY = drawThemesCard(ctx, cardX, curY, cardW, mx, my, inBody) + cardGap;

        // 3. Accent Color Card
        curY = drawAccentsCard(ctx, cardX, curY, cardW, mx, my, inBody) + cardGap;

        // 4. Style Card
        curY = drawStylesCard(ctx, cardX, curY, cardW, mx, my, inBody) + cardGap;

        // 5. Click GUI Parameters Card
        if (guiMod != null) {
            curY = drawGuiParamsCard(ctx, cardX, curY, cardW, mx, my, inBody, guiMod) + cardGap;
        }

        Render2D.popScissor(ctx);

        contentH = (curY + currentScroll) - (y + 6f);

        // Scrollbar if needed
        if (maxScroll > 1f) {
            float trackX = x + w - 4f;
            float trackY = y + 4f;
            float trackH = h - 8f;
            float thumbH = Math.max(20f, trackH * h / contentH);
            float thumbY = trackY + (trackH - thumbH) * (currentScroll / maxScroll);

            Render2D.rect(ctx, trackX, trackY, 3f, trackH, 1.5f, 0x14FFFFFF);
            Render2D.rect(ctx, trackX, thumbY, 3f, thumbH, 1.5f, Theme.accent(0.6f));
        }
    }

    private float drawLanguageCard(DrawContext ctx, float cx, float cy, float cw, float mx, float my, boolean inBody) {
        float ch = 64f;
        drawCardBg(ctx, cx, cy, cw, ch);

        drawCardHeader(ctx, cx, cy, I18n.get("Язык интерфейса"), I18n.get("Выберите язык для всех элементов интерфейса"));

        // Language Buttons
        float btnY = cy + 28f;
        float btnH = 24f;
        float btnW = (cw - 32f) / 2f;

        I18n.Language currentLang = I18n.getLanguage();

        // Russian Button
        float ruX = cx + 12f;
        boolean ruActive = currentLang == I18n.Language.RU;
        boolean ruHover = inBody && Widgets.inside(mx, my, ruX, btnY, btnW, btnH);
        drawOptionPill(ctx, ruX, btnY, btnW, btnH, "Русский", "RU", ruActive, ruHover);
        clickAreas.add(new ClickArea(ruX, btnY, btnW, btnH, () -> {
            I18n.setLanguage(I18n.Language.RU);
            Config.save();
        }));

        // English Button
        float enX = cx + 12f + btnW + 8f;
        boolean enActive = currentLang == I18n.Language.EN;
        boolean enHover = inBody && Widgets.inside(mx, my, enX, btnY, btnW, btnH);
        drawOptionPill(ctx, enX, btnY, btnW, btnH, "English", "EN", enActive, enHover);
        clickAreas.add(new ClickArea(enX, btnY, btnW, btnH, () -> {
            I18n.setLanguage(I18n.Language.EN);
            Config.save();
        }));

        return cy + ch;
    }

    private float drawThemesCard(DrawContext ctx, float cx, float cy, float cw, float mx, float my, boolean inBody) {
        Theme.ThemePreset[] presets = Theme.ThemePreset.values();
        float ch = 70f;
        drawCardBg(ctx, cx, cy, cw, ch);

        Theme.ThemePreset activePreset = Theme.preset();
        int activeIdx = activePreset.ordinal();

        drawCardHeader(ctx, cx, cy, I18n.get("Тема оформления"), I18n.get("Цветовая палитра окна и элементов интерфейса"));

        // Current theme preview badge on header right
        String activeName = activePreset.getDisplayName();
        float badgeW = Fonts.SEMIBOLD.width(activeName, 5.4f) + 26f;
        float badgeH = 15f;
        float badgeX = cx + cw - 12f - badgeW;
        float badgeY = cy + 7f;
        Render2D.rect(ctx, badgeX, badgeY, badgeW, badgeH, 4f, 0x1AFFFFFF);
        Render2D.outline(ctx, badgeX, badgeY, badgeW, badgeH, 4f, 0.8f, Theme.accent(0.65f));
        Render2D.gradientV(ctx, badgeX + 4f, badgeY + 3.5f, 12f, 8f, 2f, activePreset.windowTop, activePreset.windowBottom);
        Fonts.SEMIBOLD.draw(ctx, activeName, badgeX + 20f, badgeY + 4f, 5.4f, Theme.TEXT);

        // Slider track
        themeTrackX = cx + 16f;
        themeTrackW = cw - 32f;
        themeTrackY = cy + 34f;
        float trackH = 4f;

        // Background track
        Render2D.rect(ctx, themeTrackX, themeTrackY, themeTrackW, trackH, 2f, 0x18FFFFFF);

        // Filled active track
        float progress = (float) activeIdx / (float) (presets.length - 1);
        Render2D.rect(ctx, themeTrackX, themeTrackY, themeTrackW * progress, trackH, 2f, Theme.accent());

        // Step notches and preset titles
        for (int i = 0; i < presets.length; i++) {
            float tickX = themeTrackX + (themeTrackW * i) / (presets.length - 1);
            boolean isStepActive = i == activeIdx;
            boolean isPassed = i <= activeIdx;

            Render2D.circle(ctx, tickX, themeTrackY + trackH / 2f, 2.5f, isPassed ? Theme.accent() : 0x40FFFFFF);

            String name = presets[i].getDisplayName();
            float nw = Fonts.REGULAR.width(name, 5.2f);
            float labelX = tickX - nw / 2f;
            labelX = Math.max(cx + 8f, Math.min(cx + cw - 8f - nw, labelX));
            int textCol = isStepActive ? Theme.TEXT : (inBody && Widgets.inside(mx, my, tickX - 16f, themeTrackY - 4f, 32f, 28f) ? Theme.TEXT_DIM : Theme.TEXT_MUTE);
            Fonts.REGULAR.draw(ctx, name, labelX, themeTrackY + 11f, 5.2f, textCol);
        }

        // Knob
        float knobX = themeTrackX + themeTrackW * progress;
        float knobY = themeTrackY + trackH / 2f;
        Render2D.circle(ctx, knobX, knobY, 5.5f, 0xFFFFFFFF);
        Render2D.circleOutline(ctx, knobX, knobY, 5.5f, 1.0f, Theme.accent());

        // Interactive area for dragging / clicking
        clickAreas.add(new ClickArea(themeTrackX - 8f, themeTrackY - 10f, themeTrackW + 16f, 32f, (clickX, clickY) -> {
            draggingSlider = 4;
            updateSliderValue(clickX);
        }));

        return cy + ch;
    }

    private float drawAccentsCard(DrawContext ctx, float cx, float cy, float cw, float mx, float my, boolean inBody) {
        Theme.Accent[] accents = Theme.Accent.values();
        float ch = 76f;
        drawCardBg(ctx, cx, cy, cw, ch);

        drawCardHeader(ctx, cx, cy, I18n.get("Акцентный цвет"), I18n.get("Основной цвет кнопок, тумблеров и свечения"));

        float gap = 6f;
        float chipW = (cw - 24f - (accents.length - 1) * gap) / accents.length;
        float chipH = 36f;
        float chipY = cy + 28f;

        Theme.Accent activeAccent = Theme.current();

        for (int i = 0; i < accents.length; i++) {
            Theme.Accent a = accents[i];
            float ax = cx + 12f + i * (chipW + gap);
            boolean active = activeAccent == a;
            boolean hover = inBody && Widgets.inside(mx, my, ax, chipY, chipW, chipH);

            Render2D.rect(ctx, ax, chipY, chipW, chipH, 6f, active ? 0x2AFFFFFF : (hover ? 0x18FFFFFF : 0x0EFFFFFF));
            if (active) {
                Render2D.outline(ctx, ax, chipY, chipW, chipH, 6f, 1.2f, a.primary | 0xFF000000);
            } else {
                Render2D.outline(ctx, ax, chipY, chipW, chipH, 6f, 0.7f, hover ? 0x24FFFFFF : 0x10FFFFFF);
            }

            // Dot circle at top center of chip
            float dotCenterX = ax + chipW / 2f;
            float dotCenterY = chipY + 11f;
            if (active) {
                Render2D.circleOutline(ctx, dotCenterX, dotCenterY, 6.5f, 1.0f, a.primary | 0xFF000000);
            }
            Render2D.circle(ctx, dotCenterX, dotCenterY, 4.5f, a.primary | 0xFF000000);

            // Title centered horizontally at bottom of chip
            int textCol = active ? Theme.TEXT : (hover ? Theme.TEXT_DIM : Theme.TEXT_MUTE);
            String title = a.getDisplayName();
            float tw = Fonts.SEMIBOLD.width(title, 5.0f);
            Fonts.SEMIBOLD.drawCentered(ctx, title, ax + (chipW - tw) / 2f, chipY + 25f, 5.0f, textCol);

            clickAreas.add(new ClickArea(ax, chipY, chipW, chipH, () -> {
                Theme.set(a);
                Config.save();
            }));
        }

        return cy + ch;
    }

    private float drawStylesCard(DrawContext ctx, float cx, float cy, float cw, float mx, float my, boolean inBody) {
        Theme.Style[] styles = Theme.Style.values();
        float ch = 76f;
        drawCardBg(ctx, cx, cy, cw, ch);

        drawCardHeader(ctx, cx, cy, I18n.get("Стиль оформления"), I18n.get("Геометрия карточек, скругления и границы"));

        float chipY = cy + 28f;
        float chipH = 36f;
        float gap = 6f;
        float chipW = (cw - 24f - (styles.length - 1) * gap) / styles.length;

        Theme.Style activeStyle = Theme.style();

        for (int i = 0; i < styles.length; i++) {
            Theme.Style s = styles[i];
            float sx = cx + 12f + i * (chipW + gap);
            boolean active = activeStyle == s;
            boolean hover = inBody && Widgets.inside(mx, my, sx, chipY, chipW, chipH);

            Render2D.rect(ctx, sx, chipY, chipW, chipH, 6f, active ? 0x2AFFFFFF : (hover ? 0x1AFFFFFF : 0x10FFFFFF));
            if (active) {
                Render2D.outline(ctx, sx, chipY, chipW, chipH, 6f, 1.2f, Theme.accent(0.85f));
            } else {
                Render2D.outline(ctx, sx, chipY, chipW, chipH, 6f, 0.7f, hover ? 0x24FFFFFF : 0x12FFFFFF);
            }

            // Geometry preview box
            float prevX = sx + (chipW - 24f) / 2f;
            float prevY = chipY + 4f;
            Render2D.rect(ctx, prevX, prevY, 24f, 12f, s.cardRadius * 0.4f, 0x1CFFFFFF);
            Render2D.outline(ctx, prevX, prevY, 24f, 12f, s.cardRadius * 0.4f, s.borderWidth * 0.8f, active ? Theme.accent() : 0x40FFFFFF);

            // Title
            int textCol = active ? Theme.TEXT : (hover ? Theme.TEXT_DIM : Theme.TEXT_MUTE);
            Fonts.SEMIBOLD.drawCentered(ctx, s.getDisplayName(), sx + chipW / 2f - Fonts.SEMIBOLD.width(s.getDisplayName(), 5.2f) / 2f, prevY + 18f, 5.2f, textCol);

            clickAreas.add(new ClickArea(sx, chipY, chipW, chipH, () -> {
                Theme.setStyle(s);
                Config.save();
            }));
        }

        return cy + ch;
    }

    private float drawGuiParamsCard(DrawContext ctx, float cx, float cy, float cw, float mx, float my, boolean inBody, ClickGuiModule guiMod) {
        float ch = 104f;
        drawCardBg(ctx, cx, cy, cw, ch);

        drawCardHeader(ctx, cx, cy, I18n.get("Параметры Click GUI"), I18n.get("Эффекты размытия, стекло и масштаб окна"));

        sliderTrackW = Math.min(130f, Math.max(80f, cw * 0.35f));
        sliderTrackX = cx + cw - 12f - sliderTrackW;

        // Row 1: Blur toggle
        float r1Y = cy + 28f;
        Fonts.REGULAR.drawCentered(ctx, I18n.get("Размытие окна"), cx + 12f, r1Y + 7f, 6.2f, Theme.TEXT);
        float switchX = cx + cw - 12f - Widgets.SWITCH_W;
        float switchY = r1Y + 1f;
        boolean blurVal = guiMod.blur.get();
        Widgets.toggle(ctx, switchX, switchY, blurVal ? 1f : 0f, 0f);
        clickAreas.add(new ClickArea(switchX - 4f, switchY - 2f, Widgets.SWITCH_W + 8f, Widgets.SWITCH_H + 4f, () -> {
            guiMod.blur.set(!guiMod.blur.get());
            Config.save();
        }));

        // Row 2: Glass opacity slider
        float r2Y = cy + 46f;
        glassTrackY = r2Y + 4f;
        float glassVal = guiMod.glass.get();
        drawInteractiveSlider(ctx, cx + 12f, r2Y, sliderTrackX, glassTrackY, sliderTrackW,
                I18n.get("Плотность стекла"), String.format("%.0f%%", glassVal * 100f),
                (glassVal - guiMod.glass.min) / (guiMod.glass.max - guiMod.glass.min), mx, my, inBody, 1);

        // Row 3: Dimming slider
        float r3Y = cy + 64f;
        dimTrackY = r3Y + 4f;
        float dimVal = guiMod.dim.get();
        drawInteractiveSlider(ctx, cx + 12f, r3Y, sliderTrackX, dimTrackY, sliderTrackW,
                I18n.get("Затемнение фона"), String.format("%.0f%%", dimVal * 100f),
                (dimVal - guiMod.dim.min) / (guiMod.dim.max - guiMod.dim.min), mx, my, inBody, 2);

        // Row 4: Scale slider
        float r4Y = cy + 82f;
        scaleTrackY = r4Y + 4f;
        float scaleVal = guiMod.scale.get();
        drawInteractiveSlider(ctx, cx + 12f, r4Y, sliderTrackX, scaleTrackY, sliderTrackW,
                I18n.get("Масштаб GUI"), String.format("%.0f%%", scaleVal * 100f),
                (scaleVal - guiMod.scale.min) / (guiMod.scale.max - guiMod.scale.min), mx, my, inBody, 3);

        return cy + ch;
    }

    private void drawInteractiveSlider(DrawContext ctx, float lx, float ly, float tx, float ty, float tw,
                                        String label, String valueText, float progress, float mx, float my, boolean inBody, int sliderId) {
        Fonts.REGULAR.drawCentered(ctx, label, lx, ly + 6f, 6.0f, Theme.TEXT_DIM);

        float trackH = 4f;
        Render2D.rect(ctx, tx, ty, tw, trackH, 2f, 0x18FFFFFF);
        Render2D.rect(ctx, tx, ty, tw * progress, trackH, 2f, Theme.accent());

        float knobX = tx + tw * progress;
        float knobY = ty + trackH / 2f;
        Render2D.circle(ctx, knobX, knobY, 4.5f, 0xFFFFFFFF);
        Render2D.circleOutline(ctx, knobX, knobY, 4.5f, 0.8f, Theme.accent());

        // Value text
        Fonts.SEMIBOLD.drawCentered(ctx, valueText, tx - Fonts.SEMIBOLD.width(valueText, 5.6f) - 6f, ly + 6f, 5.6f, Theme.TEXT_DIM);

        clickAreas.add(new ClickArea(tx - 6f, ty - 8f, tw + 12f, 20f, (clickX, clickY) -> {
            draggingSlider = sliderId;
            updateSliderValue(clickX);
        }));
    }

    private void updateSliderValue(float mx) {
        if (draggingSlider == 4) { // Theme preset slider
            Theme.ThemePreset[] presets = Theme.ThemePreset.values();
            if (themeTrackW > 0f) {
                float pct = clamp((mx - themeTrackX) / themeTrackW, 0f, 1f);
                int idx = Math.min(presets.length - 1, Math.max(0, Math.round(pct * (presets.length - 1))));
                if (Theme.preset() != presets[idx]) {
                    Theme.setPreset(presets[idx]);
                }
            }
            return;
        }

        ClickGuiModule guiMod = ModuleManager.CLICK_GUI;
        if (guiMod == null || sliderTrackW <= 0f) return;

        float pct = clamp((mx - sliderTrackX) / sliderTrackW, 0f, 1f);
        if (draggingSlider == 1) { // Glass
            float val = guiMod.glass.min + pct * (guiMod.glass.max - guiMod.glass.min);
            guiMod.glass.set(Math.round(val * 50f) / 50f);
        } else if (draggingSlider == 2) { // Dim
            float val = guiMod.dim.min + pct * (guiMod.dim.max - guiMod.dim.min);
            guiMod.dim.set(Math.round(val * 20f) / 20f);
        } else if (draggingSlider == 3) { // Scale
            float val = guiMod.scale.min + pct * (guiMod.scale.max - guiMod.scale.min);
            guiMod.scale.set(Math.round(val * 20f) / 20f);
        }
    }

    private void drawCardBg(DrawContext ctx, float x, float y, float w, float h) {
        float r = Theme.cardRadius();
        Render2D.rect(ctx, x, y, w, h, r, Theme.cardBg());
        Render2D.outline(ctx, x, y, w, h, r, Theme.borderWidth(), 0x18FFFFFF);
    }

    private void drawCardHeader(DrawContext ctx, float x, float y, String title, String subtitle) {
        Fonts.SEMIBOLD.draw(ctx, title, x + 12f, y + 8f, 7.2f, Theme.TEXT);
        Fonts.REGULAR.draw(ctx, subtitle, x + 12f, y + 17f, 5.4f, Theme.TEXT_MUTE);
    }

    private void drawOptionPill(DrawContext ctx, float px, float py, float pw, float ph, String title, String badge, boolean active, boolean hover) {
        Render2D.rect(ctx, px, py, pw, ph, 6f, active ? 0x2AFFFFFF : (hover ? 0x18FFFFFF : 0x10FFFFFF));
        if (active) {
            Render2D.outline(ctx, px, py, pw, ph, 6f, 1.2f, Theme.accent(0.85f));
        } else {
            Render2D.outline(ctx, px, py, pw, ph, 6f, 0.7f, hover ? 0x24FFFFFF : 0x12FFFFFF);
        }

        // Badge pill
        float bW = 20f;
        float bH = 14f;
        float bX = px + 6f;
        float bY = py + (ph - bH) / 2f;
        Render2D.rect(ctx, bX, bY, bW, bH, 3f, active ? Theme.accent(0.35f) : 0x14FFFFFF);
        Fonts.SEMIBOLD.drawCentered(ctx, badge, bX + bW / 2f - Fonts.SEMIBOLD.width(badge, 5.2f) / 2f, bY + 3.5f, 5.2f, active ? Theme.accent() : Theme.TEXT_DIM);

        // Title
        int textCol = active ? Theme.TEXT : (hover ? Theme.TEXT_DIM : Theme.TEXT_MUTE);
        Fonts.SEMIBOLD.drawCentered(ctx, title, bX + bW + 8f, py + ph / 2f, 6.2f, textCol);
    }

    public boolean mouseClicked(float mx, float my, int button, boolean inBody) {
        if (!inBody || button != 0) return false;
        for (ClickArea area : clickAreas) {
            if (area.contains(mx, my)) {
                area.action.accept(mx, my);
                return true;
            }
        }
        return false;
    }

    public void mouseReleased() {
        if (draggingSlider != 0) {
            draggingSlider = 0;
            Config.save();
        }
    }

    public boolean mouseDragged(float mx, float my, int button, float dx, float dy) {
        if (draggingSlider != 0) {
            updateSliderValue(mx);
            return true;
        }
        return false;
    }

    public void mouseScrolled(float mx, float my, double h, double v, float bodyH) {
        float maxScroll = Math.max(0f, contentH - bodyH);
        float next = clamp(scroll.get() - (float) v * 20f, 0f, maxScroll);
        scroll.to(next);
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
