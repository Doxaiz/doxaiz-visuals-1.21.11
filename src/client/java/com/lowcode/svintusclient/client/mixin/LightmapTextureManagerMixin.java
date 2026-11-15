package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.FullbrightModule;
import com.lowcode.svintusclient.client.module.impl.render.NoFogModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0), require = 0)
    private float svintus$darknessScale(Double value) {
        NoFogModule noFog = ModuleManager.get(NoFogModule.class);
        if (noFog != null && noFog.isEnabled() && noFog.darknessFog.get()) {
            return 0.0f;
        }
        return value.floatValue();
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;"), require = 0)
    private Object svintus$fullbrightGamma(net.minecraft.client.option.SimpleOption<?> option) {
        FullbrightModule fb = ModuleManager.get(FullbrightModule.class);
        Object val = option.getValue();
        if (val instanceof Double d && fb != null && fb.isEnabled() && "Гамма".equals(fb.mode.get())) {
            return Math.max(d, (double) fb.gammaLevel.get());
        }
        return val;
    }
}
