package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.misc.TabPingModule;
import com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule;
import com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule;
import com.lowcode.svintusclient.client.module.impl.render.TabListModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void svintus$formatPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (entry == null || entry.getProfile() == null) return;

        Text original = cir.getReturnValue();
        String name = entry.getProfile().name();
        if (name == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        MutableText result = Text.empty();

        // 1. Бейдж друга
        if (FriendHighlightModule.isFriend(name)) {
            result.append(Text.literal("§e★ "));
        }

        // 2. Имя игрока (с учетом StreamerMode)
        Text nameText = original != null ? original : Text.literal(name);
        StreamerModeModule sm = ModuleManager.get(StreamerModeModule.class);
        if (sm != null && sm.isEnabled()) {
            nameText = Text.literal(sm.replaceName(nameText.getString()));
        }

        result.append(nameText);

        // 3. Пинг в Tab (TabPingModule или TabListModule)
        TabPingModule tpm = ModuleManager.get(TabPingModule.class);
        TabListModule tlm = ModuleManager.get(TabListModule.class);

        if (tpm != null && tpm.isEnabled()) {
            tpm.shouldUpdate();
        }
        if (tlm != null && tlm.isEnabled()) {
            boolean heads3D = tlm.show3DHeads.get();
            int limit = tlm.maxPlayers.get().intValue();
        }

        boolean showPing = (tpm != null && tpm.isEnabled() && tpm.showNumber.get()) ||
                           (tlm != null && tlm.isEnabled() && tlm.numericPing.get());

        if (showPing) {
            int latency = entry.getLatency();
            boolean colored = (tpm != null && tpm.isEnabled() && tpm.colorCode.get()) ||
                              (tlm != null && tlm.isEnabled() && tlm.coloredPing.get());

            Formatting color = Formatting.WHITE;
            if (colored) {
                if (latency < 80) color = Formatting.GREEN;
                else if (latency < 150) color = Formatting.YELLOW;
                else color = Formatting.RED;
            }
            final Formatting finalColor = color;

            String pStyle = tlm != null && tlm.isEnabled() ? tlm.pingStyle.get() : "Цветной текст";
            String pingStr = "Бейдж с заливкой".equals(pStyle) ? " [" + latency + "ms]"
                           : "Минимальный".equals(pStyle) ? " " + latency
                           : " " + latency + "ms";

            MutableText pingText = Text.literal(pingStr).styled(s -> s.withColor(finalColor));
            result.append(pingText);
        }

        // 4. Здоровье игроков в Tab (TabListModule.showHealth)
        if (tlm != null && tlm.isEnabled() && tlm.showHealth.get()) {
            if (mc.world != null) {
                net.minecraft.entity.player.PlayerEntity pe = mc.world.getPlayerByUuid(entry.getProfile().id());
                if (pe != null) {
                    float hp = pe.getHealth() + pe.getAbsorptionAmount();
                    Formatting hpColor = hp > 15 ? Formatting.GREEN : (hp > 8 ? Formatting.GOLD : Formatting.RED);
                    result.append(Text.literal(" " + String.format(java.util.Locale.US, "%.0f❤", hp)).styled(s -> s.withColor(hpColor)));
                }
            }
        }

        cir.setReturnValue(result);
    }

    @Inject(method = "collectPlayerEntries", at = @At("RETURN"), cancellable = true, require = 0)
    private void svintus$limitPlayers(CallbackInfoReturnable<java.util.List<PlayerListEntry>> cir) {
        TabListModule tlm = ModuleManager.get(TabListModule.class);
        if (tlm != null && tlm.isEnabled() && cir.getReturnValue() != null) {
            int max = tlm.maxPlayers.get().intValue();
            java.util.List<PlayerListEntry> list = cir.getReturnValue();
            if (list.size() > max) {
                cir.setReturnValue(list.subList(0, max));
            }
        }
    }
}
