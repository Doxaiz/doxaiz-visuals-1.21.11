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

public final class BodyAuraModule extends Module implements Render3DListener {

    public final ModeSetting auraType = add(new ModeSetting("Тип ауры", "Огненная аура", "Сердечки", "Галактическая пыль", "Магические руны", "Электрические дуги"));
    public final ColorSetting color = add(new ColorSetting("Цвет ауры", 0xFFF97316));
    public final NumberSetting radius = add(new NumberSetting("Радиус ауры", 0.75f, 0.4f, 1.8f, 0.05f));
    public final NumberSetting speed = add(new NumberSetting("Скорость вихря", 1.2f, 0.2f, 3.0f, 0.1f));
    public final NumberSetting density = add(new NumberSetting("Плотность", 6.0f, 2.0f, 12.0f, 1.0f));

    public BodyAuraModule() {
        super("Body Aura", "3D вихревая аура вокруг персонажа (пламя, руны, сердечки, молнии)", Category.COSMETICS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null) return;

        PlayerEntity player = mc.player;
        float delta = tickCounter.getTickProgress(false);
        Vec3d pPos = player.getLerpedPos(delta);

        int count = Math.round(density.get());
        float r = radius.get();
        double spd = speed.get();
        double time = (System.currentTimeMillis() / 1000.0) * spd;
        int col = color.get();

        String type = auraType.get();
        double height = player.getHeight();

        for (int i = 0; i < count; i++) {
            double progress = ((time * 0.4 + (double) i / count) % 1.0);
            double y = pPos.y + progress * (height + 0.3);
            double angle = time * 2.5 + i * (Math.PI * 2.0 / count);

            double currentR = r * Math.sin(progress * Math.PI);
            double x = pPos.x + Math.cos(angle) * currentR;
            double z = pPos.z + Math.sin(angle) * currentR;
            Vec3d pt = new Vec3d(x, y, z);

            int alpha = (int) ((1.0 - progress) * 220);
            int ptCol = (col & 0x00FFFFFF) | (alpha << 24);

            if ("Огненная аура".equals(type)) {
                Render3D.drawRotatedCube(pt, 0.08, time * 3.0, 0, 0, ptCol, 0xFFFFFFFF, true, true, 1.0f);
            } else if ("Сердечки".equals(type)) {
                Render3D.drawCircle(pt, 0.1, 12, 0xFFF43F5E, 1.8f);
                Render3D.drawRotatedCube(pt, 0.05, 0, time * 2.0, 0, 0xFFFB7185, 0xFFFFFFFF, true, true, 1.0f);
            } else if ("Электрические дуги".equals(type)) {
                Vec3d next = pt.add(Math.sin(time * 5.0 + i) * 0.15, 0.1, Math.cos(time * 5.0 + i) * 0.15);
                Render3D.drawLine(pt, next, 0xFF38BDF8, 2.0f);
            } else if ("Магические руны".equals(type)) {
                Render3D.drawCircle(pt, 0.12, 16, ptCol, 1.8f);
                Render3D.drawRotatedCube(pt, 0.06, time * 4.0, time * 2.0, 0, ptCol, 0xFFFFFFFF, true, true, 1.2f);
            } else {
                // Галактическая пыль
                Render3D.drawRotatedCube(pt, 0.05, time * 2.0, 0, 0, ptCol, 0xFFFFFFFF, true, true, 1.0f);
            }
        }
    }
}
