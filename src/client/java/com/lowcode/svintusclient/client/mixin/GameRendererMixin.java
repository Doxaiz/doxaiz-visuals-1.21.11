package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.combat.TotemPopModule;
import com.lowcode.svintusclient.client.module.impl.render.AspectRatioModule;
import com.lowcode.svintusclient.client.module.impl.render.NoHurtCamModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/gui/render/state/GuiRenderState;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/command/RenderDispatcher;Ljava/util/List;)V"), index = 4)
    private static List<net.minecraft.client.gui.render.SpecialGuiElementRenderer<?>> svintus$addCosmeticPreviewRenderer(List<net.minecraft.client.gui.render.SpecialGuiElementRenderer<?>> renderers) {
        List<net.minecraft.client.gui.render.SpecialGuiElementRenderer<?>> list = new ArrayList<>(renderers);
        list.add(new com.lowcode.lowcosmetic.client.render.CosmeticPreviewGuiElementRenderer(net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers()));
        return list;
    }
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void svintus$onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoHurtCamModule module = ModuleManager.get(NoHurtCamModule.class);
        if (module != null && module.isEnabled()) {
            float intensity = module.intensity.get();
            if (intensity <= 0.01f) {
                float shake = com.lowcode.svintusclient.client.module.impl.combat.KillEffectsModule.getKillShakeDegrees();
                if (shake != 0.0f) {
                    matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(shake));
                }
                ci.cancel();
            } else if (intensity < 100.0f && matrices != null) {
                float scale = intensity / 100.0f;
                matrices.scale(1.0f, scale, 1.0f);
            }
        }
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$onShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        if (floatingItem != null && floatingItem.isOf(Items.TOTEM_OF_UNDYING)) {
            com.lowcode.svintusclient.client.module.impl.render.TotemGhostModule ghost = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.TotemGhostModule.class);
            if (ghost != null && ghost.isEnabled()) {
                ci.cancel();
                return;
            }
            TotemPopModule module = ModuleManager.get(TotemPopModule.class);
            if (module != null && module.isEnabled() && module.cleanScreen.get()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void svintus$onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        com.lowcode.svintusclient.client.module.impl.render.ZoomModule zoomMod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ZoomModule.class);
        if (zoomMod != null && zoomMod.isEnabled()) {
            float z = zoomMod.getEffectiveFovMultiplier();
            if (z < 0.999f) {
                cir.setReturnValue(cir.getReturnValue() * z);
            }
        }
    }

    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"), cancellable = true, require = 0)
    private void svintus$onGetBasicProjectionMatrix(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatioModule ar = ModuleManager.get(AspectRatioModule.class);
        if (ar != null && ar.isEnabled() && cir.getReturnValue() != null) {
            Matrix4f mat = new Matrix4f(cir.getReturnValue());
            ar.apply(mat);
            cir.setReturnValue(mat);
        }
    }
}
