package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public final class ChunkBordersModule extends Module implements Render3DListener {

    public final ColorSetting color = add(new ColorSetting("Цвет границ", 0xFFFF3366));
    public final BooleanSetting subChunkGrid = add(new BooleanSetting("Сетка суб-чанков (16x16x16)", true));
    public final BooleanSetting centerCross = add(new BooleanSetting("Центр чанка (8x8)", true));
    public final BooleanSetting wallFill = add(new BooleanSetting("Полупрозрачные стены", false));
    public final NumberSetting wallAlpha = add(new NumberSetting("Прозрачность стен", 0.12f, 0.05f, 0.5f, 0.02f));
    public final NumberSetting lineWidth = add(new NumberSetting("Толщина линий", 1.5f, 1.0f, 4.0f, 0.5f));
    public final NumberSetting heightRange = add(new NumberSetting("Диапазон высоты", 48.0f, 16.0f, 128.0f, 8.0f));

    public ChunkBordersModule() {
        super("Chunk Borders", "Стильные 3D границы и сетка текущего чанка", Category.NAVIGATION);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null) return;

        BlockPos pPos = mc.player.getBlockPos();
        ChunkPos chunkPos = new ChunkPos(pPos);

        double minX = chunkPos.getStartX();
        double minZ = chunkPos.getStartZ();
        double maxX = minX + 16.0;
        double maxZ = minZ + 16.0;

        double pY = mc.player.getY();
        double hRange = heightRange.get();
        double minY = Math.max(-64.0, pY - hRange);
        double maxY = Math.min(320.0, pY + hRange);

        int col = color.getEffectiveColor();
        float lw = lineWidth.get();
        int subCol = (0x66000000) | (col & 0x00FFFFFF);

        // 4 угловых вертикальных столба чанка
        Render3D.drawLine(new Vec3d(minX, minY, minZ), new Vec3d(minX, maxY, minZ), col, lw);
        Render3D.drawLine(new Vec3d(maxX, minY, minZ), new Vec3d(maxX, maxY, minZ), col, lw);
        Render3D.drawLine(new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, maxY, maxZ), col, lw);
        Render3D.drawLine(new Vec3d(minX, minY, maxZ), new Vec3d(minX, maxY, maxZ), col, lw);

        if (wallFill.get()) {
            int fillA = (int) (wallAlpha.get() * 255.0f);
            int fillCol = (col & 0x00FFFFFF) | (fillA << 24);
            Vec3d c1 = new Vec3d(minX, minY, minZ);
            Vec3d c2 = new Vec3d(maxX, minY, minZ);
            Vec3d c3 = new Vec3d(maxX, minY, maxZ);
            Vec3d c4 = new Vec3d(minX, minY, maxZ);

            Vec3d t1 = new Vec3d(minX, maxY, minZ);
            Vec3d t2 = new Vec3d(maxX, maxY, minZ);
            Vec3d t3 = new Vec3d(maxX, maxY, maxZ);
            Vec3d t4 = new Vec3d(minX, maxY, maxZ);

            Render3D.drawGradientQuad(c1, c2, t2, t1, fillCol, fillCol, 0, 0);
            Render3D.drawGradientQuad(c2, c3, t3, t2, fillCol, fillCol, 0, 0);
            Render3D.drawGradientQuad(c3, c4, t4, t3, fillCol, fillCol, 0, 0);
            Render3D.drawGradientQuad(c4, c1, t1, t4, fillCol, fillCol, 0, 0);
        }

        // Горизонтальные кольца суб-чанков каждые 16 блоков по высоте
        if (subChunkGrid.get()) {
            int startSub = (int) Math.floor(minY / 16.0) * 16;
            for (int y = startSub; y <= maxY; y += 16) {
                if (y < minY) continue;
                Vec3d p1 = new Vec3d(minX, y, minZ);
                Vec3d p2 = new Vec3d(maxX, y, minZ);
                Vec3d p3 = new Vec3d(maxX, y, maxZ);
                Vec3d p4 = new Vec3d(minX, y, maxZ);

                Render3D.drawLine(p1, p2, subCol, lw * 0.8f);
                Render3D.drawLine(p2, p3, subCol, lw * 0.8f);
                Render3D.drawLine(p3, p4, subCol, lw * 0.8f);
                Render3D.drawLine(p4, p1, subCol, lw * 0.8f);
            }
        }

        // Центр чанка (блок 8, 8)
        if (centerCross.get()) {
            double cX = minX + 8.0;
            double cZ = minZ + 8.0;
            int centerColor = 0xFF00CEC9;
            Render3D.drawLine(new Vec3d(cX, minY, cZ), new Vec3d(cX, maxY, cZ), centerColor, lw * 1.2f);
        }
    }
}
