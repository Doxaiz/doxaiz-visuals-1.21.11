package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
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

public final class CrownModule extends Module implements Render3DListener {

    public final NumberSetting scale = add(new NumberSetting("Размер короны", 0.45f, 0.2f, 1.0f, 0.05f));
    public final NumberSetting yOffset = add(new NumberSetting("Смещение по Y", 0.35f, 0.1f, 0.8f, 0.05f));
    public final ModeSetting gemColor = add(new ModeSetting("Драгоценные камни", "Рубин", "Изумруд", "Сапфир", "Аметист", "Алмаз"));
    public final BooleanSetting rotate = add(new BooleanSetting("Вращение короны", true));
    public final NumberSetting rotationSpeed = add(new NumberSetting("Скорость вращения", 1.0f, 0.2f, 3.0f, 0.1f));
    public final BooleanSetting glow = add(new BooleanSetting("Сияние драгоценностей", true));

    public CrownModule() {
        super("Crown", "Парящая 3D золотая корона монарха над головой персонажа", Category.COSMETICS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null) return;

        PlayerEntity player = mc.player;
        float delta = tickCounter.getTickProgress(false);
        Vec3d pPos = player.getLerpedPos(delta);

        double bob = Math.sin(System.currentTimeMillis() / 250.0) * 0.04;
        double cy = pPos.y + player.getHeight() + yOffset.get() + bob;
        Vec3d center = new Vec3d(pPos.x, cy, pPos.z);

        float sz = scale.get();
        int goldColor = 0xFFFFD700;
        int goldFill = 0x55FFD700;
        int gemCol = resolveGemColor();

        double rot = rotate.get() ? ((System.currentTimeMillis() % 100000L) * 0.001 * rotationSpeed.get()) * Math.PI * 2.0 : 0.0;
        int peaks = 6;
        double step = (Math.PI * 2.0) / peaks;

        // Нижний и средний золотой обод
        Render3D.drawCircle(center, sz, 24, goldColor, 2.2f);
        Render3D.drawCircle(center.add(0, sz * 0.25, 0), sz * 1.05, 24, goldColor, 1.8f);

        // 6 золотых пиков короны с драгоценными камнями на вершинах
        for (int i = 0; i < peaks; i++) {
            double a1 = rot + i * step;
            double a2 = rot + (i + 0.5) * step;
            double a3 = rot + (i + 1) * step;

            Vec3d b1 = center.add(Math.cos(a1) * sz, sz * 0.25, Math.sin(a1) * sz);
            Vec3d top = center.add(Math.cos(a2) * sz * 1.15, sz * 0.75, Math.sin(a2) * sz * 1.15);
            Vec3d b2 = center.add(Math.cos(a3) * sz, sz * 0.25, Math.sin(a3) * sz);

            Render3D.drawLine(b1, top, goldColor, 2.0f);
            Render3D.drawLine(top, b2, goldColor, 2.0f);
            Render3D.drawGradientTriangle(top, b1, b2, goldFill, goldFill, goldFill);

            // Инкрустированный самоцвет на вершине каждого зубца
            Render3D.drawRotatedCube(top, sz * 0.15, rot * 2.0, 0, 0, gemCol, 0xFFFFFFFF, true, true, 1.0f);
            if (glow.get()) {
                Render3D.drawCircle(top, sz * 0.18, 12, (0x66 << 24) | (gemCol & 0x00FFFFFF), 1.2f);
            }
        }
    }

    private int resolveGemColor() {
        return switch (gemColor.get()) {
            case "Изумруд" -> 0xFF2ED573;
            case "Сапфир" -> 0xFF1E90FF;
            case "Аметист" -> 0xFF9B59B6;
            case "Алмаз" -> 0xFF00CEC9;
            default -> 0xFFFF4757; // Рубин
        };
    }
}
