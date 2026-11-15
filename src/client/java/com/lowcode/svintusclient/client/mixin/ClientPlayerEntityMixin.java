package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.LockSlotModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void svintus$preventLockedItemDrop(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
        LockSlotModule lockSlot = ModuleManager.get(LockSlotModule.class);
        if (lockSlot != null && lockSlot.isEnabled()) {
            ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
            if (player.getInventory() != null) {
                int selected = player.getInventory().getSelectedSlot();
                if (lockSlot.isSlotLocked(selected)) {
                    cir.setReturnValue(false);
                    player.sendMessage(Text.literal("§c🔒 Слот " + (selected + 1) + " защищен от случайного выброса (item.save)"), true);
                    if (lockSlot.soundAlert.get()) {
                        player.playSound(net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
                    }
                }
            }
        }
    }
}
