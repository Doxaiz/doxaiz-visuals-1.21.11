package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.NoFireModule;
import com.lowcode.svintusclient.client.module.impl.render.WorldAmbienceModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void svintus$onRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        WorldAmbienceModule wa = ModuleManager.get(WorldAmbienceModule.class);
        if (wa != null && wa.isEnabled() && wa.hideScreenFire.get()) {
            ci.cancel();
            return;
        }
        NoFireModule module = ModuleManager.get(NoFireModule.class);
        if (module != null && module.isEnabled()) {
            float h = module.getEffectiveHeight();
            if (h <= 0.01f) {
                ci.cancel();
                return;
            }
            if (h < 0.99f && matrices != null) {
                matrices.translate(0.0f, -(1.0f - h) * 0.45f, 0.0f);
            }
        }
    }
}
