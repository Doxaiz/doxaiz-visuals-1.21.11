package com.lowcode.svintusclient.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.BlockOverlayModule;
import com.lowcode.svintusclient.client.module.impl.render.ChunksAnimationModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.DynamicUniforms;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "renderTargetBlockOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$cancelVanillaBlockOutline(VertexConsumerProvider.Immediate consumers, MatrixStack matrices, boolean translucent, WorldRenderState state, CallbackInfo ci) {
        BlockOverlayModule blockOverlay = ModuleManager.get(BlockOverlayModule.class);
        if (blockOverlay != null && blockOverlay.isEnabled() && blockOverlay.hideVanillaOutline.get()) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "renderBlockLayers",
        at = @At(value = "NEW", target = "(Lorg/joml/Matrix4fc;IIIFII)Lnet/minecraft/client/gl/DynamicUniforms$ChunkSectionsValue;"),
        require = 0
    )
    private DynamicUniforms.ChunkSectionsValue svintus$animateChunk(
        Matrix4fc matrix, int x, int y, int z, float visibility, int a, int b,
        Operation<DynamicUniforms.ChunkSectionsValue> original
    ) {
        ChunksAnimationModule anim = ModuleManager.get(ChunksAnimationModule.class);
        if (anim != null && anim.isEnabled()) {
            float offsetY = anim.getChunkOffsetY(x >> 4, z >> 4);
            int newY = y + Math.round(offsetY);
            return original.call(matrix, x, newY, z, visibility, a, b);
        }
        return original.call(matrix, x, y, z, visibility, a, b);
    }
}
