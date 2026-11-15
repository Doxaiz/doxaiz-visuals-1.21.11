package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.mixin.MinecraftClientAccessor;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public final class FastExpModule extends Module implements TickListener {

    public final NumberSetting delay = add(new NumberSetting("Задержка (тики)", 0.0f, 0.0f, 4.0f, 1.0f));
    public final BooleanSetting autoThrow = add(new BooleanSetting("Авто-сброс при ПКМ", true));
    public final BooleanSetting soundAlert = add(new BooleanSetting("Звук опыта", false));

    public FastExpModule() {
        super("FastExp", "Убирает задержку при броске пузырьков опыта (быстрый фарм)", Category.UTILITIES);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean mainExp = mc.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE);
        boolean offExp = mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE);

        if (mainExp || offExp) {
            if (mc instanceof MinecraftClientAccessor accessor) {
                accessor.svintus$setItemUseCooldown(delay.get().intValue());
                if (autoThrow.get() && mc.options.useKey.isPressed()) {
                    if (mc.interactionManager != null) {
                        net.minecraft.util.Hand hand = mainExp ? net.minecraft.util.Hand.MAIN_HAND : net.minecraft.util.Hand.OFF_HAND;
                        mc.interactionManager.interactItem(mc.player, hand);
                        if (soundAlert.get() && mc.player.age % 2 == 0) {
                            com.lowcode.svintusclient.client.util.SoundPreview.playPreview("8-бит", 0.5f);
                        }
                    }
                }
            }
        }
    }
}
