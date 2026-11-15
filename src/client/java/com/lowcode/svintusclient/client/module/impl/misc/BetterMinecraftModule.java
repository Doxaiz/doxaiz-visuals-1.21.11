package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

public final class BetterMinecraftModule extends Module {

    public final BooleanSetting hotbarAnimation = add(new BooleanSetting("Анимация хотбара", true));
    public final BooleanSetting chatAnimations = add(new BooleanSetting("Анимации чата", true));
    public final BooleanSetting inventoryAnimation = add(new BooleanSetting("Анимация инвентаря", true));
    public final BooleanSetting tabAnimation = add(new BooleanSetting("Анимация таба", true));
    public final BooleanSetting hotbarChatLift = add(new BooleanSetting("Подъем хотбара в чате", true));
    public final BooleanSetting saturationDisplay = add(new BooleanSetting("Индикатор насыщения", true));

    private static float currentSlotX = 0.0f;
    private static float targetSlotX = 0.0f;

    public BetterMinecraftModule() {
        super("Плавные анимации", "Плавные анимации открытия окон, инвентаря, чата и интерфейса Minecraft", Category.UTILITIES);
    }

    public static boolean isHotbarAnim() {
        BetterMinecraftModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(BetterMinecraftModule.class);
        return mod != null && mod.isEnabled() && mod.hotbarAnimation.get();
    }

    public static boolean isChatAnim() {
        BetterMinecraftModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(BetterMinecraftModule.class);
        return mod != null && mod.isEnabled() && mod.chatAnimations.get();
    }

    public static boolean isInventoryAnim() {
        BetterMinecraftModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(BetterMinecraftModule.class);
        return mod != null && mod.isEnabled() && mod.inventoryAnimation.get();
    }

    public static boolean isChatLift() {
        BetterMinecraftModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(BetterMinecraftModule.class);
        return mod != null && mod.isEnabled() && mod.hotbarChatLift.get();
    }

    public static boolean isTabAnim() {
        BetterMinecraftModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(BetterMinecraftModule.class);
        return mod != null && mod.isEnabled() && mod.tabAnimation.get();
    }

    public static boolean isSaturation() {
        BetterMinecraftModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(BetterMinecraftModule.class);
        return mod != null && mod.isEnabled() && mod.saturationDisplay.get();
    }

    public static float getSmoothSlotX(float target, float delta) {
        if (!isHotbarAnim()) return target;
        currentSlotX += (target - currentSlotX) * Math.min(1.0f, delta * 0.45f);
        return currentSlotX;
    }
}
