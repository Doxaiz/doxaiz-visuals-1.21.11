package com.lowcode.lowcosmetic.client.gui;

public final class Theme {
    public static final int TEXT = 0xFFF4F4F8;
    public static final int TEXT_DIM = 0xFFA6A6BC;
    public static final int TEXT_MUTED = 0xFF6C6C84;
    public static final int DANGER = 0xFFF87171;
    public static final int GOLD = 0xFFFBBF24;
    public static final int WINDOW_TOP = 0xF4161622;
    public static final int WINDOW_BOTTOM = 0xF40E0E15;
    public static final int SURFACE = 0xFF1B1B26;
    public static final int SURFACE_HOVER = 0xFF242433;
    public static final int WELL = 0xFF15151E;
    public static final int STROKE = 0x18FFFFFF;

    private Theme() {
    }

    public static int accent() {
        return UiSettings.accent.color;
    }

    public static int accent(float alpha) {
        return Render2D.alpha(accent(), alpha);
    }

    public static int accentLight() {
        return Render2D.lerpColor(accent(), 0xFFFFFFFF, 0.28F);
    }

    public static int accentDark() {
        return Render2D.lerpColor(accent(), 0xFF000000, 0.25F);
    }

    public static int tint(int base, float amount) {
        return Render2D.lerpColor(base, accent() | 0xFF000000, amount) & 0x00FFFFFF | (base & 0xFF000000);
    }
}
