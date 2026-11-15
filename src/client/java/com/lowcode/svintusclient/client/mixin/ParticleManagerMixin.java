package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.combat.HitParticlesModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$cancelVanillaHitParticles(ParticleEffect parameters, double x, double y, double z,
                                                   double velocityX, double velocityY, double velocityZ,
                                                   CallbackInfoReturnable<Particle> cir) {
        HitParticlesModule hpm = ModuleManager.get(HitParticlesModule.class);
        if (hpm != null && hpm.isEnabled() && hpm.cancelVanilla.get()) {
            var type = parameters.getType();
            if (type == ParticleTypes.CRIT 
                    || type == ParticleTypes.ENCHANTED_HIT 
                    || type == ParticleTypes.SWEEP_ATTACK 
                    || type == ParticleTypes.DAMAGE_INDICATOR) {
                cir.setReturnValue(null);
            }
        }
    }
}
