package com.lowcode.lowcosmetic.client.render;

import com.lowcode.lowcosmetic.client.cosmetic.Cosmetic;
import com.lowcode.lowcosmetic.client.cosmetic.CosmeticModel;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.render.SpecialGuiElementRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.TexturedQuadGuiElementRenderState;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

public class CosmeticPreviewGuiElementRenderer extends SpecialGuiElementRenderer<CosmeticPreviewGuiElementRenderState> {

    private static final class PreviewSlot implements AutoCloseable {
        GpuTexture texture;
        GpuTextureView textureView;
        GpuTexture depthTexture;
        GpuTextureView depthTextureView;
        int width = -1;
        int height = -1;

        void ensureSize(int w, int h, int id) {
            if (texture != null && width == w && height == h) {
                return;
            }
            close();
            this.width = w;
            this.height = h;
            GpuDevice device = RenderSystem.getDevice();
            this.texture = device.createTexture(() -> "cosmetic_preview_" + id, 12, TextureFormat.RGBA8, w, h, 1, 1);
            this.textureView = device.createTextureView(this.texture);
            this.depthTexture = device.createTexture(() -> "cosmetic_preview_depth_" + id, 8, TextureFormat.DEPTH32, w, h, 1, 1);
            this.depthTextureView = device.createTextureView(this.depthTexture);
        }

        @Override
        public void close() {
            if (textureView != null) {
                textureView.close();
                textureView = null;
            }
            if (texture != null) {
                texture.close();
                texture = null;
            }
            if (depthTextureView != null) {
                depthTextureView.close();
                depthTextureView = null;
            }
            if (depthTexture != null) {
                depthTexture.close();
                depthTexture = null;
            }
            width = -1;
            height = -1;
        }
    }

    private final Int2ObjectOpenHashMap<PreviewSlot> slots = new Int2ObjectOpenHashMap<>();
    private final ProjectionMatrix2 projectionMatrix = new ProjectionMatrix2("cosmetic_preview", -1000.0f, 1000.0f, true);

    public CosmeticPreviewGuiElementRenderer(VertexConsumerProvider.Immediate vertexConsumers) {
        super(vertexConsumers);
    }

    @Override
    public Class<CosmeticPreviewGuiElementRenderState> getElementClass() {
        return CosmeticPreviewGuiElementRenderState.class;
    }

    @Override
    protected float getYOffset(int height, int scale) {
        return (float) height / 2.0f;
    }

    @Override
    public void render(CosmeticPreviewGuiElementRenderState state, GuiRenderState guiRenderState, int scale) {
        Cosmetic cosmetic = state.cosmetic();
        if (cosmetic == null || cosmetic.model() == null) return;

        int width = (state.x2() - state.x1()) * scale;
        int height = (state.y2() - state.y1()) * scale;
        if (width <= 0 || height <= 0) return;

        PreviewSlot slot = slots.computeIfAbsent(cosmetic.id(), id -> new PreviewSlot());
        slot.ensureSize(width, height, cosmetic.id());

        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder encoder = device.createCommandEncoder();
        encoder.clearColorAndDepthTextures(slot.texture, 0, slot.depthTexture, 1.0);

        RenderSystem.setProjectionMatrix(this.projectionMatrix.set((float) width, (float) height), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.outputColorTextureOverride = slot.textureView;
        RenderSystem.outputDepthTextureOverride = slot.depthTextureView;

        MatrixStack matrixStack = new MatrixStack();
        matrixStack.translate((float) width / 2.0f, this.getYOffset(height, scale), 0.0f);
        float s = (float) scale * state.scale();
        matrixStack.scale(s, s, -s);

        this.render(state, matrixStack);
        this.vertexConsumers.draw();

        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;

        guiRenderState.addSimpleElementToCurrentLayer(new TexturedQuadGuiElementRenderState(
                RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
                TextureSetup.of(slot.textureView, RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST)),
                state.pose(),
                state.x1(),
                state.y1(),
                state.x2(),
                state.y2(),
                0.0f, 1.0f, 1.0f, 0.0f,
                -1,
                state.scissorArea(),
                null
        ));
    }

    @Override
    protected void render(CosmeticPreviewGuiElementRenderState state, MatrixStack matrices) {
        Cosmetic cosmetic = state.cosmetic();
        if (cosmetic == null || cosmetic.model() == null) return;

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

        CosmeticModel model = cosmetic.model();
        float[] b = model.bounds;
        float dx = Math.max(0.08f, b[3] - b[0]);
        float dy = Math.max(0.08f, b[4] - b[1]);
        float dz = Math.max(0.08f, b[5] - b[2]);
        float wide = (float) Math.sqrt(dx * dx + dz * dz);
        float tall = dy;
        float boxW = state.w();
        float boxH = state.h();
        float size = Math.min(boxW / Math.max(wide, 0.05f), boxH / Math.max(tall, 0.05f)) * 0.78f;

        matrices.push();
        matrices.scale(size, -size, size);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.yaw()));
        matrices.translate(-(b[0] + b[3]) / 2.0F, -(b[1] + b[4]) / 2.0F, -(b[2] + b[5]) / 2.0F);

        GeoRenderer.render(model.geo, model.animation(CosmeticModel.Motion.IDLE, false), state.seconds(), matrices,
                this.vertexConsumers.getBuffer(RenderLayers.entityCutoutNoCull(cosmetic.texture())),
                LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
        matrices.pop();
    }

    @Override
    public void close() {
        super.close();
        for (PreviewSlot slot : slots.values()) {
            slot.close();
        }
        slots.clear();
        this.projectionMatrix.close();
    }

    @Override
    protected String getName() {
        return "cosmetic_preview";
    }
}
