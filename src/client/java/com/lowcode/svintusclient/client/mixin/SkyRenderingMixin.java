package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.CustomSkyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(SkyRendering.class)
public abstract class SkyRenderingMixin {

    @Inject(method = "updateRenderState", at = @At("RETURN"), require = 0)
    private void svintus$onUpdateSkyRenderState(ClientWorld world, float tickDelta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        CustomSkyModule customSky = ModuleManager.get(CustomSkyModule.class);
        if (customSky != null && customSky.isEnabled()) {
            state.skyColor = customSky.getEffectiveSkyColor();
            if (customSky.stars.get()) {
                state.starBrightness = Math.max(state.starBrightness, 0.9f);
            }
        }
    }
}
