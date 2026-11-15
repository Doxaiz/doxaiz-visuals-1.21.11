package com.lowcode.svintusclient.client.module.impl.combat;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
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
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class KillEffectsModule extends Module implements Render3DListener {

    public final ModeSetting effect = add(new ModeSetting("Эффект убийства",
            "Молния", "Искры", "Кровь", "Фейерверк", "Призрак Тотема", "Звуковой удар", "Взрыв кубов (Cubes)", "Черная дыра (Vortex)", "Ангел смерти (Death Angel)", "Комбо / Все", "Выкл"));
    public final ModeSetting killSound = add(new ModeSetting("Звук убийства",
            "Final Pok", "Headshot Rust", "Neptune", "Abmiss", "Whii", "Critow", "Final Blink", "Молния", "Опыт", "Крит", "Выкл"));
    public final NumberSetting soundVolume = add(new NumberSetting("Громкость звука", 0.85f, 0.1f, 1.0f, 0.05f));
    public final BooleanSetting lightningVisual = add(new BooleanSetting("3D Молния с неба", true));
    public final BooleanSetting screenFlash = add(new BooleanSetting("Вспышка / Шипы на экране", true));
    public final BooleanSetting cameraShake = add(new BooleanSetting("Тряска камеры", false));
    public final ColorSetting effectColor = add(new ColorSetting("Цвет эффекта", 0xFFFFD700));

    private long lastKillTime = 0L;

    public static final class LightningBolt {
        public final List<Vec3d> segments = new ArrayList<>();
        public final long spawnTime;
        public final int color;

        public LightningBolt(Vec3d target, int color) {
            this.spawnTime = System.currentTimeMillis();
            this.color = color;

            Random rnd = new Random();
            double curX = target.x;
            double curY = target.y + 20.0;
            double curZ = target.z;
            segments.add(new Vec3d(curX, curY, curZ));

            int steps = 14;
            double yStep = 20.0 / steps;

            for (int i = 0; i < steps; i++) {
                curY -= yStep;
                if (i < steps - 1) {
                    curX += (rnd.nextDouble() - 0.5) * 1.4;
                    curZ += (rnd.nextDouble() - 0.5) * 1.4;
                } else {
                    curX = target.x;
                    curZ = target.z;
                }
                segments.add(new Vec3d(curX, curY, curZ));
            }
        }

        public boolean isAlive() {
            return System.currentTimeMillis() - spawnTime < 450L;
        }

        public float getAlpha() {
            float p = (System.currentTimeMillis() - spawnTime) / 450.0f;
            return Math.max(0.0f, 1.0f - p);
        }
    }

    private final List<LightningBolt> bolts = new ArrayList<>();
    private final Random random = new Random();

    public KillEffectsModule() {
        super("Kill Effects", "Кастомные визуальные и звуковые эффекты при убийстве игрока", Category.COSMETICS);
    }

    public void onEntityKill(LivingEntity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || entity == null || mc.world == null) return;

        lastKillTime = System.currentTimeMillis();
        Vec3d pos = entity.getEntityPos().add(0, entity.getHeight() * 0.5, 0);


        // 1. Play kill sound
        String snd = killSound.get();
        if (!"Выкл".equals(snd) && mc.getSoundManager() != null) {
            float vol = soundVolume.get();
            try {
                SoundEvent event = com.lowcode.svintusclient.client.util.SoundPreview.resolveSound(snd);
                if (event != null) {
                    mc.getSoundManager().play(PositionedSoundInstance.ambient(event, 1.0f, vol));
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Spawn particles
        String eff = effect.get();
        if (!"Выкл".equals(eff)) {
            boolean all = "Комбо / Все".equals(eff);

            if (all || "Молния".equals(eff)) {
                for (int i = 0; i < 30; i++) {
                    mc.world.addParticleClient(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
                            (random.nextDouble() - 0.5) * 0.6, random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.6);
                }
                mc.world.addParticleClient(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 0, 0, 0);
            }
            if (all || "Искры".equals(eff)) {
                for (int i = 0; i < 25; i++) {
                    mc.world.addParticleClient(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z,
                            (random.nextDouble() - 0.5) * 0.5, random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.5);
                }
            }
            if (all || "Кровь".equals(eff)) {
                for (int i = 0; i < 30; i++) {
                    mc.world.addParticleClient(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y, pos.z,
                            (random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.4);
                }
            }
            if (all || "Фейерверк".equals(eff)) {
                for (int i = 0; i < 35; i++) {
                    mc.world.addParticleClient(ParticleTypes.FIREWORK, pos.x, pos.y, pos.z,
                            (random.nextDouble() - 0.5) * 0.7, random.nextDouble() * 0.7, (random.nextDouble() - 0.5) * 0.7);
                }
            }
            if (all || "Призрак Тотема".equals(eff)) {
                for (int i = 0; i < 30; i++) {
                    mc.world.addParticleClient(ParticleTypes.TOTEM_OF_UNDYING, pos.x, pos.y, pos.z,
                            (random.nextDouble() - 0.5) * 0.5, random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.5);
                }
            }
            if (all || "Звуковой удар".equals(eff)) {
                mc.world.addParticleClient(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 0, 0, 0);
            }
            if (all || "Взрыв кубов (Cubes)".equals(eff)) {
                for (int i = 0; i < 20; i++) {
                    mc.world.addParticleClient(ParticleTypes.CRIT, pos.x, pos.y + 0.5, pos.z,
                            (random.nextDouble() - 0.5) * 0.8, random.nextDouble() * 0.8, (random.nextDouble() - 0.5) * 0.8);
                }
            }
            if (all || "Черная дыра (Vortex)".equals(eff)) {
                for (int i = 0; i < 35; i++) {
                    mc.world.addParticleClient(ParticleTypes.PORTAL, pos.x, pos.y + 0.8, pos.z,
                            (random.nextDouble() - 0.5) * 1.2, (random.nextDouble() - 0.5) * 1.2, (random.nextDouble() - 0.5) * 1.2);
                }
            }
            if (all || "Ангел смерти (Death Angel)".equals(eff)) {
                for (int i = 0; i < 25; i++) {
                    mc.world.addParticleClient(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z,
                            (random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.5, (random.nextDouble() - 0.5) * 0.4);
                }
            }
        }

        // 3. 3D Procedural Lightning
        if (lightningVisual.get()) {
            bolts.add(new LightningBolt(entity.getEntityPos(), effectColor.getEffectiveColor()));
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
        if (!isEnabled() || bolts.isEmpty()) return;

        Iterator<LightningBolt> it = bolts.iterator();
        Vec3d camPos = camera.getCameraPos();

        while (it.hasNext()) {
            LightningBolt bolt = it.next();
            if (!bolt.isAlive()) {
                it.remove();
                continue;
            }

            if (bolt.segments.isEmpty()) continue;
            if (bolt.segments.get(0).squaredDistanceTo(camPos) > 1600) continue; // 40 blocks distance cull

            float alpha = bolt.getAlpha();
            int a = Math.round(alpha * 255.0f);
            if (a <= 5) continue;

            int activeColor = (a << 24) | (bolt.color & 0x00FFFFFF);

            for (int i = 0; i < bolt.segments.size() - 1; i++) {
                Vec3d p1 = bolt.segments.get(i);
                Vec3d p2 = bolt.segments.get(i + 1);
                Render3D.drawLine(p1, p2, activeColor, 3.0f);
            }
        }
    }

    public void renderOverlay(net.minecraft.client.gui.DrawContext context, int screenW, int screenH) {
        if (!isEnabled() || !screenFlash.get() || lastKillTime <= 0L) return;
        long elapsed = System.currentTimeMillis() - lastKillTime;
        if (elapsed >= 600L) return;

        float f = (float) elapsed / 600.0f;
        float sin = (float) Math.sin(f * Math.PI);
        int alpha = Math.round(140.0f * sin);
        if (alpha > 0) {
            int color = (alpha << 24) | (effectColor.getEffectiveColor() & 0x00FFFFFF);
            float border = Math.max(8.0f, 26.0f * sin);
            com.lowcode.svintusclient.client.render.Render2D.rect(context, 0, 0, screenW, border, 0, color);
            com.lowcode.svintusclient.client.render.Render2D.rect(context, 0, screenH - border, screenW, border, 0, color);
            com.lowcode.svintusclient.client.render.Render2D.rect(context, 0, border, border, screenH - border * 2.0f, 0, color);
            com.lowcode.svintusclient.client.render.Render2D.rect(context, screenW - border, border, border, screenH - border * 2.0f, 0, color);
        }
    }

    public static float getKillShakeDegrees() {
        KillEffectsModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(KillEffectsModule.class);
        if (mod == null || !mod.isEnabled() || !mod.cameraShake.get() || mod.lastKillTime <= 0L) return 0.0f;
        long elapsed = System.currentTimeMillis() - mod.lastKillTime;
        if (elapsed >= 500L) return 0.0f;
        float p = (float) elapsed / 500.0f;
        float f3 = (float) elapsed / 1000.0f;
        float f4 = (float) (Math.sin(f3 * 26.0) * 0.65 + Math.sin(f3 * 16.5) * 0.35);
        float f5 = (1.0f - p) * (1.0f - p);
        return f4 * 2.0f * f5;
    }
}
