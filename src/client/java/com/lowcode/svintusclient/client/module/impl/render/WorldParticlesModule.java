package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class WorldParticlesModule extends Module implements TickListener, Render3DListener {

    public final ModeSetting mode = add(new ModeSetting("Тип частиц",
            "Сакура", "Звезда (Hit)", "Молния (Hit)", "Крест (Hit)", "Корона (Hit)",
            "Сердце (Hit)", "Линия (Hit)", "Ромб (Hit)", "Доллар (Hit)", "Снежинка (Hit)",
            "Треугольник (Hit)", "Точка (Hit)", "Искры Astrix 1", "Искры Astrix 2",
            "Кристаллы Astrix 3", "Вспышка Astrix 4", "Светлячки", "Кубики", "Пузыри",
            "Блум / Сияние", "Комбо / Все"));
    public final NumberSetting maxParticles = add(new NumberSetting("Лимит частиц", 60.0f, 15.0f, 150.0f, 5.0f));
    public final NumberSetting particleSize = add(new NumberSetting("Размер", 0.08f, 0.03f, 0.25f, 0.01f));
    public final NumberSetting fallSpeed = add(new NumberSetting("Скорость", 1.0f, 0.2f, 2.5f, 0.1f));
    public final NumberSetting renderDistance = add(new NumberSetting("Дальность прорисовки", 18.0f, 8.0f, 36.0f, 2.0f));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета", "Свой цвет", "Радуга", "Акцент темы"));
    public final ColorSetting color = add(new ColorSetting("Цвет частиц", 0xFFFF7597));

    // Hit Particles Textures
    private static final Identifier TEX_POINT = Identifier.of("doxaiz", "textures/particles/point.png");
    private static final Identifier TEX_STAR = Identifier.of("doxaiz", "textures/particles/star.png");
    private static final Identifier TEX_LIGHTNING = Identifier.of("doxaiz", "textures/particles/lighting.png");
    private static final Identifier TEX_CROSS = Identifier.of("doxaiz", "textures/particles/cross.png");
    private static final Identifier TEX_CROWN = Identifier.of("doxaiz", "textures/particles/crown.png");
    private static final Identifier TEX_HEART = Identifier.of("doxaiz", "textures/particles/heart.png");
    private static final Identifier TEX_LINE = Identifier.of("doxaiz", "textures/particles/line.png");
    private static final Identifier TEX_RHOMBUS = Identifier.of("doxaiz", "textures/particles/rhombus.png");
    private static final Identifier TEX_DOLLAR = Identifier.of("doxaiz", "textures/particles/dollar.png");
    private static final Identifier TEX_SNOWFLAKE = Identifier.of("doxaiz", "textures/particles/snowflake.png");
    private static final Identifier TEX_TRIANGLE = Identifier.of("doxaiz", "textures/particles/triangle.png");
    private static final Identifier TEX_ASTRIX_1 = Identifier.of("doxaiz", "textures/particles/hit_particle1.png");
    private static final Identifier TEX_ASTRIX_2 = Identifier.of("doxaiz", "textures/particles/hit_particle2.png");
    private static final Identifier TEX_ASTRIX_3 = Identifier.of("doxaiz", "textures/particles/hit_particle3.png");
    private static final Identifier TEX_ASTRIX_4 = Identifier.of("doxaiz", "textures/particles/hit_particle4.png");

    // Ambient textures
    private static final Identifier TEX_SAKURA = Identifier.of("doxaiz", "textures/world/star1.png");
    private static final Identifier TEX_CUBE = Identifier.of("doxaiz", "textures/world/cube.png");
    private static final Identifier TEX_BUBBLE = Identifier.of("doxaiz", "textures/particles/bubble.png");
    private static final Identifier TEX_GLOW = Identifier.of("doxaiz", "textures/particles/glow.png");

    private static class Particle {
        double x, y, z;
        double vx, vy, vz;
        long spawnTime;
        long lifetime;
        float rot;
        float rotSpeed;
        int shapeType;

        void reset(double x, double y, double z, double vx, double vy, double vz, long lifetime, int shapeType) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.spawnTime = System.currentTimeMillis();
            this.lifetime = lifetime;
            this.rot = (float) (Math.random() * Math.PI * 2);
            this.rotSpeed = (float) ((Math.random() - 0.5) * 4.0);
            this.shapeType = shapeType;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> pool = new ArrayList<>();
    private final Random random = new Random();

    public WorldParticlesModule() {
        super("Частицы в небе", "Атмосферные 3D частицы вокруг игрока (звезды, сакура, искры)", Category.VISUALS);
        for (int i = 0; i < 10; i++) {
            pool.add(new Particle());
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled()) {
            if (!particles.isEmpty()) {
                pool.addAll(particles);
                particles.clear();
            }
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();

        // 1. Update existing particles
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            if (now - p.spawnTime >= p.lifetime) {
                it.remove();
                if (pool.size() < 120) pool.add(p);
                continue;
            }
            p.x += p.vx;
            p.y += p.vy;
            p.z += p.vz;
            p.rot += p.rotSpeed * 0.05f;
        }

        // 2. Spawn new particles up to limit
        int max = Math.round(maxParticles.get());
        if (particles.size() < max) {
            double px = mc.player.getX();
            double py = mc.player.getY();
            double pz = mc.player.getZ();
            double rad = renderDistance.get();
            double rx = px + (random.nextDouble() - 0.5) * rad * 2.0;
            double ry = py + random.nextDouble() * 6.0 - 1.0;
            double rz = pz + (random.nextDouble() - 0.5) * rad * 2.0;

            float speed = fallSpeed.get();
            double vx = (random.nextDouble() - 0.5) * 0.02 * speed;
            double vz = (random.nextDouble() - 0.5) * 0.02 * speed;
            double vy = -0.015 * speed;

            String m = mode.get();
            int shape = switch (m) {
                case "Звезда (Hit)" -> 6;
                case "Молния (Hit)" -> 7;
                case "Крест (Hit)" -> 8;
                case "Корона (Hit)" -> 9;
                case "Сердце (Hit)" -> 10;
                case "Линия (Hit)" -> 11;
                case "Ромб (Hit)" -> 12;
                case "Доллар (Hit)" -> 13;
                case "Снежинка (Hit)" -> 14;
                case "Треугольник (Hit)" -> 15;
                case "Точка (Hit)" -> 16;
                case "Искры Astrix 1" -> 2;
                case "Искры Astrix 2" -> 3;
                case "Кристаллы Astrix 3" -> 4;
                case "Вспышка Astrix 4" -> 5;
                case "Светлячки" -> 1;
                case "Кубики" -> 17;
                case "Пузыри" -> 18;
                case "Блум / Сияние" -> 19;
                case "Комбо / Все" -> random.nextInt(20);
                default -> 0; // Сакура
            };

            if (shape == 1 || shape == 18 || shape == 19) {
                vy = 0.012 * speed; // плывут вверх
            }

            long lifetime = 2500 + random.nextInt(3500);
            Particle p = pool.isEmpty() ? new Particle() : pool.remove(pool.size() - 1);
            p.reset(rx, ry, rz, vx, vy, vz, lifetime, shape);
            particles.add(p);
        }
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled() || particles.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        float sz = particleSize.get();
        float maxDistSq = renderDistance.get() * renderDistance.get();
        Vec3d camPos = camera.getCameraPos();

        int baseColor = color.getEffectiveColor();
        String cMode = colorMode.get();

        for (Particle p : particles) {
            Vec3d pos = new Vec3d(p.x, p.y, p.z);
            double distSq = pos.squaredDistanceTo(camPos);
            if (distSq > maxDistSq) continue;

            float age = (float) (now - p.spawnTime) / p.lifetime;
            if (age >= 1.0f) continue;
            float alphaProgress = (float) Math.sin(age * Math.PI);
            int alpha = (int) (alphaProgress * 230);
            if (alpha <= 4) continue;

            int drawColor;
            if ("Радуга".equals(cMode)) {
                float hue = ((now + (long) (p.x * 200 + p.z * 200)) % 4000L) / 4000.0f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.75f, 1.0f) & 0x00FFFFFF;
                drawColor = (alpha << 24) | rgb;
            } else if ("Акцент темы".equals(cMode)) {
                drawColor = (alpha << 24) | (Theme.accent() & 0x00FFFFFF);
            } else {
                drawColor = (alpha << 24) | (baseColor & 0x00FFFFFF);
            }

            Identifier tex = switch (p.shapeType) {
                case 1 -> TEX_GLOW;
                case 2 -> TEX_ASTRIX_1;
                case 3 -> TEX_ASTRIX_2;
                case 4 -> TEX_ASTRIX_3;
                case 5 -> TEX_ASTRIX_4;
                case 6 -> TEX_STAR;
                case 7 -> TEX_LIGHTNING;
                case 8 -> TEX_CROSS;
                case 9 -> TEX_CROWN;
                case 10 -> TEX_HEART;
                case 11 -> TEX_LINE;
                case 12 -> TEX_RHOMBUS;
                case 13 -> TEX_DOLLAR;
                case 14 -> TEX_SNOWFLAKE;
                case 15 -> TEX_TRIANGLE;
                case 16 -> TEX_POINT;
                case 17 -> TEX_CUBE;
                case 18 -> TEX_BUBBLE;
                case 19 -> TEX_GLOW;
                default -> TEX_SAKURA;
            };
            drawBillboardTexture(matrices, consumers, camera, tex, pos, sz * 3.5f, (float) Math.toDegrees(p.rot), drawColor);
        }
    }

    private void drawBillboardTexture(
            MatrixStack matrices,
            VertexConsumerProvider.Immediate consumers,
            Camera camera,
            Identifier texture,
            Vec3d pos,
            float size,
            float rotation,
            int color
    ) {
        if (consumers == null || camera == null || texture == null) return;
        RenderLayer layer = RenderLayers.entityTranslucent(texture);
        VertexConsumer buffer = consumers.getBuffer(layer);

        Vec3d camPos = camera.getCameraPos();
        float half = size * 0.5f;

        matrices.push();
        matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        matrices.multiply(camera.getRotation());
        if (rotation != 0.0f) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }

        MatrixStack.Entry entry = matrices.peek();

        int a = (color >>> 24) & 0xFF;
        if (a == 0) a = 255;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;

        buffer.vertex(entry, -half, -half, 0.0f).color(r, g, b, a).texture(0.0f, 1.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);
        buffer.vertex(entry, half, -half, 0.0f).color(r, g, b, a).texture(1.0f, 1.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);
        buffer.vertex(entry, half, half, 0.0f).color(r, g, b, a).texture(1.0f, 0.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);
        buffer.vertex(entry, -half, half, 0.0f).color(r, g, b, a).texture(0.0f, 0.0f).overlay(0, 10).light(0xF000F0).normal(entry, 0.0f, 1.0f, 0.0f);

        matrices.pop();
    }
}
