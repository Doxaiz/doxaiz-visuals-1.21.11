package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class JumpCirclesModule extends Module implements Render3DListener {

    public final ModeSetting style = add(new ModeSetting("Стиль",
            "Текстурный (Soup)", "Soup Bold", "Портал (Portal)", "Логотип Soup", "Astrix Круг", "Astrix Неон", "Текстурный (Doxaiz)",
            "Волна", "Кольцо", "Диск", "Куб 3D", "Цветок", "Неоновое кольцо (Neon Ring)", "Звёздная спираль (Star Spiral)", "Импульсный взрыв (Pulse Burst)"));
    public final ModeSetting easing = add(new ModeSetting("Анимация", "Обычная", "Эластичная", "Назад"));
    public final NumberSetting duration = add(new NumberSetting("Длительность (мс)", 1200f, 400f, 3000f, 100f));
    public final NumberSetting radius = add(new NumberSetting("Радиус", 2.2f, 0.5f, 6.0f, 0.1f));
    public final NumberSetting segments = add(new NumberSetting("Сегменты (Качество)", 36f, 12f, 64f, 2f));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета", "Свой цвет", "Радуга", "Акцент темы"));
    public final ColorSetting color = add(new ColorSetting("Цвет круга", 0xFF6C5CE7));

    private static final Identifier TEX_SOUP_CIRCLE = Identifier.of("doxaiz", "textures/jumpcircle/soup_circle.png");
    private static final Identifier TEX_SOUP_BOLD = Identifier.of("doxaiz", "textures/jumpcircle/soup_bold.png");
    private static final Identifier TEX_PORTAL = Identifier.of("doxaiz", "textures/jumpcircle/portal.png");
    private static final Identifier TEX_SOUP_LOGO = Identifier.of("doxaiz", "textures/jumpcircle/soup_logo.png");
    private static final Identifier TEX_ASTRIX_1 = Identifier.of("doxaiz", "textures/jumpcircle/astrix_circle.png");
    private static final Identifier TEX_ASTRIX_2 = Identifier.of("doxaiz", "textures/jumpcircle/astrix_circle2.png");
    private static final Identifier TEX_DOXAIZ = Identifier.of("doxaiz", "textures/jumpcircle/circle.png");

    public static final class Circle {
        public final Vec3d position;
        public final long spawnTime;
        public final long duration;
        public final float maxRadius;
        public final int color;

        public Circle(Vec3d position, long duration, float maxRadius, int color) {
            this.position = position;
            this.spawnTime = System.currentTimeMillis();
            this.duration = duration;
            this.maxRadius = maxRadius;
            this.color = color;
        }

        public boolean isAlive() {
            return System.currentTimeMillis() - spawnTime < duration;
        }

        public float getProgress() {
            return (float) (System.currentTimeMillis() - spawnTime) / (float) duration;
        }
    }

    private final List<Circle> circles = new ArrayList<>();

    public JumpCirclesModule() {
        super("Jump Circles", "Анимированные круги / волны / кубы на земле при прыжке", Category.COSMETICS);
    }

    public void onJump(PlayerEntity player) {
        if (!isEnabled() || player == null) return;
        int circleColor = color.getEffectiveColor();
        circles.add(new Circle(
                player.getEntityPos(),
                (long) duration.get().floatValue(),
                radius.get(),
                circleColor
        ));
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled() || circles.isEmpty()) return;

        Iterator<Circle> it = circles.iterator();
        int segs = (int) segments.get().floatValue();
        String st = style.get();
        String cMode = colorMode.get();
        long now = System.currentTimeMillis();
        Vec3d camPos = camera.getCameraPos();

        while (it.hasNext()) {
            Circle c = it.next();
            if (!c.isAlive()) {
                it.remove();
                continue;
            }

            if (c.position.squaredDistanceTo(camPos) > 1600) continue; // Distance culling (40 blocks)

            float progress = c.getProgress();
            float currentRadius = switch (easing.get()) {
                case "Эластичная" -> c.maxRadius * easeOutElastic(progress);
                case "Назад" -> c.maxRadius * easeOutBack(progress);
                default -> c.maxRadius * (float) Math.sin(progress * Math.PI * 0.5);
            };
            int baseAlpha = (c.color >> 24) & 0xFF;
            if (baseAlpha == 0) baseAlpha = 220;
            int fadeAlpha = Math.round(baseAlpha * (1.0f - progress));
            if (fadeAlpha <= 2) continue;

            int activeColor;
            if ("Радуга".equals(cMode)) {
                float hue = ((now + (long) (c.position.x * 200 + c.position.z * 200)) % 3000L) / 3000.0f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f) & 0x00FFFFFF;
                activeColor = (fadeAlpha << 24) | rgb;
            } else if ("Акцент темы".equals(cMode)) {
                activeColor = (fadeAlpha << 24) | (Theme.accent() & 0x00FFFFFF);
            } else {
                activeColor = (fadeAlpha << 24) | (c.color & 0x00FFFFFF);
            }

            double y = c.position.y + 0.02;

            Identifier tex = switch (st) {
                case "Текстурный (Soup)" -> TEX_SOUP_CIRCLE;
                case "Soup Bold" -> TEX_SOUP_BOLD;
                case "Портал (Portal)" -> TEX_PORTAL;
                case "Логотип Soup" -> TEX_SOUP_LOGO;
                case "Astrix Круг" -> TEX_ASTRIX_1;
                case "Astrix Неон" -> TEX_ASTRIX_2;
                case "Текстурный (Doxaiz)" -> TEX_DOXAIZ;
                default -> null;
            };

            if (tex != null) {
                float rot = (float) ((now - c.spawnTime) * 0.1) % 360f;
                Render3D.drawGroundTexture(matrices, consumers, camera, tex, c.position, currentRadius, rot, activeColor);
                continue;
            }

            if ("Куб 3D".equals(st)) {
                double cs = currentRadius * 0.55;
                double rot = progress * Math.PI * 1.5;
                Vec3d cPos = new Vec3d(c.position.x, y + cs * 0.5, c.position.z);
                int fill = ((int)(fadeAlpha * 0.35f) << 24) | (activeColor & 0x00FFFFFF);
                Render3D.drawRotatedCube(cPos, cs, rot, rot * 0.5, 0, fill, activeColor, true, true, 1.8f);
                continue;
            }

            int innerColor = ((int) (fadeAlpha * 0.35f) << 24) | (activeColor & 0x00FFFFFF);
            float innerRadius = "Кольцо".equals(st) ? currentRadius * 0.92f : ("Волна".equals(st) ? currentRadius * 0.75f : 0f);

            for (int i = 0; i < segs; i++) {
                double a1 = (i * 2.0 * Math.PI) / segs;
                double a2 = ((i + 1) * 2.0 * Math.PI) / segs;

                float r1 = currentRadius;
                float r2 = currentRadius;
                if ("Цветок".equals(st)) {
                    r1 *= (1.0f + (float) Math.sin(a1 * 6.0) * 0.15f);
                    r2 *= (1.0f + (float) Math.sin(a2 * 6.0) * 0.15f);
                } else if ("Звёздная спираль (Star Spiral)".equals(st)) {
                    r1 *= (1.0f + (float) Math.sin(a1 * 5.0 + progress * 6.0) * 0.25f);
                    r2 *= (1.0f + (float) Math.sin(a2 * 5.0 + progress * 6.0) * 0.25f);
                } else if ("Импульсный взрыв (Pulse Burst)".equals(st)) {
                    r1 *= (float) Math.pow(progress, 0.5);
                    r2 *= (float) Math.pow(progress, 0.5);
                }

                Vec3d out1 = new Vec3d(c.position.x + Math.cos(a1) * r1, y, c.position.z + Math.sin(a1) * r1);
                Vec3d out2 = new Vec3d(c.position.x + Math.cos(a2) * r2, y, c.position.z + Math.sin(a2) * r2);

                if ("Диск".equals(st)) {
                    Vec3d center = new Vec3d(c.position.x, y, c.position.z);
                    Render3D.drawGradientTriangle(center, out1, out2, innerColor, activeColor, activeColor);
                    Render3D.drawLine(out1, out2, activeColor, 1.5f);
                } else {
                    Vec3d in1 = new Vec3d(c.position.x + Math.cos(a1) * innerRadius, y, c.position.z + Math.sin(a1) * innerRadius);
                    Vec3d in2 = new Vec3d(c.position.x + Math.cos(a2) * innerRadius, y, c.position.z + Math.sin(a2) * innerRadius);

                    Render3D.drawGradientTriangle(in1, out1, out2, innerColor, activeColor, activeColor);
                    Render3D.drawGradientTriangle(in1, out2, in2, innerColor, activeColor, innerColor);

                    Render3D.drawLine(out1, out2, activeColor, 1.8f);
                    if ("Кольцо".equals(st) || "Волна".equals(st)) {
                        Render3D.drawLine(in1, in2, innerColor, 1.2f);
                    }
                }
            }
        }
    }

    private static float easeOutElastic(float x) {
        if (x <= 0.0f) return 0.0f;
        if (x >= 1.0f) return 1.0f;
        double c4 = (2.0 * Math.PI) / 3.0;
        return (float) (Math.pow(2.0, -10.0 * x) * Math.sin((x * 10.0 - 0.75) * c4) + 1.0);
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(x - 1.0f, 3) + c1 * (float) Math.pow(x - 1.0f, 2);
    }
}
