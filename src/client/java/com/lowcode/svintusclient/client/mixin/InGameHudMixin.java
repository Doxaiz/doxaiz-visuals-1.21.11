package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.CrosshairModule;
import com.lowcode.svintusclient.client.module.impl.render.CustomScoreboardModule;
import com.lowcode.svintusclient.client.module.impl.render.WaypointsModule;
import com.lowcode.svintusclient.client.module.impl.render.ItemHighlightModule;
import com.lowcode.svintusclient.client.render.Render2D;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$renderCustomCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CrosshairModule crosshair = ModuleManager.get(CrosshairModule.class);
        boolean customCrosshair = crosshair != null && crosshair.isEnabled();
        if (customCrosshair) {
            crosshair.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }
        WaypointsModule waypoints = ModuleManager.get(WaypointsModule.class);
        if (waypoints != null && waypoints.isEnabled()) {
            waypoints.renderGps(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }
        com.lowcode.svintusclient.client.module.impl.combat.MaceVisualsModule mace = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.combat.MaceVisualsModule.class);
        if (mace != null && mace.isEnabled()) {
            mace.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }
        com.lowcode.svintusclient.client.module.impl.render.NoHurtCamModule noHurt = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.NoHurtCamModule.class);
        if (noHurt != null && noHurt.shouldFlash()) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null && mc.player.hurtTime > 0) {
                float factor = mc.player.hurtTime / 10.0f;
                int fc = noHurt.getFlashColor();
                int baseAlpha = (fc >>> 24) & 0xFF;
                int dynamicAlpha = Math.min(255, (int) (baseAlpha * factor * 1.5f));
                int renderCol = (dynamicAlpha << 24) | (fc & 0x00FFFFFF);
                context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), renderCol);
            }
        }

        com.lowcode.svintusclient.client.module.impl.render.TntTimerModule tntMod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.TntTimerModule.class);
        if (tntMod != null && tntMod.isEnabled()) {
            tntMod.renderScreenAlert(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }

        com.lowcode.svintusclient.client.module.impl.combat.KillEffectsModule killEffects = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.combat.KillEffectsModule.class);
        if (killEffects != null && killEffects.isEnabled()) {
            killEffects.renderOverlay(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }

        if (customCrosshair) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbarItem", at = @At("HEAD"), require = 0)
    private void svintus$renderHotbarHighlight(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        ItemHighlightModule module = ModuleManager.get(ItemHighlightModule.class);
        if (module != null && module.isEnabled()) {
            int color = module.getHighlightColor(stack);
            if (color != 0) {
                String st = module.style.get();
                if ("Светящаяся рамка (Border)".equals(st)) {
                    Render2D.outline(context, x, y, 16f, 16f, 3f, 1.2f, color);
                } else if ("Неоновый контур (Glow)".equals(st)) {
                    Render2D.outline(context, x - 1, y - 1, 18f, 18f, 4f, 1.5f, color);
                } else if ("Точка-маркер (Dot)".equals(st)) {
                    Render2D.rect(context, x + 6.5f, y + 13.5f, 3f, 3f, 1.5f, color);
                } else {
                    Render2D.rect(context, x, y, 16f, 16f, 3f, color);
                }
            }
        }
        com.lowcode.svintusclient.client.module.impl.render.ItemDurabilityOverlayModule ido = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ItemDurabilityOverlayModule.class);
        if (ido != null && ido.isEnabled()) {
            ido.renderSlotOverlay(context, stack, x, y, false);
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$onRenderScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        CustomScoreboardModule scoreboard = ModuleManager.get(CustomScoreboardModule.class);
        if (scoreboard != null && scoreboard.isEnabled()) {
            if (scoreboard.hide.get()) {
                ci.cancel();
                return;
            }
            float offsetX = scoreboard.customX.get();
            float offsetY = scoreboard.customY.get();
            float scale = scoreboard.scale.get();
            if (Math.abs(offsetX) > 0.01f || Math.abs(offsetY) > 0.01f || Math.abs(scale - 1.0f) > 0.01f) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(offsetX, offsetY);
                if (Math.abs(scale - 1.0f) > 0.01f) {
                    context.getMatrices().scale(scale, scale);
                }
            }
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("TAIL"), require = 0)
    private void svintus$afterRenderScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        CustomScoreboardModule scoreboard = ModuleManager.get(CustomScoreboardModule.class);
        if (scoreboard != null && scoreboard.isEnabled() && !scoreboard.hide.get()) {
            float offsetX = scoreboard.customX.get();
            float offsetY = scoreboard.customY.get();
            float scale = scoreboard.scale.get();
            if (Math.abs(offsetX) > 0.01f || Math.abs(offsetY) > 0.01f || Math.abs(scale - 1.0f) > 0.01f) {
                context.getMatrices().popMatrix();
            }
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "method_55439",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/ScoreboardEntry;formatted(Lnet/minecraft/scoreboard/number/NumberFormat;)Lnet/minecraft/text/MutableText;"),
        require = 0
    )
    private net.minecraft.text.MutableText svintus$hideScoreNumber(net.minecraft.scoreboard.ScoreboardEntry entry, net.minecraft.scoreboard.number.NumberFormat format) {
        CustomScoreboardModule scoreboard = ModuleManager.get(CustomScoreboardModule.class);
        if (scoreboard != null && scoreboard.isEnabled() && scoreboard.hideScores.get()) {
            return net.minecraft.text.Text.empty();
        }
        return entry.formatted(format);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "method_55439",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Team;decorateName(Lnet/minecraft/scoreboard/AbstractTeam;Lnet/minecraft/text/Text;)Lnet/minecraft/text/MutableText;"),
        require = 0
    )
    private net.minecraft.text.MutableText svintus$decorateScoreboardName(net.minecraft.scoreboard.AbstractTeam team, net.minecraft.text.Text name, net.minecraft.scoreboard.Scoreboard scoreboardObj, net.minecraft.scoreboard.number.NumberFormat numberFormat, net.minecraft.scoreboard.ScoreboardEntry scoreboardEntry) {
        net.minecraft.text.MutableText base = net.minecraft.scoreboard.Team.decorateName(team, name);
        CustomScoreboardModule scoreboard = ModuleManager.get(CustomScoreboardModule.class);
        if (scoreboard != null && scoreboard.isEnabled() && scoreboard.showHp.get()) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.world != null) {
                String owner = scoreboardEntry.owner();
                for (PlayerEntity p : mc.world.getPlayers()) {
                    if (p.getNameForScoreboard().equals(owner)) {
                        String hpStr = scoreboard.formatHealth(p);
                        if (!hpStr.isEmpty()) {
                            base.append(net.minecraft.text.Text.literal(hpStr));
                        }
                        break;
                    }
                }
            }
        }

        com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule np = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule.class);
        if (np != null && np.shouldProtectScoreboard()) {
            return net.minecraft.text.Text.literal(np.replaceName(base.getString()));
        }

        return base;
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$hideVanillaPotionOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule phm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule.class);
        if (phm != null && phm.isEnabled() && phm.hideVanilla.get()) {
            ci.cancel();
            return;
        }
        if (ModuleManager.HUD.potions.get() && com.lowcode.svintusclient.client.hud.HudManager.POTIONS.hideVanilla.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), require = 0)
    private void svintus$beforeRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        com.lowcode.svintusclient.client.module.impl.render.CustomHotbarModule chm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CustomHotbarModule.class);
        if (chm != null && chm.isEnabled()) {
            chm.renderBackground(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }

        if (com.lowcode.svintusclient.client.module.impl.misc.BetterMinecraftModule.isChatLift()) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(0.0f, -14.0f);
            }
        }
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"), require = 0)
    private void svintus$afterRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        com.lowcode.svintusclient.client.module.impl.render.CustomHotbarModule chm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CustomHotbarModule.class);
        if (chm != null && chm.isEnabled()) {
            chm.renderOverlay(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }

        if (com.lowcode.svintusclient.client.module.impl.misc.BetterMinecraftModule.isChatLift()) {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
                context.getMatrices().popMatrix();
            }
        }
    }

    @Inject(method = "renderFood", at = @At("TAIL"), require = 0)
    private void svintus$renderSaturation(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo ci) {
        com.lowcode.svintusclient.client.module.impl.misc.FoodInfoModule foodInfo = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.FoodInfoModule.class);
        boolean enabled = (foodInfo != null && foodInfo.isEnabled() && foodInfo.showSaturation.get())
                || com.lowcode.svintusclient.client.module.impl.misc.BetterMinecraftModule.isSaturation();
        if (!enabled) return;
        if (player == null || player.getHungerManager() == null) return;
        float saturation = player.getHungerManager().getSaturationLevel();
        if (saturation <= 0.0f) return;

        int satColor = foodInfo != null && foodInfo.isEnabled() ? foodInfo.getSaturationColor() : 0xFFFFD700;
        int fullDots = (int) (saturation / 2.0f);
        boolean hasHalf = (saturation % 2.0f) >= 0.5f;

        for (int i = 0; i < 10; i++) {
            if (i > fullDots) break;
            int x = right - i * 8 - 9;
            int y = top;
            if (i < fullDots) {
                Render2D.rect(context, x + 2.5f, y + 2.5f, 4f, 4f, 2f, satColor);
            } else if (hasHalf) {
                Render2D.rect(context, x + 2.5f, y + 2.5f, 2f, 4f, 1f, satColor);
            }
        }

        if (foodInfo != null && foodInfo.isNumeric()) {
            String satText = String.format(java.util.Locale.US, "%.1f", saturation);
            context.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer, satText, right - 10 * 8 - 18, top + 1, satColor, false);
        }
    }
}
