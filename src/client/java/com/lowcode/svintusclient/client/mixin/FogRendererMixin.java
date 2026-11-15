package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.NoFogModule;
import com.lowcode.svintusclient.client.module.impl.render.CustomSkyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AtmosphericFogModifier.class)
public abstract class FogRendererMixin {

    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void svintus$onGetFogColor(ClientWorld world, Camera camera, int viewDistance, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        CustomSkyModule ambience = ModuleManager.get(CustomSkyModule.class);
        if (ambience != null && ambience.isEnabled()) {
            String fm = ambience.fogMode.get();
            if ("Кастомный".equals(fm) || "Неоновая дымка".equals(fm)) {
                cir.setReturnValue(ambience.getEffectiveFogColor());
                return;
            }
        }
        com.lowcode.svintusclient.client.module.impl.render.WorldAmbienceModule wAmbience = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.WorldAmbienceModule.class);
        if (wAmbience != null && wAmbience.isEnabled()) {
            String fm = wAmbience.fogMode.get();
            if ("Кастомный".equals(fm) || "Неоновая дымка".equals(fm)) {
                cir.setReturnValue(wAmbience.fogColor.getEffectiveColor());
            }
        }
    }

    @Inject(method = "applyStartEndModifier", at = @At("TAIL"))
    private void svintus$onApplyFog(
            FogData data,
            Camera camera,
            ClientWorld world,
            float viewDistance,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        CustomSkyModule ambience = ModuleManager.get(CustomSkyModule.class);
        if (ambience != null && ambience.isEnabled()) {
            String fm = ambience.fogMode.get();
            if ("Без тумана".equals(fm)) {
                data.environmentalStart = 999999.0f;
                data.environmentalEnd = 999999.0f;
                data.renderDistanceStart = 999999.0f;
                data.renderDistanceEnd = 999999.0f;
                data.skyEnd = 999999.0f;
                data.cloudEnd = 999999.0f;
                return;
            } else if ("Кастомный".equals(fm) || "Неоновая дымка".equals(fm)) {
                float dist = ambience.fogDistance.get();
                int fc = ambience.getEffectiveFogColor();
                int sc = ambience.getEffectiveSkyColor();
                data.environmentalStart = dist * 0.35f;
                data.environmentalEnd = dist;
                data.renderDistanceStart = dist * 0.35f;
                data.renderDistanceEnd = dist;
                return;
            }
        }

        com.lowcode.svintusclient.client.module.impl.render.WorldAmbienceModule wAmbience = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.WorldAmbienceModule.class);
        if (wAmbience != null && wAmbience.isEnabled()) {
            String fm = wAmbience.fogMode.get();
            if ("Без тумана".equals(fm)) {
                data.environmentalStart = 999999.0f;
                data.environmentalEnd = 999999.0f;
                data.renderDistanceStart = 999999.0f;
                data.renderDistanceEnd = 999999.0f;
                data.skyEnd = 999999.0f;
                data.cloudEnd = 999999.0f;
                return;
            } else if ("Кастомный".equals(fm) || "Неоновая дымка".equals(fm)) {
                float dist = wAmbience.fogDistance.get();
                data.environmentalStart = dist * 0.35f;
                data.environmentalEnd = dist;
                data.renderDistanceStart = dist * 0.35f;
                data.renderDistanceEnd = dist;
                return;
            }
        }

        

        NoFogModule noFog = ModuleManager.get(NoFogModule.class);
        if (noFog != null && noFog.isEnabled()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && (mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.DARKNESS) || mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS))) {
                if (!noFog.darknessFog.get()) return;
            }
            float noFogDist = noFog.terrainFog.get() ? noFog.customDistance.get() : 999999.0f;
            if (camera.getSubmersionType() == net.minecraft.block.enums.CameraSubmersionType.WATER && !noFog.waterFog.get()) return;
            if (camera.getSubmersionType() == net.minecraft.block.enums.CameraSubmersionType.LAVA && !noFog.lavaFog.get()) return;
            data.environmentalStart = noFogDist;
            data.environmentalEnd = noFogDist;
            data.renderDistanceStart = noFogDist;
            data.renderDistanceEnd = noFogDist;
            data.skyEnd = noFogDist;
            data.cloudEnd = noFogDist;
        }
    }
}
