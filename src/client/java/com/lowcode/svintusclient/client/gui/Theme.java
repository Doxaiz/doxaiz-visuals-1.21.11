package com.lowcode.svintusclient.client.gui;

import com.lowcode.svintusclient.client.util.Anim;
import com.lowcode.svintusclient.client.util.Colors;
import com.lowcode.svintusclient.client.util.I18n;

public final class Theme {

    public enum Accent {
        PERIWINKLE("Перивинкл", "Periwinkle", 0xFF8C9EFF, 0xFFB69CFF),
        ROSE("Роза", "Rose", 0xFFE5737F, 0xFFF29E7C),
        VIOLET("Фиалка", "Violet", 0xFF8A72F2, 0xFFCB7CF4),
        MINT("Мята", "Mint", 0xFF5DD39E, 0xFF5BC0E8),
        SILVER("Серебро", "Silver", 0xFFC2C6D0, 0xFF8D93A3);

        public final String titleRu;
        public final String titleEn;
        public final int primary;
        public final int secondary;

        Accent(String titleRu, String titleEn, int primary, int secondary) {
            this.titleRu = titleRu;
            this.titleEn = titleEn;
            this.primary = primary;
            this.secondary = secondary;
        }

        public String getDisplayName() {
            return I18n.isEnglish() ? titleEn : titleRu;
        }
    }

    public enum ThemePreset {
        OBSIDIAN("Обсидиан", "Obsidian", 0xF50D0E13, 0xF508090C, 0xA0000000, 0x0EFFFFFF, 0x16FFFFFF),
        TWILIGHT("Сумерки", "Twilight", 0xF513101C, 0xF50B0813, 0xB0080512, 0x14FFFFFF, 0x1CFFFFFF),
        CYBERPUNK("Киберпанк", "Cyberpunk", 0xF50A101C, 0xF5060A12, 0xB0040810, 0x12FFFFFF, 0x1AFFFFFF),
        AURORA("Аврора", "Aurora", 0xF50A1814, 0xF5060F0C, 0xB0030B08, 0x12FFFFFF, 0x1AFFFFFF),
        CLASSIC_DARK("Классика", "Classic Dark", 0xF5111218, 0xF50B0C10, 0xA0000000, 0x0CFFFFFF, 0x14FFFFFF);

        public final String titleRu;
        public final String titleEn;
        public final int windowTop;
        public final int windowBottom;
        public final int dimBackdrop;
        public final int cardBg;
        public final int cardHoverBg;

        ThemePreset(String titleRu, String titleEn, int windowTop, int windowBottom, int dimBackdrop, int cardBg, int cardHoverBg) {
            this.titleRu = titleRu;
            this.titleEn = titleEn;
            this.windowTop = windowTop;
            this.windowBottom = windowBottom;
            this.dimBackdrop = dimBackdrop;
            this.cardBg = cardBg;
            this.cardHoverBg = cardHoverBg;
        }

        public String getDisplayName() {
            return I18n.isEnglish() ? titleEn : titleRu;
        }
    }

    public enum Style {
        GLASS_PILL("Капсульное стекло", "Glass Pill", 12f, 1.0f, true, "Сглаженные капсульные формы с матовым обсидиановым стеклом"),
        NEON_GLOW("Неоновый контур", "Neon Glow", 10f, 1.2f, true, "Мягкое неоновое свечение и яркий контур по периметру карточек"),
        MINIMAL_CLEAN("Минимализм", "Minimal Clean", 6f, 0.7f, false, "Ультратонкие изящные рамки без лишнего фона для чистого обзора"),
        ACRYLIC_GLASS("Акрил", "Acrylic Glass", 14f, 1.1f, true, "Глубокое полупрозрачное акриловое стекло с рассеиванием света");

        public final String titleRu;
        public final String titleEn;
        public final float cardRadius;
        public final float borderWidth;
        public final boolean glow;
        public final String desc;

        Style(String titleRu, String titleEn, float cardRadius, float borderWidth, boolean glow, String desc) {
            this.titleRu = titleRu;
            this.titleEn = titleEn;
            this.cardRadius = cardRadius;
            this.borderWidth = borderWidth;
            this.glow = glow;
            this.desc = desc;
        }

        public String getDisplayName() {
            return I18n.isEnglish() ? titleEn : titleRu;
        }
    }

    public static final int SURFACE_RAISED = 0xFF1E1F27;
    public static final int TRACK = 0xFF23242C;
    public static final int TEXT = 0xFFE9EAF1;
    public static final int TEXT_DIM = 0xFF8D8F9B;
    public static final int TEXT_MUTE = 0xFF555762;

    private static Accent current = Accent.PERIWINKLE;
    private static ThemePreset currentPreset = ThemePreset.CLASSIC_DARK;
    private static Style currentStyle = Style.GLASS_PILL;

    private static int fromPrimary = current.primary;
    private static int fromSecondary = current.secondary;
    private static final Anim BLEND = new Anim(1f, 8f);

    private Theme() {
    }

    public static Accent current() {
        return current;
    }

    public static ThemePreset preset() {
        return currentPreset;
    }

    public static void setPreset(ThemePreset preset) {
        if (preset != null) currentPreset = preset;
    }

    public static Style style() {
        return currentStyle;
    }

    public static void setStyle(Style style) {
        if (style != null) currentStyle = style;
    }

    public static void setInstant(Accent accent) {
        if (accent == null) return;
        current = accent;
        fromPrimary = accent.primary;
        fromSecondary = accent.secondary;
        BLEND.snap(1f);
    }

    public static void set(Accent accent) {
        if (accent == null || accent == current) return;
        fromPrimary = accent();
        fromSecondary = accent2();
        current = accent;
        BLEND.snap(0f).to(1f);
    }

    public static int accent() {
        return Colors.lerp(fromPrimary, current.primary, BLEND.get());
    }

    public static int accent2() {
        return Colors.lerp(fromSecondary, current.secondary, BLEND.get());
    }

    public static int accent(float alpha) {
        return Colors.withAlpha(accent(), alpha);
    }

    public static int tint(int base, float amount) {
        return Colors.lerp(base, accent() | 0xFF000000, amount) & 0x00FFFFFF | (base & 0xFF000000);
    }

    public static int windowTop() {
        return currentPreset.windowTop;
    }

    public static int windowBottom() {
        return currentPreset.windowBottom;
    }

    public static int dimBackdrop() {
        return currentPreset.dimBackdrop;
    }

    public static int cardBg() {
        return currentPreset.cardBg;
    }

    public static int cardHoverBg() {
        return currentPreset.cardHoverBg;
    }

    public static float cardRadius() {
        return currentStyle.cardRadius;
    }

    public static float borderWidth() {
        return currentStyle.borderWidth;
    }

    public static boolean hasGlow() {
        return currentStyle.glow;
    }
}
