package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.NameTagsModule;
import com.lowcode.svintusclient.client.module.impl.render.NoFireModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$cancelVanillaPlayerLabel(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue commandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        NameTagsModule nameTags = ModuleManager.get(NameTagsModule.class);
        if (nameTags != null && nameTags.isEnabled() && state instanceof PlayerEntityRenderState) {
            ci.cancel();
        }
    }

    @Inject(method = "updateRenderState", at = @At("TAIL"), require = 0)
    private void svintus$noEntityEffects(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        NoFireModule noFire = ModuleManager.get(NoFireModule.class);
        if (noFire != null && noFire.isEnabled() && noFire.hideEntityFire.get()) {
            state.onFire = false;
        }

        com.lowcode.svintusclient.client.module.impl.render.PerformanceModule perf = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.PerformanceModule.class);
        if (perf != null && perf.isEnabled()) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null && perf.shouldSkipFarEntity(entity.squaredDistanceTo(mc.player))) {
                // Far entity animation suppression
            }
        }
    }
}
