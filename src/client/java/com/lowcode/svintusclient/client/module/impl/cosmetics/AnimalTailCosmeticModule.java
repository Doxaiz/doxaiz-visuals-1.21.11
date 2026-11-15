package com.lowcode.svintusclient.client.module.impl.cosmetics;

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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class AnimalTailCosmeticModule extends Module implements Render3DListener {

    public final ModeSetting tailType = add(new ModeSetting("Тип хвоста", "Лисий", "Кошачий", "Волчий", "Драконий"));
    public final ColorSetting color = add(new ColorSetting("Цвет хвоста", 0xFFF97316));
    public final ColorSetting tipColor = add(new ColorSetting("Цвет кончика", 0xFFFFFFFF));
    public final NumberSetting waveSpeed = add(new NumberSetting("Скорость покачивания", 1.0f, 0.5f, 2.5f, 0.1f));
    public final NumberSetting tailLength = add(new NumberSetting("Длина хвоста", 0.75f, 0.3f, 1.5f, 0.05f));
    public final BooleanSetting dynamicPhysics = add(new BooleanSetting("Физика при прыжках", true));

    public float getTailLength() {
        return isEnabled() ? tailLength.get() : 0.75f;
    }

    public boolean hasDynamicPhysics() {
        return isEnabled() && dynamicPhysics.get();
    }

    public AnimalTailCosmeticModule() {
        super("Animated Tail", "3D анимированный покачивающийся хвост персонажа", Category.COSMETICS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null) return;

        PlayerEntity player = mc.player;
        float delta = tickCounter.getTickProgress(false);
        Vec3d pPos = player.getLerpedPos(delta);

        float bodyYaw = (float) Math.toRadians(-player.getBodyYaw());
        double time = (System.currentTimeMillis() / 1000.0) * waveSpeed.get() * 3.0;

        // Позиция крепления к пояснице
        double waistY = pPos.y + player.getHeight() * 0.42;
        double backDist = 0.22;
        Vec3d base = new Vec3d(
                pPos.x - Math.sin(bodyYaw) * backDist,
                waistY,
                pPos.z - Math.cos(bodyYaw) * backDist
        );

        int col = color.get();
        int tip = tipColor.get();
        String type = tailType.get();
        int segments = "Драконий".equals(type) ? 7 : "Кошачий".equals(type) ? 6 : 5;
        double segThickness = "Лисий".equals(type) ? 0.08 : "Кошачий".equals(type) ? 0.045 : 0.06;

        Vec3d prev = base;
        for (int i = 1; i <= segments; i++) {
            double wave = Math.sin(time + i * 0.7) * (0.05 * i);
            double depth = i * 0.14;
            double drop = "Кошачий".equals(type) ? -(i * 0.015) : (i * i) * 0.025;

            Vec3d cur = new Vec3d(
                    base.x - Math.sin(bodyYaw) * depth + Math.cos(bodyYaw) * wave,
                    base.y - drop + Math.cos(time + i) * 0.03,
                    base.z - Math.cos(bodyYaw) * depth - Math.sin(bodyYaw) * wave
            );

            int segCol = (i == segments) ? tip : col;
            Render3D.drawLine(prev, cur, segCol, 3.5f - (i * 0.3f));
            Render3D.drawRotatedCube(cur, segThickness - (i * 0.005), 0, bodyYaw, 0, segCol, 0xFFFFFFFF, true, true, 1.0f);
            prev = cur;
        }
    }
}
