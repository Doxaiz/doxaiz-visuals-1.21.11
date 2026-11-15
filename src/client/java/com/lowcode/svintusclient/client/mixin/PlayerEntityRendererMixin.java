package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.NameTagsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    @Inject(
        method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void svintus$cancelVanillaPlayerLabel(
        PlayerEntityRenderState state,
        MatrixStack matrices,
        OrderedRenderCommandQueue commandQueue,
        CameraRenderState cameraRenderState,
        CallbackInfo ci
    ) {
        NameTagsModule nameTags = ModuleManager.get(NameTagsModule.class);
        if (nameTags != null && nameTags.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(
        method = "hasLabel(Lnet/minecraft/entity/Entity;D)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void svintus$cancelHasLabel(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        NameTagsModule nameTags = ModuleManager.get(NameTagsModule.class);
        if (nameTags != null && nameTags.isEnabled()) {
            cir.setReturnValue(false);
        }
    }
}
