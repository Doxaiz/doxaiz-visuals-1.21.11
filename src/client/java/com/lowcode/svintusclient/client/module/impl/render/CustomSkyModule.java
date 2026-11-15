package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class CustomSkyModule extends Module implements Render3DListener, TickListener {

    public final ModeSetting mode = add(new ModeSetting("Стиль неба",
            "Звездная туманность", "Кибер-закат", "Глубокий космос", "Северное сияние",
            "Кровавая луна", "Синтвейв 80s", "Сумерки", "Галактика",
            "Лавандовый сон", "Кислотный токсик", "Океанская бездна", "Кастом"));
    public final ColorSetting skyColor = add(new ColorSetting("Цвет неба", 0xFF6C5CE7));

    // Stars Controls
    public final BooleanSetting stars = add(new BooleanSetting("Мерцающие звезды", true));
    public final NumberSetting starCount = add(new NumberSetting("Количество звезд", 120f, 40f, 300f, 10f));
    public final NumberSetting starSize = add(new NumberSetting("Размер звезд", 1.2f, 0.5f, 3.0f, 0.1f));
    public final BooleanSetting starTwinkle = add(new BooleanSetting("Мерцание звезд", true));

    // Time Changer Integration
    public final ModeSetting timePreset = add(new ModeSetting("Время суток",
            "Ванильное", "День (Day)", "Закат (Sunset)", "Полночь (Midnight)", "Кровавая луна (Blood Moon)", "Кастомное"));
    public final NumberSetting customTime = add(new NumberSetting("Время (тики)", 6000f, 0f, 24000f, 500f));

    // Fog & Ambience Controls
    public final ModeSetting fogMode = add(new ModeSetting("Режим тумана",
            "Ванильный", "Без тумана", "Кастомный", "Неоновая дымка"));
    public final NumberSetting fogDistance = add(new NumberSetting("Дистанция тумана", 45.0f, 5.0f, 200.0f, 5.0f));
    public final ColorSetting fogColor = add(new ColorSetting("Цвет тумана", 0xFF6C5CE7));

    private final List<Vec3d> starPoints = new ArrayList<>();

    public CustomSkyModule() {
        super("Небо и Время", "Кастомные космические скайбоксы, заморозка времени и управление туманом", Category.VISUALS);
        generateStars();
    }

    private void generateStars() {
        starPoints.clear();
        int count = Math.round(starCount.get());
        for (int i = 0; i < count; i++) {
            double u = Math.random();
            double v = Math.random();
            double theta = u * 2.0 * Math.PI;
            double phi = Math.acos(2.0 * v - 1.0);
            double r = 100.0;
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = Math.abs(r * Math.cos(phi)) + 15.0; // только верхняя полусфера
            double z = r * Math.sin(phi) * Math.sin(theta);
            starPoints.add(new Vec3d(x, y, z));
        }
    }

    public long getClientTime() {
        if (!isEnabled()) return -1;
        return switch (timePreset.get()) {
            case "День (Day)" -> 6000L;
            case "Закат (Sunset)" -> 12500L;
            case "Полночь (Midnight)" -> 18000L;
            case "Кровавая луна (Blood Moon)" -> 16000L;
            case "Кастомное" -> customTime.get().longValue();
            default -> -1L;
        };
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        long t = getClientTime();
        if (t >= 0) {
            mc.world.setTime(mc.world.getTime(), t, false);
        }

        if (starPoints.size() != Math.round(starCount.get())) {
            generateStars();
        }
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.world == null || camera == null) return;

        Vec3d camPos = camera.getCameraPos();
        double rot = (System.currentTimeMillis() % 160000L) / 160000.0 * Math.PI * 2.0;

        if (stars.get()) {
            float sz = starSize.get();
            boolean twinkle = starTwinkle.get();

            for (int i = 0; i < starPoints.size(); i++) {
                Vec3d sp = starPoints.get(i);
                // Вращение звезд вокруг зенита
                double x = sp.x * Math.cos(rot) - sp.z * Math.sin(rot);
                double z = sp.x * Math.sin(rot) + sp.z * Math.cos(rot);
                Vec3d starWorld = camPos.add(x, sp.y, z);

                float tw = twinkle ? ((float) Math.sin(System.currentTimeMillis() / 200.0 + i) * 0.5f + 0.5f) : 1.0f;
                int alpha = (int) (140 + tw * 115);
                int tint = "Кастом".equals(mode.get()) ? (skyColor.getEffectiveColor() & 0x00FFFFFF) : 0x00FFFFFF;
                int starCol = (alpha << 24) | tint;

                float half = 0.2f * sz;
                Render3D.drawLine(starWorld.add(0, -half, 0), starWorld.add(0, half, 0), starCol, 1.8f);
                Render3D.drawLine(starWorld.add(-half, 0, 0), starWorld.add(half, 0, 0), starCol, 1.8f);
            }
        }
    }

    public int getEffectiveSkyColor() {
        return switch (mode.get()) {
            case "Звездная туманность" -> 0xFF2D114D;
            case "Кибер-закат" -> 0xFFFF2A6D;
            case "Глубокий космос" -> 0xFF050510;
            case "Северное сияние" -> 0xFF00B894;
            case "Кровавая луна" -> 0xFFD63031;
            case "Синтвейв 80s" -> 0xFF6C5CE7;
            case "Сумерки" -> 0xFF192A56;
            case "Галактика" -> 0xFF341F97;
            case "Лавандовый сон" -> 0xFFE056FD;
            case "Кислотный токсик" -> 0xFF00E676;
            case "Океанская бездна" -> 0xFF00CEC9;
            default -> skyColor.getEffectiveColor();
        };
    }

    public int getEffectiveFogColor() {
        return fogColor.getEffectiveColor();
    }
}
