package com.lowcode.svintusclient.client.module.impl.cosmetics;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
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

public final class DivineHaloModule extends Module implements Render3DListener {

    public final ModeSetting style = add(new ModeSetting("Стиль нимба", "Священный нимб", "Кибер-кольцо", "Двойной обод", "Шипастый нимб"));
    public final ColorSetting color = add(new ColorSetting("Цвет сияния", 0xFFFBBF24));
    public final NumberSetting radius = add(new NumberSetting("Радиус нимба", 0.38f, 0.2f, 0.8f, 0.02f));
    public final NumberSetting yOffset = add(new NumberSetting("Высота над головой", 0.32f, 0.1f, 0.7f, 0.02f));
    public final NumberSetting tilt = add(new NumberSetting("Наклон", 12.0f, 0.0f, 30.0f, 1.0f));

    public DivineHaloModule() {
        super("Divine Halo", "Парящий 3D сияющий ангельский или кибер-нимб над головой", Category.COSMETICS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null) return;

        PlayerEntity player = mc.player;
        float delta = tickCounter.getTickProgress(false);
        Vec3d pPos = player.getLerpedPos(delta);

        double bob = Math.sin(System.currentTimeMillis() / 350.0) * 0.03;
        double cy = pPos.y + player.getHeight() + yOffset.get() + bob;
        Vec3d center = new Vec3d(pPos.x, cy, pPos.z);

        float r = radius.get();
        int col = color.get();
        String st = style.get();
        double tRad = Math.toRadians(tilt.get());

        int segs = 32;
        double step = (2.0 * Math.PI) / segs;
        for (int i = 0; i < segs; i++) {
            double a1 = i * step;
            double a2 = (i + 1) * step;
            Vec3d p1 = center.add(Math.cos(a1) * r, Math.sin(a1) * r * Math.sin(tRad), Math.sin(a1) * r * Math.cos(tRad));
            Vec3d p2 = center.add(Math.cos(a2) * r, Math.sin(a2) * r * Math.sin(tRad), Math.sin(a2) * r * Math.cos(tRad));
            Render3D.drawLine(p1, p2, col, 2.5f);
        }

        if ("Двойной обод".equals(st)) {
            for (int i = 0; i < segs; i++) {
                double a1 = i * step;
                double a2 = (i + 1) * step;
                double r2 = r * 0.82;
                Vec3d p1 = center.add(Math.cos(a1) * r2, 0.05 + Math.sin(a1) * r2 * Math.sin(tRad), Math.sin(a1) * r2 * Math.cos(tRad));
                Vec3d p2 = center.add(Math.cos(a2) * r2, 0.05 + Math.sin(a2) * r2 * Math.sin(tRad), Math.sin(a2) * r2 * Math.cos(tRad));
                Render3D.drawLine(p1, p2, (col & 0x00FFFFFF) | 0x88000000, 1.8f);
            }
        } else if ("Кибер-кольцо".equals(st)) {
            double time = (System.currentTimeMillis() % 4000L) / 4000.0 * Math.PI * 2.0;
            int segments = 4;
            for (int i = 0; i < segments; i++) {
                double a = time + i * (Math.PI * 2.0 / segments);
                Vec3d node = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                Render3D.drawRotatedCube(node, 0.05, time * 2.0, 0, 0, col, 0xFFFFFFFF, true, true, 1.0f);
            }
        } else if ("Шипастый нимб".equals(st)) {
            int spikes = 8;
            for (int i = 0; i < spikes; i++) {
                double a = i * (Math.PI * 2.0 / spikes);
                Vec3d inner = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                Vec3d outer = center.add(Math.cos(a) * (r + 0.12), 0.04, Math.sin(a) * (r + 0.12));
                Render3D.drawLine(inner, outer, col, 2.0f);
            }
        }
    }
}
