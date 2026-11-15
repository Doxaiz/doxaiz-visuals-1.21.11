package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.ItemPhysicsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

@Environment(EnvType.CLIENT)
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Shadow @Final private Random random;

    @Unique
    private static final WeakHashMap<ItemEntityRenderState, Boolean> svintus$groundStateMap = new WeakHashMap<>();

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("HEAD"), require = 0)
    private void svintus$captureGroundState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity != null && state != null) {
            svintus$groundStateMap.put(state, entity.isOnGround());
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$onRenderItem(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera, CallbackInfo ci) {
        ItemPhysicsModule mod = ModuleManager.get(ItemPhysicsModule.class);
        if (mod != null && mod.isEnabled()) {
            if (state.itemRenderState.isEmpty()) return;
            ci.cancel();

            matrices.push();
            Box box = state.itemRenderState.getModelBoundingBox();
            float f6 = -((float) box.minY) + 0.0625f;
            matrices.translate(0.0f, f6, 0.0f);

            boolean onGround = svintus$groundStateMap.getOrDefault(state, false);
            mod.applyTransform(matrices, state, onGround);

            ItemEntityRenderer.render(matrices, queue, state.light, state, this.random, box);
            matrices.pop();
        }
    }
}
