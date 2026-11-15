package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class ChinaHatModule extends Module {

    public final ModeSetting targetMode = add(new ModeSetting("Кому рисовать", "Только себе", "Себе и союзникам", "Всем игрокам"));
    public final ModeSetting style = add(new ModeSetting("Стиль", "Классика", "Градиент", "Обод", "Зонтик"));
    public final NumberSetting radius = add(new NumberSetting("Радиус", 0.65f, 0.3f, 1.2f, 0.05f));
    public final NumberSetting height = add(new NumberSetting("Высота", 0.25f, 0.1f, 0.6f, 0.05f));
    public final NumberSetting yOffset = add(new NumberSetting("Смещение Y", 0.35f, 0.0f, 0.8f, 0.05f));
    public final NumberSetting segments = add(new NumberSetting("Сегменты", 36f, 12f, 64f, 2f));
    public final NumberSetting fillAlpha = add(new NumberSetting("Альфа заливки", 0.45f, 0.1f, 1.0f, 0.05f));
    public final BooleanSetting rotation = add(new BooleanSetting("Вращение", true));
    public final BooleanSetting fill = add(new BooleanSetting("Заливка", true));
    public final ColorSetting topColor = add(new ColorSetting("Цвет вершины", 0xFF6C5CE7));
    public final ColorSetting rimColor = add(new ColorSetting("Цвет обода", 0xFF00CEC9));

    public ChinaHatModule() {
        super("China Hat", "3D коническая шляпа над головой игрока с градиентом", Category.COSMETICS);
    }

    public void renderForPlayer(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, PlayerEntity player, float tickDelta) {
        if (!isEnabled() || player == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            boolean isSelf = (player == mc.player);
            String tm = targetMode.get();
            if ("Только себе".equals(tm) && !isSelf) return;
            if ("Себе и союзникам".equals(tm) && !isSelf) {
                FriendHighlightModule friends = ModuleManager.get(FriendHighlightModule.class);
                if (friends == null || !friends.isFriend(player.getNameForScoreboard())) return;
            }
        }

        Vec3d headPos = Render3D.interpolate(player, tickDelta).add(0, player.getEyeHeight(player.getPose()) + yOffset.get(), 0);
        float r = radius.get();
        float coneHeight = height.get();
        int segs = (int) segments.get().floatValue();
        String currentStyle = style.get();
        int topCol = "Классика".equals(currentStyle) ? rimColor.getEffectiveColor() : topColor.getEffectiveColor();
        int rimCol = rimColor.getEffectiveColor();
        float alpha = fillAlpha.get();

        int topA = (topCol >> 24) & 0xFF;
        if (topA == 0) topA = 255;
        int rimA = (rimCol >> 24) & 0xFF;
        if (rimA == 0) rimA = 255;

        int topFill = ((int) (topA * alpha) << 24) | (topCol & 0x00FFFFFF);
        int rimFill = ((int) (rimA * alpha) << 24) | (rimCol & 0x00FFFFFF);

        float angleOffset = rotation.get() ? (System.currentTimeMillis() % 3600L) / 10.0f : 0.0f;
        Vec3d tip = headPos.add(0, coneHeight, 0);

        for (int i = 0; i < segs; i++) {
            double a1 = Math.toRadians((i * 360.0 / segs) + angleOffset);
            double a2 = Math.toRadians(((i + 1) * 360.0 / segs) + angleOffset);

            float r1 = r;
            float r2 = r;
            if ("Зонтик".equals(currentStyle)) {
                r1 *= (1.0f + 0.15f * (float) Math.sin(a1 * 8.0));
                r2 *= (1.0f + 0.15f * (float) Math.sin(a2 * 8.0));
            }

            Vec3d p1 = headPos.add(Math.cos(a1) * r1, 0, Math.sin(a1) * r1);
            Vec3d p2 = headPos.add(Math.cos(a2) * r2, 0, Math.sin(a2) * r2);

            if (fill.get() && !"Обод".equals(currentStyle)) {
                Render3D.drawGradientTriangle(tip, p1, p2, topFill, rimFill, rimFill);
            }

            if (!"Обод".equals(currentStyle)) {
                Render3D.drawLine(tip, p1, topCol, 1.2f);
            }
            Render3D.drawLine(p1, p2, rimCol, 2.0f);
        }
    }
}
