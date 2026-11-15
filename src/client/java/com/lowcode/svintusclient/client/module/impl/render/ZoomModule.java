package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public final class ZoomModule extends Module {

    public final ModeSetting mode = add(new ModeSetting("Режим зума", "Плавный (Smooth)", "Кинематографичный (Cinematic)", "Мгновенный (Instant)"));
    public final NumberSetting zoomFactor = add(new NumberSetting("Сила приближения", 3.0f, 1.5f, 6.0f, 0.5f));
    public final NumberSetting smoothSpeed = add(new NumberSetting("Скорость анимации", 0.25f, 0.05f, 0.6f, 0.05f));
    public final BooleanSetting smoothMouse = add(new BooleanSetting("Плавная мышь при зуме", true));
    public final BooleanSetting scrollZoom = add(new BooleanSetting("Зум колесиком мыши", true));
    public final NumberSetting cameraDistance = add(new NumberSetting("Дистанция камеры F5", 4.0f, 2.0f, 12.0f, 0.5f));
    public final BooleanSetting smoothF5 = add(new BooleanSetting("Плавное переключение F5", true));
    public final BooleanSetting cameraClip = add(new BooleanSetting("Камера сквозь блоки (NoClip)", false));

    private float currentFovMultiplier = 1.0f;
    private float scrollAdjustment = 0.0f;
    private float f5Progress = 1.0f;
    private net.minecraft.client.option.Perspective lastPerspective = net.minecraft.client.option.Perspective.FIRST_PERSON;

    public ZoomModule() {
        super("Zoom", "Плавное приближение камеры (клавиша C) и расширенные настройки камеры F5", Category.UTILITIES);
        setKey(GLFW.GLFW_KEY_C);
        enableByDefault();
    }

    public float getEffectiveF5Scale() {
        if (!isEnabled() || !smoothF5.get()) return 1.0f;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return 1.0f;

        net.minecraft.client.option.Perspective current = mc.options.getPerspective();
        if (current != lastPerspective) {
            f5Progress = 0.0f;
            lastPerspective = current;
        }

        if (f5Progress < 1.0f) {
            f5Progress += 0.15f;
            if (f5Progress > 1.0f) f5Progress = 1.0f;
        }

        return current.isFirstPerson() ? 1.0f : f5Progress;
    }

    public boolean isZoomKeyHeld() {
        if (!isEnabled()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null || mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        int boundKey = getKey() != GLFW.GLFW_KEY_UNKNOWN ? getKey() : GLFW.GLFW_KEY_C;
        return GLFW.glfwGetKey(handle, boundKey) == GLFW.GLFW_PRESS;
    }

    public float getEffectiveFovMultiplier() {
        if (!isEnabled()) return 1.0f;

        boolean active = isZoomKeyHeld();
        if (!active) {
            scrollAdjustment = 0.0f;
        }

        float baseFactor = zoomFactor.get() + scrollAdjustment;
        if (baseFactor < 1.2f) baseFactor = 1.2f;
        float targetMultiplier = active ? (1.0f / baseFactor) : 1.0f;

        String m = mode.get();
        if ("Мгновенный (Instant)".equals(m)) {
            currentFovMultiplier = targetMultiplier;
        } else if ("Кинематографичный (Cinematic)".equals(m)) {
            float speed = smoothSpeed.get() * 0.5f;
            currentFovMultiplier += (targetMultiplier - currentFovMultiplier) * speed;
        } else {
            // Плавный (Smooth)
            float speed = smoothSpeed.get();
            currentFovMultiplier += (targetMultiplier - currentFovMultiplier) * speed;
        }

        if (Math.abs(currentFovMultiplier - targetMultiplier) < 0.002f) {
            currentFovMultiplier = targetMultiplier;
        }

        return currentFovMultiplier;
    }

    public void onMouseScroll(double vertical) {
        if (isEnabled() && scrollZoom.get() && isZoomKeyHeld()) {
            scrollAdjustment += (float) vertical * 0.4f;
            if (scrollAdjustment < -1.0f) scrollAdjustment = -1.0f;
            if (scrollAdjustment > 5.0f) scrollAdjustment = 5.0f;
        }
    }
}
