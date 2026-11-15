package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class FullbrightModule extends Module implements TickListener {

    public final ModeSetting mode = add(new ModeSetting("Режим", "Гамма", "Ночное зрение"));
    public final NumberSetting gammaLevel = add(new NumberSetting("Уровень яркости", 12.0f, 1.0f, 25.0f, 0.5f));

    private static final int NIGHT_VISION_DURATION = 1728000;
    private boolean appliedByModule = false;

    public FullbrightModule() {
        super("Fullbright", "Максимальная яркость и видимость в темных пещерах и ночью", Category.UTILITIES);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (isEnabled()) {
            String m = mode.get();
            if ("Ночное зрение".equals(m)) {
                StatusEffectInstance current = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (current == null || current.getDuration() <= 200) {
                    mc.player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.NIGHT_VISION,
                            NIGHT_VISION_DURATION,
                            0,
                            false,
                            false,
                            false
                    ));
                    appliedByModule = true;
                }
            } else if (appliedByModule) {
                // If switched away from Night Vision to Gamma
                if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                }
                appliedByModule = false;
            }
        } else if (appliedByModule) {
            if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
            appliedByModule = false;
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (appliedByModule && mc.player != null && mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        appliedByModule = false;
    }
}
