package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class RegionESPModule extends Module implements Render3DListener {

    public final ModeSetting style = add(new ModeSetting("Стиль стен",
            "Неоновая сетка (Grid)", "Сплошные стены (Walls)", "Колонны по углам", "Голограмма"));
    public final ColorSetting color = add(new ColorSetting("Цвет границ", 0xFFFF7675));
    public final NumberSetting wallHeight = add(new NumberSetting("Высота стен", 32.0f, 8.0f, 64.0f, 4.0f));
    public final NumberSetting wallAlpha = add(new NumberSetting("Прозрачность стен", 0.25f, 0.05f, 0.7f, 0.05f));
    public final BooleanSetting pulse = add(new BooleanSetting("Плавная пульсация", true));

    public RegionESPModule() {
        super("Region Borders", "Подсветка границ регионов и чанков с вертикальными неоновыми стенами и колоннами", Category.NAVIGATION);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        BlockPos pPos = mc.player.getBlockPos();
        int chunkX = (pPos.getX() >> 4) << 4;
        int chunkZ = (pPos.getZ() >> 4) << 4;

        double minY = Math.max(mc.player.getY() - 10, -64);
        double maxY = minY + wallHeight.get();

        Box b = new Box(chunkX, minY, chunkZ, chunkX + 16, maxY, chunkZ + 16);

        int baseCol = color.getEffectiveColor();
        float aMod = 1.0f;
        if (pulse.get()) {
            aMod = (float) (0.8 + 0.2 * Math.sin(System.currentTimeMillis() * 0.003));
        }

        int aI = Math.min(255, (int) (wallAlpha.get() * aMod * 255.0f));
        int fCol = (baseCol & 0x00FFFFFF) | (aI << 24);
        int oCol = (baseCol & 0x00FFFFFF) | 0xDD000000;

        String st = style.get();
        if ("Колонны по углам".equals(st)) {
            Render3D.drawLine(new Vec3d(b.minX, b.minY, b.minZ), new Vec3d(b.minX, b.maxY, b.minZ), oCol, 3.0f);
            Render3D.drawLine(new Vec3d(b.maxX, b.minY, b.minZ), new Vec3d(b.maxX, b.maxY, b.minZ), oCol, 3.0f);
            Render3D.drawLine(new Vec3d(b.maxX, b.minY, b.maxZ), new Vec3d(b.maxX, b.maxY, b.maxZ), oCol, 3.0f);
            Render3D.drawLine(new Vec3d(b.minX, b.minY, b.maxZ), new Vec3d(b.minX, b.maxY, b.maxZ), oCol, 3.0f);
        } else {
            Render3D.drawBox(b, fCol, oCol, true, true, 1.5f);
            if ("Неоновая сетка (Grid)".equals(st)) {
                for (double y = b.minY + 4; y < b.maxY; y += 4) {
                    Render3D.drawLine(new Vec3d(b.minX, y, b.minZ), new Vec3d(b.maxX, y, b.minZ), oCol, 1.0f);
                    Render3D.drawLine(new Vec3d(b.maxX, y, b.minZ), new Vec3d(b.maxX, y, b.maxZ), oCol, 1.0f);
                    Render3D.drawLine(new Vec3d(b.maxX, y, b.maxZ), new Vec3d(b.minX, y, b.maxZ), oCol, 1.0f);
                    Render3D.drawLine(new Vec3d(b.minX, y, b.maxZ), new Vec3d(b.minX, y, b.minZ), oCol, 1.0f);
                }
            }
        }
    }
}
