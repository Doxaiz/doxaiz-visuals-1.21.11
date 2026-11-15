package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class BlockOverlayModule extends Module implements Render3DListener {

    public final ModeSetting style = add(new ModeSetting("Стиль",
            "Полный бокс", "Только грани", "Только контур", "Верхняя грань", "Плавное смещение", "Волны (Shader)", "Нити / Сетка (Grid)"));
    public final BooleanSetting fill = add(new BooleanSetting("Заливка", true));
    public final BooleanSetting outline = add(new BooleanSetting("Контур", true));
    public final NumberSetting outlineWidth = add(new NumberSetting("Толщина линий", 2.0f, 1.0f, 6.0f, 0.5f));
    public final ColorSetting fillColor = add(new ColorSetting("Цвет заливки", 0x336C5CE7));
    public final ColorSetting outlineColor = add(new ColorSetting("Цвет контура", 0xFF6C5CE7));
    public final BooleanSetting hideVanillaOutline = add(new BooleanSetting("Скрыть ванильную рамку", true));

    private double smoothX, smoothY, smoothZ;
    private boolean hasTarget;

    public BlockOverlayModule() {
        super("Обводка блока", "Стильное выделение и подсветка целевого блока с анимацией", Category.VISUALS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.world == null || mc.crosshairTarget == null) {
            hasTarget = false;
            return;
        }
        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            hasTarget = false;
            return;
        }

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            hasTarget = false;
            return;
        }

        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) {
            hasTarget = false;
            return;
        }

        Vec3d camPos = camera.getCameraPos();
        Box targetBox = shape.getBoundingBox().offset(pos);
        String st = style.get();

        Box renderBox;
        if ("Плавное смещение".equals(st)) {
            if (!hasTarget) {
                smoothX = targetBox.minX;
                smoothY = targetBox.minY;
                smoothZ = targetBox.minZ;
                hasTarget = true;
            } else {
                double speed = 0.35;
                smoothX += (targetBox.minX - smoothX) * speed;
                smoothY += (targetBox.minY - smoothY) * speed;
                smoothZ += (targetBox.minZ - smoothZ) * speed;
            }
            double w = targetBox.maxX - targetBox.minX;
            double h = targetBox.maxY - targetBox.minY;
            double d = targetBox.maxZ - targetBox.minZ;
            renderBox = new Box(smoothX, smoothY, smoothZ, smoothX + w, smoothY + h, smoothZ + d);
        } else {
            hasTarget = false;
            renderBox = targetBox;
        }

        int fColor = fillColor.getEffectiveColor();
        int oColor = outlineColor.getEffectiveColor();
        float width = outlineWidth.get();

        boolean doFill = fill.get() && !"Только контур".equals(st);
        boolean doOutline = outline.get() && !"Только грани".equals(st);

        // 1. Draw outline using native VertexRendering.drawOutline
        if (doOutline) {
            VertexConsumer lineConsumer = consumers.getBuffer(RenderLayers.lines());
            if ("Плавное смещение".equals(st)) {
                VoxelShape smoothShape = VoxelShapes.cuboid(renderBox.offset(-pos.getX(), -pos.getY(), -pos.getZ()));
                VertexRendering.drawOutline(matrices, lineConsumer, smoothShape, pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z, oColor, width);
            } else if ("Верхняя грань".equals(st)) {
                Vec3d p1 = new Vec3d(renderBox.minX, renderBox.maxY + 0.005, renderBox.minZ);
                Vec3d p2 = new Vec3d(renderBox.maxX, renderBox.maxY + 0.005, renderBox.minZ);
                Vec3d p3 = new Vec3d(renderBox.maxX, renderBox.maxY + 0.005, renderBox.maxZ);
                Vec3d p4 = new Vec3d(renderBox.minX, renderBox.maxY + 0.005, renderBox.maxZ);
                Render3D.drawLine(p1, p2, oColor, width);
                Render3D.drawLine(p2, p3, oColor, width);
                Render3D.drawLine(p3, p4, oColor, width);
                Render3D.drawLine(p4, p1, oColor, width);
            } else {
                VertexRendering.drawOutline(matrices, lineConsumer, shape, pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z, oColor, width);
            }
            consumers.draw(RenderLayers.lines());
        }

        // 2. Draw fill / special face effects
        if (doFill) {
            if ("Верхняя грань".equals(st)) {
                Vec3d p1 = new Vec3d(renderBox.minX, renderBox.maxY + 0.005, renderBox.minZ);
                Vec3d p2 = new Vec3d(renderBox.maxX, renderBox.maxY + 0.005, renderBox.minZ);
                Vec3d p3 = new Vec3d(renderBox.maxX, renderBox.maxY + 0.005, renderBox.maxZ);
                Vec3d p4 = new Vec3d(renderBox.minX, renderBox.maxY + 0.005, renderBox.maxZ);
                Render3D.drawGradientTriangle(p1, p2, p3, fColor, fColor, fColor);
                Render3D.drawGradientTriangle(p1, p3, p4, fColor, fColor, fColor);
            } else {
                Render3D.drawBox(renderBox, fColor, oColor, true, false, width);
            }
        }

        // 3. Special styles (Shader wave, Grid)
        if ("Волны (Shader)".equals(st)) {
            long now = System.currentTimeMillis();
            float t = (now % 3000L) / 3000.0f;
            double waveY = renderBox.minY + (renderBox.maxY - renderBox.minY) * (0.5 + 0.5 * Math.sin(t * Math.PI * 2));
            Render3D.drawLine(new Vec3d(renderBox.minX, waveY, renderBox.minZ), new Vec3d(renderBox.maxX, waveY, renderBox.minZ), oColor, width * 1.5f);
            Render3D.drawLine(new Vec3d(renderBox.maxX, waveY, renderBox.minZ), new Vec3d(renderBox.maxX, waveY, renderBox.maxZ), oColor, width * 1.5f);
            Render3D.drawLine(new Vec3d(renderBox.maxX, waveY, renderBox.maxZ), new Vec3d(renderBox.minX, waveY, renderBox.maxZ), oColor, width * 1.5f);
            Render3D.drawLine(new Vec3d(renderBox.minX, waveY, renderBox.maxZ), new Vec3d(renderBox.minX, waveY, renderBox.minZ), oColor, width * 1.5f);
        } else if ("Нити / Сетка (Grid)".equals(st)) {
            double midX = (renderBox.minX + renderBox.maxX) / 2.0;
            double midY = (renderBox.minY + renderBox.maxY) / 2.0;
            double midZ = (renderBox.minZ + renderBox.maxZ) / 2.0;
            Render3D.drawLine(new Vec3d(midX, renderBox.minY, renderBox.minZ), new Vec3d(midX, renderBox.maxY, renderBox.minZ), oColor, 1.2f);
            Render3D.drawLine(new Vec3d(renderBox.minX, midY, renderBox.minZ), new Vec3d(renderBox.maxX, midY, renderBox.minZ), oColor, 1.2f);
            Render3D.drawLine(new Vec3d(midX, renderBox.minY, renderBox.maxZ), new Vec3d(midX, renderBox.maxY, renderBox.maxZ), oColor, 1.2f);
            Render3D.drawLine(new Vec3d(renderBox.minX, midY, renderBox.maxZ), new Vec3d(renderBox.maxX, midY, renderBox.maxZ), oColor, 1.2f);
        }
    }
}
