package com.lowcode.lowcosmetic.client.render;

import com.lowcode.lowcosmetic.client.cosmetic.Cosmetic;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import org.joml.Matrix3x2f;

public final class ModelPreview {
    private ModelPreview() {
    }

    public static void draw(DrawContext ctx, Cosmetic cosmetic, float cx, float cy, float boxW, float boxH,
                            float yaw, float swayRange, float pitch, float seconds) {
        if (cosmetic == null || cosmetic.model() == null) {
            return;
        }

        int x1 = (int) Math.floor(cx - boxW / 2.0f);
        int y1 = (int) Math.floor(cy - boxH / 2.0f);
        int x2 = (int) Math.ceil(cx + boxW / 2.0f);
        int y2 = (int) Math.ceil(cy + boxH / 2.0f);

        ScreenRect scissor = ctx.scissorStack.peekLast();
        Matrix3x2f pose = new Matrix3x2f(ctx.getMatrices());

        float effPitch = pitch;
        if (effPitch == 0f) {
            if (cosmetic.type() == CosmeticType.HAT) effPitch = 12.0f;
            else if (cosmetic.type() == CosmeticType.WINGS) effPitch = 10.0f;
            else if (cosmetic.type() == CosmeticType.PET) effPitch = 8.0f;
        }

        ctx.state.addSpecialElement(new CosmeticPreviewGuiElementRenderState(
                cosmetic, x1, y1, x2, y2, boxW, boxH, yaw, effPitch, seconds, pose, scissor
        ));
    }
}
