package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$preventFriendAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof net.minecraft.entity.LivingEntity living) {
            com.lowcode.svintusclient.client.module.impl.render.TargetMarkerModule tmm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.TargetMarkerModule.class);
            if (tmm != null && tmm.isEnabled()) {
                tmm.setTarget(living);
            }
            com.lowcode.svintusclient.client.module.impl.combat.HitParticlesModule hpm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.combat.HitParticlesModule.class);
            if (hpm != null && hpm.isEnabled()) {
                hpm.onEntityHit(living);
            }
            com.lowcode.svintusclient.client.module.impl.combat.HitColorModule hcm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.combat.HitColorModule.class);
            if (hcm != null && hcm.isEnabled()) {
                hcm.onEntityHit(living);
            }
            com.lowcode.svintusclient.client.module.impl.sounds.HitSoundsModule hsm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.sounds.HitSoundsModule.class);
            if (hsm != null && hsm.isEnabled()) {
                hsm.playHitSound();
            }
            com.lowcode.svintusclient.client.module.impl.combat.DamageIndicatorModule dim = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.combat.DamageIndicatorModule.class);
            if (dim != null && dim.isEnabled()) {
                boolean crit = player.getVelocity().y < -0.05 && !player.isOnGround();
                float estDmg = crit ? 6.5f : 4.0f;
                net.minecraft.item.ItemStack weapon = player.getMainHandStack();
                if (weapon != null && !weapon.isEmpty()) {
                    if (weapon.isIn(net.minecraft.registry.tag.ItemTags.SWORDS)) estDmg += 3.0f;
                    else if (weapon.isOf(net.minecraft.item.Items.MACE)) estDmg += 5.0f;
                    else if (weapon.isIn(net.minecraft.registry.tag.ItemTags.AXES)) estDmg += 4.0f;
                }
                net.minecraft.util.math.Vec3d pos = living.getEntityPos().add(
                        (Math.random() - 0.5) * 0.4,
                        living.getHeight() * 0.75 + Math.random() * 0.2,
                        (Math.random() - 0.5) * 0.4
                );
                dim.addDamage(pos, estDmg, crit);
            }
        }
    }

    @Inject(method = "breakBlock", at = @At("HEAD"), require = 0)
    private void svintus$onBreakBlock(net.minecraft.util.math.BlockPos pos, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.world != null) {
            net.minecraft.block.Block block = mc.world.getBlockState(pos).getBlock();
            com.lowcode.svintusclient.client.module.impl.render.MineViewerModule mvm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.MineViewerModule.class);
            if (mvm != null && mvm.isEnabled()) {
                mvm.onOreMined(block);
            }
            com.lowcode.svintusclient.client.module.impl.render.ItemPickupLogModule ipl = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ItemPickupLogModule.class);
            if (ipl != null && ipl.isEnabled() && block.asItem() != net.minecraft.item.Items.AIR) {
                ipl.onPickup(new net.minecraft.item.ItemStack(block.asItem()), 1);
            }
            com.lowcode.svintusclient.client.module.impl.sounds.ValuableBreakSoundsModule vbs = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.sounds.ValuableBreakSoundsModule.class);
            if (vbs != null && vbs.isEnabled()) {
                vbs.onBlockBreak(block);
            }
        }
    }
}
