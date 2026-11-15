package com.lowcode.svintusclient.client.module;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public interface Render3DListener {
    void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter);
}
