package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.FreeLookModule;
import com.lowcode.svintusclient.client.module.impl.render.ZoomModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), require = 0)
    private void svintus$onMouseButton(long window, net.minecraft.client.input.MouseInput input, int action, CallbackInfo ci) {
        int button = input != null ? input.button() : -1;
        if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE && action == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.currentScreen == null) {
                com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule fhm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.class);
                if (fhm != null && fhm.isEnabled() && fhm.middleClickAdd.get() && mc.targetedEntity instanceof net.minecraft.entity.player.PlayerEntity targetPlayer && mc.player != null) {
                    String name = targetPlayer.getNameForScoreboard();
                    boolean added = com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.toggleFriend(name);
                    String msg = added ? "§a[Friends] §fИгрок §e" + name + " §fдобавлен в друзья!" : "§c[Friends] §fИгрок §e" + name + " §fудален из друзей!";
                    mc.player.sendMessage(net.minecraft.text.Text.literal(msg), true);
                    return;
                }
                com.lowcode.svintusclient.client.module.impl.misc.ClickPearlModule.onMiddleClick();
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), require = 0)
    private void svintus$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        ZoomModule zoom = ModuleManager.get(ZoomModule.class);
        if (zoom != null && zoom.isEnabled() && zoom.isZoomKeyHeld()) {
            zoom.onMouseScroll(vertical);
        }
    }

    @Redirect(
        method = "updateMouse",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"),
        require = 0
    )
    private void svintus$handleLookDirection(ClientPlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
        FreeLookModule freeLook = ModuleManager.get(FreeLookModule.class);
        if (freeLook != null && freeLook.isActive()) {
            freeLook.onMouseDelta(cursorDeltaX, cursorDeltaY);
            return;
        }

        ZoomModule zoom = ModuleManager.get(ZoomModule.class);
        if (zoom != null && zoom.isEnabled() && zoom.smoothMouse.get()) {
            float fovMul = zoom.getEffectiveFovMultiplier();
            if (fovMul < 0.999f) {
                cursorDeltaX *= fovMul;
                cursorDeltaY *= fovMul;
            }
        }
        player.changeLookDirection(cursorDeltaX, cursorDeltaY);
    }
}
