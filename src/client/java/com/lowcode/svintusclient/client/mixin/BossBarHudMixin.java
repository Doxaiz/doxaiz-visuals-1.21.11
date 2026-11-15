package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.BossBarsModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
@Mixin(BossBarHud.class)
public abstract class BossBarHudMixin {

    @Shadow @Final
    Map<UUID, ClientBossBar> bossBars;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void svintus$renderCustomBossBars(DrawContext context, CallbackInfo ci) {
        BossBarsModule module = ModuleManager.get(BossBarsModule.class);
        if (module == null || !module.isEnabled()) return;

        if (module.hideBars.get()) {
            ci.cancel();
            return;
        }

        if (module.customStyle.get()) {
            if (this.bossBars == null || this.bossBars.isEmpty()) return;
            ci.cancel();

            int width = context.getScaledWindowWidth();
            int currentY = 12;

            for (ClientBossBar bar : this.bossBars.values()) {
                int x = width / 2 - 91;
                module.renderCustomBar(context, x, currentY, bar);
                currentY += 24;
            }
        }
    }
}
