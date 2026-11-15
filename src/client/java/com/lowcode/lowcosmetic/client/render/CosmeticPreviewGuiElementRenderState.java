package com.lowcode.lowcosmetic.client.render;

import com.lowcode.lowcosmetic.client.cosmetic.Cosmetic;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

public record CosmeticPreviewGuiElementRenderState(
        Cosmetic cosmetic,
        int x1,
        int y1,
        int x2,
        int y2,
        float w,
        float h,
        float yaw,
        float pitch,
        float seconds,
        Matrix3x2f pose,
        ScreenRect scissorArea,
        ScreenRect bounds
) implements SpecialGuiElementRenderState {

    public CosmeticPreviewGuiElementRenderState(
            Cosmetic cosmetic,
            int x1, int y1, int x2, int y2,
            float w, float h,
            float yaw, float pitch, float seconds,
            Matrix3x2f pose,
            ScreenRect scissorArea) {
        this(cosmetic, x1, y1, x2, y2, w, h, yaw, pitch, seconds, pose, scissorArea,
                computeBounds(x1, y1, x2, y2, pose, scissorArea));
    }

    private static ScreenRect computeBounds(int x1, int y1, int x2, int y2, Matrix3x2f pose, ScreenRect scissorArea) {
        if (pose == null) {
            return SpecialGuiElementRenderState.createBounds(x1, y1, x2, y2, scissorArea);
        }
        Vector2f a = pose.transformPosition(x1, y1, new Vector2f());
        Vector2f b = pose.transformPosition(x2, y1, new Vector2f());
        Vector2f c = pose.transformPosition(x2, y2, new Vector2f());
        Vector2f d = pose.transformPosition(x1, y2, new Vector2f());
        int minX = (int) Math.floor(Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)));
        int minY = (int) Math.floor(Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)));
        int maxX = (int) Math.ceil(Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)));
        int maxY = (int) Math.ceil(Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)));
        ScreenRect rect = new ScreenRect(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
        return scissorArea != null ? scissorArea.intersection(rect) : rect;
    }

    @Override
    public float scale() {
        return 1.0f;
    }
}
