package com.lowcode.lowcosmetic.client.gui;

import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class UiFont {
    private static final int[] OVERSAMPLES = {1, 2, 3, 4, 5, 6, 8};

    private UiFont() {
    }

    private static TextRenderer renderer() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private static Style style(boolean bold) {
        return style(bold, UiSettings.font);
    }

    private static Style style(boolean bold, UiSettings.Font font) {
        if (font.id == null) {
            return bold ? Style.EMPTY.withBold(true) : Style.EMPTY;
        }
        double pixels = MinecraftClient.getInstance().getWindow().getScaleFactor() * UiSettings.scale();
        int oversample = OVERSAMPLES[0];
        for (int candidate : OVERSAMPLES) {
            if (Math.abs(candidate - pixels) < Math.abs(oversample - pixels)) {
                oversample = candidate;
            }
        }
        Identifier id = LowcosmeticClient.id(font.id + (bold ? "_bold_o" : "_regular_o") + oversample);
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(id));
    }

    public static Text text(String value, boolean bold) {
        return Text.literal(value).setStyle(style(bold));
    }

    public static Text text(String value, UiSettings.Font font) {
        return Text.literal(value).setStyle(style(false, font));
    }

    public static int width(Text text) {
        return renderer().getWidth(text);
    }

    public static int width(String value) {
        return renderer().getWidth(text(value, false));
    }

    public static int widthBold(String value) {
        return renderer().getWidth(text(value, true));
    }

    public static void draw(DrawContext ctx, String value, float x, float y, int color) {
        draw(ctx, text(value, false), x, y, color);
    }

    public static void drawBold(DrawContext ctx, String value, float x, float y, int color) {
        draw(ctx, text(value, true), x, y, color);
    }

    public static void drawCentered(DrawContext ctx, String value, float cx, float y, int color) {
        draw(ctx, value, cx - width(value) / 2.0F, y, color);
    }

    public static void drawBoldCentered(DrawContext ctx, String value, float cx, float y, int color) {
        drawBold(ctx, value, cx - widthBold(value) / 2.0F, y, color);
    }

    public static void draw(DrawContext ctx, Text text, float x, float y, int color) {
        if ((color >>> 24) < 4) {
            return;
        }
        ctx.drawText(renderer(), text, (int) x, (int) y, color, false);
    }

    public static String ellipsize(String value, float maxWidth, boolean bold) {
        if ((bold ? widthBold(value) : width(value)) <= maxWidth) {
            return value;
        }
        String result = value;
        while (!result.isEmpty()) {
            result = result.substring(0, result.length() - 1);
            String candidate = result.stripTrailing() + "…";
            if ((bold ? widthBold(candidate) : width(candidate)) <= maxWidth) {
                return candidate;
            }
        }
        return "…";
    }

    public static String tail(String value, float maxWidth) {
        String result = value;
        while (!result.isEmpty() && width(result) > maxWidth) {
            result = result.substring(1);
        }
        return result;
    }
}
