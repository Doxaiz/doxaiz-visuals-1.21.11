package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.CustomSwordsModule;
import com.lowcode.svintusclient.client.module.impl.render.ItemModelsModule;
import com.lowcode.svintusclient.client.module.impl.render.SwingAnimationModule;
import com.lowcode.svintusclient.client.module.impl.render.ViewModelModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(HeldItemRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(
        method = "renderFirstPersonItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER),
        require = 0
    )
    private void svintus$onRenderFirstPersonItem(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue commandQueue,
            int light,
            CallbackInfo ci
    ) {
        Arm arm = (hand == Hand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();

        ViewModelModule viewModel = ModuleManager.get(ViewModelModule.class);
        if (viewModel != null && viewModel.isEnabled()) {
            viewModel.transform(matrices, arm);
        }

        CustomSwordsModule customSwords = ModuleManager.get(CustomSwordsModule.class);
        if (customSwords != null && customSwords.isEnabled()) {
            customSwords.transformSword(matrices, arm, item);
        }

        ItemModelsModule itemModels = ModuleManager.get(ItemModelsModule.class);
        if (itemModels != null && itemModels.isEnabled() && item.isOf(Items.TOTEM_OF_UNDYING)) {
            float hs = itemModels.getHandScale();
            if (Math.abs(hs - 1.0f) > 0.01f) {
                matrices.scale(hs, hs, hs);
            }
        }
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$customSwing(float swingProgress, MatrixStack matrices, int armX, Arm arm, CallbackInfo ci) {
        SwingAnimationModule swing = ModuleManager.get(SwingAnimationModule.class);
        if (swing != null && swing.isEnabled()) {
            ci.cancel();
            if (!swing.ignoreOffhand.get() || arm == Arm.RIGHT) {
                swing.applySwing(matrices, swingProgress, arm, armX);
            }
        }
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$cancelVanillaSwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        SwingAnimationModule swing = ModuleManager.get(SwingAnimationModule.class);
        if (swing != null && swing.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$suppressEatTransform(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, net.minecraft.entity.player.PlayerEntity player, CallbackInfo ci) {
        ViewModelModule viewModel = ModuleManager.get(ViewModelModule.class);
        if (viewModel != null && viewModel.isEnabled() && viewModel.noEat.get()) {
            ci.cancel();
        }
    }
}
