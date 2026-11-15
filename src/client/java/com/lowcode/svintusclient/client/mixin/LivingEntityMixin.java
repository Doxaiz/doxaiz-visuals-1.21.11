package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.combat.DamageIndicatorModule;
import com.lowcode.svintusclient.client.module.impl.combat.HitColorModule;
import com.lowcode.svintusclient.client.module.impl.combat.HitParticlesModule;
import com.lowcode.svintusclient.client.module.impl.combat.KillEffectsModule;
import com.lowcode.svintusclient.client.module.impl.combat.TotemPopModule;
import com.lowcode.svintusclient.client.module.impl.render.TotemGhostModule;
import com.lowcode.svintusclient.client.module.impl.render.JumpCirclesModule;
import com.lowcode.svintusclient.client.module.impl.sounds.HitSoundsModule;
import com.lowcode.svintusclient.client.module.impl.sounds.KillSoundsModule;
import com.lowcode.svintusclient.client.module.impl.sounds.TotemPopSoundModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected float lastDamageTaken;

    @Inject(method = "jump", at = @At("TAIL"))
    private void svintus$onJump(CallbackInfo ci) {
        if ((Object) this instanceof ClientPlayerEntity player) {
            JumpCirclesModule jumpCircles = ModuleManager.get(JumpCirclesModule.class);
            if (jumpCircles != null && jumpCircles.isEnabled()) {
                jumpCircles.onJump(player);
            }
        }
    }

    @Inject(method = "handleStatus", at = @At("HEAD"))
    private void svintus$onHandleStatus(byte status, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        MinecraftClient mc = MinecraftClient.getInstance();

        if (status == 3) {
            if (entity == mc.player) {
                com.lowcode.svintusclient.client.module.impl.render.WaypointsModule.setDeathPos(entity.getBlockPos());
                com.lowcode.svintusclient.client.module.impl.misc.DeathCoordsModule deathCoords = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.DeathCoordsModule.class);
                if (deathCoords != null) deathCoords.recordDeath(entity.getBlockPos());
            }
            if (mc.player != null && entity != mc.player && (entity == mc.player.getAttacking() || entity.squaredDistanceTo(mc.player) <= 64)) {
                KillEffectsModule killEffects = ModuleManager.get(KillEffectsModule.class);
                if (killEffects != null && killEffects.isEnabled()) {
                    killEffects.onEntityKill(entity);
                }
                KillSoundsModule killSounds = ModuleManager.get(KillSoundsModule.class);
                if (killSounds != null && killSounds.isEnabled()) {
                    killSounds.playKillSound(entity);
                }
            }
        } else if (status == 35) {
            TotemGhostModule totemGhost = ModuleManager.get(TotemGhostModule.class);
            if (totemGhost != null && totemGhost.isEnabled()) {
                TotemGhostModule.spawnGhost(entity.getEntityPos());
            }
            TotemPopModule totemPop = ModuleManager.get(TotemPopModule.class);
            if (totemPop != null && totemPop.isEnabled()) {
                totemPop.onTotemPop(entity);
            }
            TotemPopSoundModule totemSound = ModuleManager.get(TotemPopSoundModule.class);
            if (totemSound != null && totemSound.isEnabled()) {
                totemSound.playPopSound();
            }
            com.lowcode.svintusclient.client.module.impl.render.CooldownTagsModule cooldownTags = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CooldownTagsModule.class);
            if (cooldownTags != null && cooldownTags.isEnabled()) {
                cooldownTags.onTotemPop(entity);
            }
        } else if (status == 30) {
            com.lowcode.svintusclient.client.module.impl.render.CooldownTagsModule cooldownTags = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CooldownTagsModule.class);
            if (cooldownTags != null && cooldownTags.isEnabled()) {
                cooldownTags.onShieldDisabled(entity);
            }
        } else if (status == 2 || (status >= 46 && status <= 52)) {
            if (mc.player != null && entity != mc.player && entity.squaredDistanceTo(mc.player) <= 64) {
                HitSoundsModule hitSounds = ModuleManager.get(HitSoundsModule.class);
                if (hitSounds != null && hitSounds.isEnabled()) {
                    boolean isCrit = mc.player.getVelocity().y < -0.05 && !mc.player.isOnGround();
                    hitSounds.playHitSound(isCrit);
                }
            }
            HitParticlesModule hitParticles = ModuleManager.get(HitParticlesModule.class);
            if (hitParticles != null && hitParticles.isEnabled()) {
                hitParticles.onEntityHit(entity);
            }
            HitColorModule hitColor = ModuleManager.get(HitColorModule.class);
            if (hitColor != null && hitColor.isEnabled()) {
                hitColor.onEntityHit(entity);
            }
            DamageIndicatorModule damageIndicator = ModuleManager.get(DamageIndicatorModule.class);
            if (damageIndicator != null && damageIndicator.isEnabled() && mc.player != null) {
                if (entity != mc.player && entity.squaredDistanceTo(mc.player) <= 256) {
                    float dmg = this.lastDamageTaken > 0.0f ? this.lastDamageTaken : 1.0f;
                    boolean crit = mc.player.getVelocity().y < -0.05 && !mc.player.isOnGround();
                    Vec3d pos = entity.getEntityPos().add(
                            (Math.random() - 0.5) * 0.4,
                            entity.getHeight() * 0.65 + Math.random() * 0.25,
                            (Math.random() - 0.5) * 0.4
                    );
                    damageIndicator.addDamage(pos, dmg, crit);
                }
            }
        }
    }
}
