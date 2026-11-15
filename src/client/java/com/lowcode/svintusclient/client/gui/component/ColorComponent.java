package com.lowcode.svintusclient.client.gui.component;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.gui.Widgets;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.util.Colors;
import net.minecraft.client.gui.DrawContext;

public class ColorComponent extends SettingComponent {

    private static final float PILL_W = 48f;
    private static final float PILL_H = 15f;
    private static final float TEXT_SIZE = 5.8f;

    private static final int[] PRESET_PALETTE = {
            0xFFFF2A6D, 0xFF05D9E8, 0xFF00FF9F, 0xFFFFB800, 0xFF9D4EDD, 0xFFFFFFFF
    };
    private int paletteIndex = 0;

    private final ColorSetting setting;

    public ColorComponent(ColorSetting setting) {
        this.setting = setting;
    }

    @Override
    public float height() {
        return 24f;
    }

    @Override
    public void render(DrawContext ctx, float mx, float my) {
        float h = drawCard(ctx, mx, my);
        Fonts.REGULAR.drawCentered(ctx, com.lowcode.svintusclient.client.util.I18n.get(setting.getName()), x + PAD, y + height() / 2f, LABEL_SIZE,
                Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, h * 0.7f));

        float px = x + w - PAD - PILL_W;
        float py = y + (height() - PILL_H) / 2f;

        int color = setting.getEffectiveColor();
        Render2D.rect(ctx, px, py, PILL_W, PILL_H, 4f, 0x30000000);
        Render2D.rect(ctx, px + 2f, py + 2f, PILL_W - 4f, PILL_H - 4f, 3f, color);
        Render2D.outline(ctx, px, py, PILL_W, PILL_H, 4f, 0.8f, 0x33FFFFFF);

        String tag = setting.isRainbow() ? "RGB" : String.format("#%06X", (color & 0x00FFFFFF));
        Fonts.SEMIBOLD.drawCentered(ctx, tag, px + (PILL_W - Fonts.SEMIBOLD.width(tag, TEXT_SIZE)) / 2f, py + PILL_H / 2f, TEXT_SIZE, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(float mx, float my, int button) {
        float px = x + w - PAD - PILL_W;
        float py = y + (height() - PILL_H) / 2f;
        if (!Widgets.inside(mx, my, px, py, PILL_W, PILL_H)) return false;

        if (button == 1) {
            setting.setRainbow(!setting.isRainbow());
            return true;
        } else if (button == 0) {
            if (setting.isRainbow()) {
                setting.setRainbow(false);
            } else {
                paletteIndex = (paletteIndex + 1) % (PRESET_PALETTE.length + 1);
                if (paletteIndex == PRESET_PALETTE.length) {
                    setting.setRainbow(true);
                } else {
                    setting.set(PRESET_PALETTE[paletteIndex]);
                }
            }
            return true;
        }
        return false;
    }
}
