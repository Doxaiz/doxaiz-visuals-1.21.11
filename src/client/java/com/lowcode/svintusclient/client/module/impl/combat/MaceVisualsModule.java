package com.lowcode.svintusclient.client.module.impl.combat;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.util.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;

public final class MaceVisualsModule extends Module {

    public final BooleanSetting hudRing = add(new BooleanSetting("Круговой индикатор у прицела", true));
    public final BooleanSetting showPercent = add(new BooleanSetting("Процент заряда", true));
    public final ColorSetting chargeColor = add(new ColorSetting("Цвет индикатора", 0xFFFF9900));
    public final NumberSetting maxBlocks = add(new NumberSetting("Блоков для 100% заряда", 12.0f, 4.0f, 25.0f, 1.0f));

    public MaceVisualsModule() {
        super("Mace Visuals", "Индикатор заряда сокрушительного удара булавой при падении (1.21 Mace)", Category.BATTLE);
    }

    public void render(DrawContext ctx, int screenW, int screenH) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean holdingMace = mc.player.getMainHandStack().isOf(Items.MACE) || mc.player.getOffHandStack().isOf(Items.MACE);
        if (!holdingMace) return;

        float fallDistance = (float) mc.player.fallDistance;
        if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.1) {
            fallDistance = Math.max(fallDistance, (float) (-mc.player.getVelocity().y * 4.0));
        }
        if (fallDistance <= 0.4f) return;

        float max = maxBlocks.get();
        float progress = Math.min(1.0f, fallDistance / max);
        float cx = screenW / 2.0f;
        float cy = screenH / 2.0f;

        if (hudRing.get()) {
            int col = Colors.lerp(chargeColor.getEffectiveColor(), 0xFFFF2244, progress);
            Render2D.circleOutline(ctx, cx, cy, 14f, 1.8f, 0x44000000);
            int arcSegs = Math.max(2, (int) (progress * 28));
            for (int i = 0; i < arcSegs; i++) {
                double a = (i * 2.0 * Math.PI) / 28.0 - Math.PI / 2.0;
                float px = (float) (cx + Math.cos(a) * 14.0);
                float py = (float) (cy + Math.sin(a) * 14.0);
                Render2D.circle(ctx, px, py, 1.1f, col);
            }
        }

        if (showPercent.get()) {
            String text = progress >= 0.99f ? "MAX SMASH" : (int) (progress * 100f) + "%";
            int tCol = progress >= 0.99f ? 0xFFFF3366 : chargeColor.getEffectiveColor();
            Fonts.SEMIBOLD.drawCentered(ctx, text, cx, cy + 18f, 5.5f, tCol);
        }
    }
}
