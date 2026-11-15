package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class TntTimerModule extends Module implements Render3DListener {

    public final BooleanSetting showSeconds = add(new BooleanSetting("Секунды вместо тиков", true));
    public final NumberSetting renderDistance = add(new NumberSetting("Дистанция 3D", 48.0f, 16.0f, 96.0f, 4.0f));
    public final NumberSetting scale = add(new NumberSetting("Размер текста", 1.2f, 0.5f, 2.5f, 0.1f));
    public final BooleanSetting gradientColor = add(new BooleanSetting("Динамический цвет (Зеленый -> Красный)", true));
    public final BooleanSetting boxHighlight = add(new BooleanSetting("Подсветка куба TNT", true));
    public final BooleanSetting screenAlert = add(new BooleanSetting("Экранный индикатор (GUI)", true));
    public final NumberSetting alertRadius = add(new NumberSetting("Радиус оповещения на экране", 48.0f, 16.0f, 80.0f, 4.0f));

    public TntTimerModule() {
        super("TNT Timer", "3D таймер и экранный GUI индикатор взрывающегося динамита в радиусе", Category.INTERFACE);
    }

    public void renderScreenAlert(net.minecraft.client.gui.DrawContext context, int screenWidth, int screenHeight) {
        if (!isEnabled() || !screenAlert.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || mc.options.hudHidden) return;

        double maxDist = alertRadius.get();
        double maxDistSq = maxDist * maxDist;

        TntEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (e instanceof TntEntity tnt) {
                double dSq = tnt.squaredDistanceTo(mc.player);
                if (dSq <= maxDistSq && dSq < nearestDistSq) {
                    nearest = tnt;
                    nearestDistSq = dSq;
                }
            }
        }

        if (nearest == null) return;

        float seconds = nearest.getFuse() / 20.0f;
        double dist = Math.sqrt(nearestDistSq);

        // Пульсирующая плашка на экране
        float pulse = (float) (0.7 + 0.3 * Math.sin(System.currentTimeMillis() * 0.015));
        int alpha = (int) (pulse * 255);
        int borderCol = (alpha << 24) | 0xFF2222;

        String title = String.format(java.util.Locale.ROOT, "⚠ ДИНАМИТ: %.2fс (%.1fm)", seconds, dist);
        int textW = mc.textRenderer.getWidth(title);
        float pillW = textW + 28f;
        float pillH = 18f;
        float px = (screenWidth - pillW) / 2f;
        float py = 32f;

        com.lowcode.svintusclient.client.hud.HudDraw.glass(context, px, py, pillW, pillH, 5f);
        com.lowcode.svintusclient.client.render.Render2D.outline(context, px, py, pillW, pillH, 5f, 1.2f, borderCol);

        context.drawItem(new net.minecraft.item.ItemStack(net.minecraft.item.Items.TNT), (int) px + 4, (int) py + 1);

        int textCol = seconds < 1.0f ? 0xFFFF3333 : 0xFFFFAA00;
        context.drawTextWithShadow(mc.textRenderer, title, (int) px + 22, (int) py + 5, textCol);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.world == null || mc.player == null) return;

        double maxDistSq = renderDistance.get() * renderDistance.get();
        float delta = tickCounter.getTickProgress(false);
        Vec3d camPos = camera.getCameraPos();
        TextRenderer textRenderer = mc.textRenderer;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof TntEntity tnt)) continue;
            if (tnt.squaredDistanceTo(mc.player) > maxDistSq) continue;

            int fuse = tnt.getFuse();
            float seconds = fuse / 20.0f;
            String text = showSeconds.get() ? String.format("%.2fс", seconds) : fuse + "т";

            // Вычисляем цвет таймера
            int textColor;
            if (gradientColor.get()) {
                float frac = Math.max(0.0f, Math.min(1.0f, seconds / 4.0f));
                // Зеленый при 4с -> Желтый при 2с -> Красный при 0с
                int r = (int) ((1.0f - frac) * 255);
                int g = (int) (frac * 255);
                textColor = 0xFF000000 | (r << 16) | (g << 8) | 0x20;
            } else {
                textColor = 0xFFFF4757;
            }

            Vec3d pos = tnt.getLerpedPos(delta);

            if (boxHighlight.get()) {
                Box box = new Box(pos.x - 0.5, pos.y, pos.z - 0.5, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5);
                int alpha = (int) ((1.0f - (seconds / 4.0f)) * 120 + 40);
                int fillColor = (alpha << 24) | (textColor & 0x00FFFFFF);
                Render3D.drawBox(box, fillColor, textColor, true, true, 1.5f);
            }

            // Отрисовка 3D билборда над динамитом
            matrices.push();
            matrices.translate(pos.x - camPos.x, pos.y + 1.25 - camPos.y, pos.z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float textScale = 0.025f * scale.get();
            matrices.scale(-textScale, -textScale, textScale);

            int width = textRenderer.getWidth(text);
            int bg = 0x88000000;

            // Фон подложки
            textRenderer.draw(text, -width / 2.0f, 0, textColor, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.SEE_THROUGH, bg, 0xF000F0);
            textRenderer.draw(text, -width / 2.0f, 0, textColor, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);

            matrices.pop();
        }
    }
}
