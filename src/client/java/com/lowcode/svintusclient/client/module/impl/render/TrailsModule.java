package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TrailsModule extends Module implements TickListener, Render3DListener {

    public final ModeSetting mode = add(new ModeSetting("Режим",
            "Линия", "Неон", "Точки", "Широкая лента", "Кубики", "Искры", "3D Спираль (Spiral)", "Двойная лента (Double Ribbon)", "Кибер-глифы (Cyber Glyphs)"));
    public final NumberSetting lifetime = add(new NumberSetting("Длительность (сек)", 1.2f, 0.4f, 3.0f, 0.1f));
    public final NumberSetting thickness = add(new NumberSetting("Толщина", 2.2f, 1.0f, 5.0f, 0.2f));
    public final NumberSetting cullDistance = add(new NumberSetting("Дальность отсечения", 36.0f, 16.0f, 64.0f, 4.0f));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета", "Свой цвет", "Радуга", "Акцент темы"));
    public final ColorSetting color = add(new ColorSetting("Цвет следа", 0xFF00FF9F));

    private static class TrailPoint {
        final Vec3d pos;
        final long time;

        TrailPoint(Vec3d pos, long time) {
            this.pos = pos;
            this.time = time;
        }
    }

    private final List<TrailPoint> points = new ArrayList<>();

    public TrailsModule() {
        super("Trails", "Светящийся след (шлейф/лента/кубы) за игроком при беге и полете", Category.COSMETICS);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) {
            if (!points.isEmpty()) points.clear();
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            points.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long maxAge = (long) (lifetime.get() * 1000);

        Iterator<TrailPoint> it = points.iterator();
        while (it.hasNext()) {
            if (now - it.next().time > maxAge) {
                it.remove();
            }
        }

        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY() + 0.15, mc.player.getZ());
        // Throttling: only add point if moved at least 0.05 blocks
        if (points.isEmpty() || points.get(points.size() - 1).pos.squaredDistanceTo(playerPos) > 0.0025) {
            points.add(new TrailPoint(playerPos, now));
        }
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled() || points.size() < 2) return;

        long now = System.currentTimeMillis();
        long maxAge = (long) (lifetime.get() * 1000);
        int baseColor = color.getEffectiveColor();
        float width = thickness.get();
        String m = mode.get();
        String cMode = colorMode.get();
        Vec3d camPos = camera.getCameraPos();
        double maxDistSq = cullDistance.get() * cullDistance.get();

        for (int i = 0; i < points.size() - 1; i++) {
            TrailPoint p1 = points.get(i);
            TrailPoint p2 = points.get(i + 1);

            if (p1.pos.squaredDistanceTo(camPos) > maxDistSq) continue; // Distance culling

            float ageFrac = 1.0f - (float) (now - p1.time) / maxAge;
            if (ageFrac <= 0.02f) continue;

            int alpha = (int) (ageFrac * 230);
            int segColor;
            if ("Радуга".equals(cMode)) {
                float hue = ((now + (long) (p1.pos.x * 150 + p1.pos.z * 150)) % 3000L) / 3000.0f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.75f, 1.0f) & 0x00FFFFFF;
                segColor = (alpha << 24) | rgb;
            } else if ("Акцент темы".equals(cMode)) {
                segColor = (alpha << 24) | (Theme.accent() & 0x00FFFFFF);
            } else {
                segColor = (alpha << 24) | (baseColor & 0x00FFFFFF);
            }

            switch (m) {
                case "Точки" -> Render3D.drawLine(p1.pos, p1.pos.add(0, 0.06, 0), segColor, width * 1.6f);
                case "Неон" -> {
                    Render3D.drawLine(p1.pos, p2.pos, (alpha / 2 << 24) | (segColor & 0x00FFFFFF), width * 2.0f);
                    Render3D.drawLine(p1.pos, p2.pos, segColor, width);
                }
                case "Широкая лента" -> {
                    Vec3d up = new Vec3d(0, 0.35 * width / 2.2, 0);
                    Render3D.drawGradientTriangle(p1.pos.subtract(up), p1.pos.add(up), p2.pos.add(up), segColor, segColor, segColor);
                    Render3D.drawGradientTriangle(p1.pos.subtract(up), p2.pos.add(up), p2.pos.subtract(up), segColor, segColor, segColor);
                }
                case "Кубики" -> {
                    double s = 0.04 * width;
                    Box box = new Box(p1.pos.x - s, p1.pos.y - s, p1.pos.z - s, p1.pos.x + s, p1.pos.y + s, p1.pos.z + s);
                    Render3D.drawBox(box, (alpha / 3 << 24) | (segColor & 0x00FFFFFF), segColor, true, true, 1.0f);
                }
                case "Искры" -> {
                    Render3D.drawLine(p1.pos, p1.pos.add(0, 0.12 * width, 0), segColor, width * 1.5f);
                }
                case "3D Спираль (Spiral)" -> {
                    double angle = (p1.time * 0.01);
                    Vec3d spOffset = new Vec3d(Math.cos(angle) * 0.25 * width, Math.sin(angle) * 0.25 * width, 0);
                    Render3D.drawLine(p1.pos.add(spOffset), p2.pos.add(spOffset), segColor, width * 1.4f);
                }
                case "Двойная лента (Double Ribbon)" -> {
                    Vec3d up1 = new Vec3d(0, 0.18 * width, 0);
                    Vec3d up2 = new Vec3d(0, -0.18 * width, 0);
                    Render3D.drawLine(p1.pos.add(up1), p2.pos.add(up1), segColor, width * 1.2f);
                    Render3D.drawLine(p1.pos.add(up2), p2.pos.add(up2), segColor, width * 1.2f);
                }
                case "Кибер-глифы (Cyber Glyphs)" -> {
                    Render3D.drawLine(p1.pos, p2.pos, segColor, width);
                    if (i % 2 == 0) {
                        Render3D.drawLine(p1.pos, p1.pos.add(0, 0.25, 0), (alpha / 2 << 24) | (segColor & 0x00FFFFFF), 1.2f);
                    }
                }
                default -> Render3D.drawLine(p1.pos, p2.pos, segColor, width);
            }
        }
    }
}
