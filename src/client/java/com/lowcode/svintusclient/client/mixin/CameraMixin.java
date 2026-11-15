package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.FreeLookModule;
import com.lowcode.svintusclient.client.module.impl.render.ZoomModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class CameraMixin {

    @ModifyArg(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;clipToSpace(F)F"),
        index = 0
    )
    private float svintus$modifyCameraDistance(float requestedDistance) {
        ZoomModule mod = ModuleManager.get(ZoomModule.class);
        if (mod != null && mod.isEnabled()) {
            return requestedDistance * (mod.cameraDistance.get() / 4.0f) * mod.getEffectiveF5Scale();
        }
        return requestedDistance;
    }

    @org.spongepowered.asm.mixin.injection.Inject(
        method = "clipToSpace",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void svintus$cameraNoClip(float desiredCameraDistance, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        ZoomModule mod = ModuleManager.get(ZoomModule.class);
        if (mod != null && mod.isEnabled() && mod.cameraClip.get()) {
            cir.setReturnValue(desiredCameraDistance);
        }
    }

    @ModifyVariable(
        method = "setRotation",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private float svintus$modifyYaw(float yaw) {
        FreeLookModule freeLook = ModuleManager.get(FreeLookModule.class);
        if (freeLook != null && freeLook.isActive()) {
            return freeLook.getFreeYaw();
        }
        return yaw;
    }

    @ModifyVariable(
        method = "setRotation",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true
    )
    private float svintus$modifyPitch(float pitch) {
        com.lowcode.svintusclient.client.module.impl.cosmetics.AnimeEmotesModule emotes = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.cosmetics.AnimeEmotesModule.class);
        if (emotes != null && emotes.isEnabled()) {
            pitch += emotes.getPitchOffset();
            String activeEmote = emotes.getActiveEmote();
        }
        FreeLookModule freeLook = ModuleManager.get(FreeLookModule.class);
        if (freeLook != null && freeLook.isActive()) {
            return freeLook.getFreePitch();
        }
        return pitch;
    }
}
