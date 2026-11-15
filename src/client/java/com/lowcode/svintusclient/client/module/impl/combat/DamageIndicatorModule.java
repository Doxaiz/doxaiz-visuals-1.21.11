package com.lowcode.svintusclient.client.module.impl.combat;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DamageIndicatorModule extends Module implements Render3DListener {

    public final NumberSetting duration = add(new NumberSetting("Время жизни (мс)", 1200f, 400f, 3000f, 100f));
    public final ColorSetting normalColor = add(new ColorSetting("Цвет урона", 0xFFFF7675));
    public final ColorSetting critColor = add(new ColorSetting("Цвет крита", 0xFFFFD700));
    public final BooleanSetting showHeartIcon = add(new BooleanSetting("Иконка сердечка", true));
    public final BooleanSetting riseAnimation = add(new BooleanSetting("Анимация всплывания", true));
    public final BooleanSetting physicalSparks = add(new BooleanSetting("Физические искры", true));
    public final NumberSetting textScale = add(new NumberSetting("Масштаб цифр", 1.0f, 0.5f, 2.0f, 0.1f));

    public static final class Indicator {
        private final Vec3d position;
        private final String text;
        private final boolean crit;
        private final long spawnTime;
        private final long lifetime;

        public Indicator(Vec3d position, float damage, boolean crit, long lifetime) {
            this.position = position;
            this.text = String.format(java.util.Locale.ROOT, "-%.1f", damage);
            this.crit = crit;
            this.spawnTime = System.currentTimeMillis();
            this.lifetime = lifetime;
        }

        public boolean isAlive() {
            return System.currentTimeMillis() - spawnTime < lifetime;
        }

        public float getProgress() {
            return (float) (System.currentTimeMillis() - spawnTime) / (float) lifetime;
        }
    }

    public static final class Spark {
        public Vec3d pos;
        public Vec3d vel;
        public final int color;
        public final long spawnTime;
        public final long lifetime;

        public Spark(Vec3d pos, Vec3d vel, int color, long lifetime) {
            this.pos = pos;
            this.vel = vel;
            this.color = color;
            this.spawnTime = System.currentTimeMillis();
            this.lifetime = lifetime;
        }

        public boolean isAlive() {
            return System.currentTimeMillis() - spawnTime < lifetime;
        }

        public float getProgress() {
            return (float) (System.currentTimeMillis() - spawnTime) / (float) lifetime;
        }
    }

    private final List<Indicator> indicators = new ArrayList<>();
    private final List<Spark> sparks = new ArrayList<>();
    private final java.util.Random random = new java.util.Random();

    public DamageIndicatorModule() {
        super("DamageIndicator", "Всплывающие цифры и динамические индикаторы нанесённого урона", Category.BATTLE);
    }

    public void addDamage(Vec3d pos, float damage, boolean crit) {
        if (!isEnabled()) return;
        indicators.add(new Indicator(pos, damage, crit, (long) duration.get().floatValue()));

        if (physicalSparks.get()) {
            int sparkColor = crit ? critColor.getEffectiveColor() : normalColor.getEffectiveColor();
            int count = crit ? 10 : 6;
            for (int i = 0; i < count; i++) {
                Vec3d vel = new Vec3d(
                        (random.nextDouble() - 0.5) * 0.25,
                        random.nextDouble() * 0.18 + 0.05,
                        (random.nextDouble() - 0.5) * 0.25
                );
                sparks.add(new Spark(pos, vel, sparkColor, 500L + random.nextInt(300)));
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (indicators.isEmpty() && sparks.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        // Render sparks
        if (!sparks.isEmpty()) {
            Iterator<Spark> sIt = sparks.iterator();
            while (sIt.hasNext()) {
                Spark s = sIt.next();
                if (!s.isAlive()) {
                    sIt.remove();
                    continue;
                }
                float p = s.getProgress();
                float alpha = 1.0f - p;
                int a = (int) (alpha * 240.0f);
                if (a <= 4) continue;
                int col = (a << 24) | (s.color & 0x00FFFFFF);

                s.pos = s.pos.add(s.vel);
                s.vel = s.vel.multiply(0.90).subtract(0, 0.012, 0);

                Vec3d tail = s.pos.subtract(s.vel.multiply(1.5));
                com.lowcode.svintusclient.client.render.Render3D.drawLine(tail, s.pos, col, 2.2f);
            }
        }

        if (indicators.isEmpty() || mc.textRenderer == null) return;

        Iterator<Indicator> it = indicators.iterator();
        while (it.hasNext()) {
            Indicator ind = it.next();
            if (!ind.isAlive()) {
                it.remove();
                continue;
            }

            float progress = ind.getProgress();
            double yOffset = riseAnimation.get() ? Math.sin(progress * Math.PI * 0.5) * 0.8 : 0.0;
            Vec3d renderPos = ind.position.add(0, yOffset, 0);

            int baseColor = ind.crit ? critColor.getEffectiveColor() : normalColor.getEffectiveColor();
            int alpha = Math.round(255 * (1.0f - progress));
            int color = (alpha << 24) | (baseColor & 0x00FFFFFF);

            Vec3d camPos = camera.getCameraPos();
            double dx = renderPos.x - camPos.x;
            double dy = renderPos.y - camPos.y;
            double dz = renderPos.z - camPos.z;

            matrices.push();
            matrices.translate(dx, dy, dz);
            matrices.multiply(camera.getRotation());
            float sc = -0.025f * textScale.get();
            matrices.scale(sc, sc, -sc);

            String renderText = ind.crit ? (showHeartIcon.get() ? "★ " + ind.text + " ❤" : "★ " + ind.text) : (showHeartIcon.get() ? ind.text + " ❤" : ind.text);
            int tw = mc.textRenderer.getWidth(renderText);
            mc.textRenderer.draw(
                    renderText,
                    -tw / 2.0f,
                    0,
                    color,
                    true,
                    matrices.peek().getPositionMatrix(),
                    consumers,
                    net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH,
                    0x60000000,
                    0xF000F0
            );
            matrices.pop();
        }
    }
}
