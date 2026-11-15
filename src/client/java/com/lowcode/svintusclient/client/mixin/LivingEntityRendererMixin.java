package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.combat.HitColorModule;
import com.lowcode.svintusclient.client.module.impl.render.NameTagsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"), require = 0)
    private void svintus$onUpdateRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        NameTagsModule nameTags = ModuleManager.get(NameTagsModule.class);
        if (nameTags != null && nameTags.isEnabled()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (entity != mc.player) {
                state.displayName = null;
            } else if (nameTags.selfNametag.get() && !mc.options.getPerspective().isFirstPerson()) {
                state.displayName = entity.getDisplayName();
                if (entity.getAttachments() != null) {
                    state.nameLabelPos = entity.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, entity.getYaw());
                }
            }
        }

        HitColorModule hitColor = ModuleManager.get(HitColorModule.class);
        if (hitColor != null && hitColor.isEnabled()) {
            hitColor.captureTint(state, entity);
            if (hitColor.shouldTint(entity)) {
                state.hurt = false;
            }
        }
    }

    @Inject(method = "getMixColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void svintus$getCustomMixColor(S state, CallbackInfoReturnable<Integer> cir) {
        HitColorModule hitColor = ModuleManager.get(HitColorModule.class);
        if (hitColor != null && hitColor.isEnabled()) {
            Integer customTint = hitColor.tintFor(state);
            if (customTint != null) {
                cir.setReturnValue(customTint);
            }
        }
    }
}
