package com.lowcode.lowcosmetic.client.gui;

import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class Render2D {
    private static final Identifier DISC = LowcosmeticClient.id("gui/disc");
    private static final Identifier GLOW = LowcosmeticClient.id("gui/glow");

    private Render2D() {
    }

    public static void init() {
        int size = 128;
        float c = size / 2.0F;
        NativeImage disc = new NativeImage(size, size, false);
        NativeImage glow = new NativeImage(size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float d = (float) Math.hypot(x + 0.5F - c, y + 0.5F - c);
                float discAlpha = MathHelper.clamp(c - d - 0.5F, 0.0F, 1.0F);
                float t = MathHelper.clamp(d / c, 0.0F, 1.0F);
                float fall = 1.0F - t * t * (3.0F - 2.0F * t);
                disc.setColorArgb(x, y, ((int) (discAlpha * 255.0F) << 24) | 0xFFFFFF);
                glow.setColorArgb(x, y, ((int) (fall * fall * 255.0F) << 24) | 0xFFFFFF);
            }
        }
        register(DISC, disc);
        register(GLOW, glow);
    }

    private static void register(Identifier id, NativeImage image) {
        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "lowcosmetic_" + id.getPath(), image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
    }

    public static void rect(DrawContext ctx, float x, float y, float w, float h, int color) {
        com.lowcode.svintusclient.client.render.Render2D.rect(ctx, x, y, w, h, 0f, color);
    }

    public static void gradientV(DrawContext ctx, float x, float y, float w, float h, int top, int bottom) {
        com.lowcode.svintusclient.client.render.Render2D.gradientV(ctx, x, y, w, h, 0f, top, bottom);
    }

    public static void gradientH(DrawContext ctx, float x, float y, float w, float h, int left, int right) {
        com.lowcode.svintusclient.client.render.Render2D.gradientH(ctx, x, y, w, h, 0f, left, right);
    }

    public static void gradient(DrawContext ctx, float x, float y, float w, float h, int tl, int tr, int bl, int br) {
        com.lowcode.svintusclient.client.render.Render2D.rect(ctx, x, y, w, h, 0f, tl, tr, br, bl);
    }

    public static void round(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        com.lowcode.svintusclient.client.render.Render2D.rect(ctx, x, y, w, h, r, color);
    }

    public static void roundV(DrawContext ctx, float x, float y, float w, float h, float r, int top, int bottom) {
        com.lowcode.svintusclient.client.render.Render2D.gradientV(ctx, x, y, w, h, r, top, bottom);
    }

    public static void shadow(DrawContext ctx, float x, float y, float w, float h, float r, float spread, int color) {
        com.lowcode.svintusclient.client.render.Render2D.shadow(ctx, x, y, w, h, r, spread, color);
    }

    public static void glow(DrawContext ctx, float cx, float cy, float rx, float ry, int color) {
        com.lowcode.svintusclient.client.render.Render2D.shadow(ctx, cx - rx, cy - ry, rx * 2f, ry * 2f, rx, ry, color);
    }

    public static void circle(DrawContext ctx, float cx, float cy, float radius, int color) {
        com.lowcode.svintusclient.client.render.Render2D.circle(ctx, cx, cy, radius, color);
    }

    public static void icon(DrawContext ctx, Identifier icon, float x, float y, float size, int color) {
        if (size <= 0 || (color >>> 24) == 0) return;
        int s = (int) Math.ceil(size);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, (int) x, (int) y, 0.0f, 0.0f, s, s, s, s, color);
    }

    public static void roundOutline(DrawContext ctx, float x, float y, float w, float h, float r, float thickness, int color) {
        com.lowcode.svintusclient.client.render.Render2D.outline(ctx, x, y, w, h, r, thickness, color);
    }

    public static int alpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * MathHelper.clamp(alpha, 0.0F, 1.0F));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
