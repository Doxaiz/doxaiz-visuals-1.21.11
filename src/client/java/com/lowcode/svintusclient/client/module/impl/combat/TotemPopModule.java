package com.lowcode.svintusclient.client.module.impl.combat;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TotemPopModule extends Module implements Render3DListener {

    public final ModeSetting popEffect = add(new ModeSetting("Эффект тотема",
            "Призрак тотема", "Ангел Astrix", "Взрыв частиц", "Энергетический круг", "Сфера частиц", "Орбитальные кольца", "Комбо / Все"));
    public final ColorSetting totemColor = add(new ColorSetting("Цвет эффекта", 0xFFFFD700));
    public final BooleanSetting cleanScreen = add(new BooleanSetting("Убрать тотем с экрана", true));

    public static final class TotemPopVisual {
        public final Vec3d basePos;
        public final long spawnTime;
        public final int color;

        public TotemPopVisual(Vec3d basePos, int color) {
            this.basePos = basePos;
            this.spawnTime = System.currentTimeMillis();
            this.color = color;
        }

        public boolean isAlive() {
            return System.currentTimeMillis() - spawnTime < 1100L;
        }

        public float getProgress() {
            return (float) (System.currentTimeMillis() - spawnTime) / 1100.0f;
        }
    }

    private final List<TotemPopVisual> visuals = new ArrayList<>();

    public TotemPopModule() {
        super("Totem Pop", "Кастомные эффекты и звуки при срабатывании Тотема бессмертия", Category.BATTLE);
    }

    public void onTotemPop(LivingEntity entity) {
        if (!isEnabled() || entity == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        Vec3d center = entity.getEntityPos().add(0, entity.getHeight() * 0.5, 0);

        // 1. Spawn particles
        String mode = popEffect.get();
        if ("Взрыв частиц".equals(mode) || "Комбо / Все".equals(mode)) {
            for (int i = 0; i < 35; i++) {
                mc.world.addParticleClient(ParticleTypes.TOTEM_OF_UNDYING, center.x, center.y, center.z,
                        (Math.random() - 0.5) * 0.8, Math.random() * 0.6, (Math.random() - 0.5) * 0.8);
            }
        }

        // 3. Add 3D visual ring / ghost
        if (!"Взрыв частиц".equals(mode)) {
            visuals.add(new TotemPopVisual(entity.getEntityPos(), totemColor.getEffectiveColor()));
            com.lowcode.svintusclient.client.module.impl.render.TotemGhostModule.spawnGhost(entity.getEntityPos());
        }
    }

    private void playCustomSound(String id, SoundEvent fallback, float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Identifier soundId = Identifier.of("doxaiz", id);
            SoundEvent event = SoundEvent.of(soundId);
            mc.getSoundManager().play(PositionedSoundInstance.ambient(event, pitch, volume));
        } catch (Exception e) {
            if (fallback != null && mc.getSoundManager() != null) {
                mc.getSoundManager().play(PositionedSoundInstance.ambient(fallback, pitch, volume));
            }
        }
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled() || visuals.isEmpty()) return;

        Iterator<TotemPopVisual> it = visuals.iterator();
        Vec3d camPos = camera.getCameraPos();
        int segs = 24;

        while (it.hasNext()) {
            TotemPopVisual v = it.next();
            if (!v.isAlive()) {
                it.remove();
                continue;
            }

            if (v.basePos.squaredDistanceTo(camPos) > 1600) continue; // 40 blocks cull

            float progress = v.getProgress();
            float alphaFactor = 1.0f - progress;
            int a = Math.round(alphaFactor * 240.0f);
            if (a <= 5) continue;

            int activeColor = (a << 24) | (v.color & 0x00FFFFFF);

            // Expanding circle on ground
            float radius = 0.5f + progress * 2.2f;
            double y = v.basePos.y + 0.05 + progress * 0.4;

            for (int i = 0; i < segs; i++) {
                double a1 = (i * 2.0 * Math.PI) / segs;
                double a2 = ((i + 1) * 2.0 * Math.PI) / segs;
                Vec3d p1 = new Vec3d(v.basePos.x + Math.cos(a1) * radius, y, v.basePos.z + Math.sin(a1) * radius);
                Vec3d p2 = new Vec3d(v.basePos.x + Math.cos(a2) * radius, y, v.basePos.z + Math.sin(a2) * radius);
                Render3D.drawLine(p1, p2, activeColor, 2.5f);
            }

            // Rising ghost humanoid figure (AuroraVisuals / shape style)
            if ("Призрак тотема".equals(popEffect.get()) || "Комбо / Все".equals(popEffect.get())) {
                double ghostY = v.basePos.y + progress * 2.2;
                int ghostFill = ((int)(a * 0.35f) << 24) | (v.color & 0x00FFFFFF);
                
                // Тело (торс)
                Box body = new Box(v.basePos.x - 0.25, ghostY + 0.4, v.basePos.z - 0.15, v.basePos.x + 0.25, ghostY + 1.1, v.basePos.z + 0.15);
                Render3D.drawBox(body, ghostFill, activeColor, true, true, 1.5f);

                // Голова
                Box head = new Box(v.basePos.x - 0.2, ghostY + 1.12, v.basePos.z - 0.2, v.basePos.x + 0.2, ghostY + 1.52, v.basePos.z + 0.2);
                Render3D.drawBox(head, ghostFill, activeColor, true, true, 1.5f);

                // Руки
                Box leftArm = new Box(v.basePos.x - 0.42, ghostY + 0.4, v.basePos.z - 0.12, v.basePos.x - 0.26, ghostY + 1.1, v.basePos.z + 0.12);
                Box rightArm = new Box(v.basePos.x + 0.26, ghostY + 0.4, v.basePos.z - 0.12, v.basePos.x + 0.42, ghostY + 1.1, v.basePos.z + 0.12);
                Render3D.drawBox(leftArm, ghostFill, activeColor, true, true, 1.0f);
                Render3D.drawBox(rightArm, ghostFill, activeColor, true, true, 1.0f);
            }

            // Angel Astrix (glowing halo and celestial wings)
            if ("Ангел Astrix".equals(popEffect.get()) || "Комбо / Все".equals(popEffect.get())) {
                double angelY = v.basePos.y + 1.2 + progress * 1.8;
                float haloRadius = 0.32f;
                double haloY = angelY + 0.6;
                for (int i = 0; i < 16; i++) {
                    double a1 = (i * 2.0 * Math.PI) / 16;
                    double a2 = ((i + 1) * 2.0 * Math.PI) / 16;
                    Vec3d h1 = new Vec3d(v.basePos.x + Math.cos(a1) * haloRadius, haloY, v.basePos.z + Math.sin(a1) * haloRadius);
                    Vec3d h2 = new Vec3d(v.basePos.x + Math.cos(a2) * haloRadius, haloY, v.basePos.z + Math.sin(a2) * haloRadius);
                    Render3D.drawLine(h1, h2, activeColor, 2.0f);
                }

                float wingSpread = 0.35f + progress * 0.85f;
                Vec3d wingCenter = new Vec3d(v.basePos.x, angelY + 0.2, v.basePos.z);
                Vec3d leftTip = wingCenter.add(-wingSpread, 0.45 * (1.0 - progress), 0);
                Vec3d rightTip = wingCenter.add(wingSpread, 0.45 * (1.0 - progress), 0);
                Vec3d leftBottom = wingCenter.add(-wingSpread * 0.6, -0.3, 0);
                Vec3d rightBottom = wingCenter.add(wingSpread * 0.6, -0.3, 0);

                Render3D.drawLine(wingCenter, leftTip, activeColor, 2.5f);
                Render3D.drawLine(leftTip, leftBottom, activeColor, 2.0f);
                Render3D.drawLine(leftBottom, wingCenter, activeColor, 1.8f);

                Render3D.drawLine(wingCenter, rightTip, activeColor, 2.5f);
                Render3D.drawLine(rightTip, rightBottom, activeColor, 2.0f);
                Render3D.drawLine(rightBottom, wingCenter, activeColor, 1.8f);
            }

            // Expanding sphere particle shockwave
            if ("Сфера частиц".equals(popEffect.get()) || "Комбо / Все".equals(popEffect.get())) {
                float sphereR = 0.4f + progress * 2.5f;
                Vec3d sc = v.basePos.add(0, 1.0, 0);
                for (int i = 0; i < 12; i++) {
                    double theta = i * Math.PI / 12.0;
                    double sy = Math.cos(theta) * sphereR;
                    double ringR = Math.sin(theta) * sphereR;
                    for (int j = 0; j < 8; j++) {
                        double phi = j * 2.0 * Math.PI / 8.0;
                        Vec3d pt = sc.add(Math.cos(phi) * ringR, sy, Math.sin(phi) * ringR);
                        Render3D.drawLine(pt, pt.add(0, 0.05, 0), activeColor, 2.0f);
                    }
                }
            }

            // Orbiting inclined rings
            if ("Орбитальные кольца".equals(popEffect.get()) || "Комбо / Все".equals(popEffect.get())) {
                float orbitR = 0.6f + progress * 1.8f;
                Vec3d oc = v.basePos.add(0, 0.8 + progress * 0.5, 0);
                double rotAngle = progress * Math.PI * 4.0;
                for (int i = 0; i < 20; i++) {
                    double a1 = (i * 2.0 * Math.PI) / 20;
                    double a2 = ((i + 1) * 2.0 * Math.PI) / 20;
                    Vec3d o1 = oc.add(Math.cos(a1 + rotAngle) * orbitR, Math.sin(a1) * 0.4, Math.sin(a1 + rotAngle) * orbitR);
                    Vec3d o2 = oc.add(Math.cos(a2 + rotAngle) * orbitR, Math.sin(a2) * 0.4, Math.sin(a2 + rotAngle) * orbitR);
                    Render3D.drawLine(o1, o2, activeColor, 2.2f);
                }
            }
        }
    }
}
