package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class CustomHotbarModule extends Module {

    public final BooleanSetting glassBackground = add(new BooleanSetting("Стеклянный фон", true));
    public final BooleanSetting smoothSelector = add(new BooleanSetting("Плавный выбор слота", true));
    public final BooleanSetting glowBorder = add(new BooleanSetting("Свечение активного слота", true));
    public final ModeSetting theme = add(new ModeSetting("Тема", "Акриловое стекло", "Неон", "Минимализм", "Радуга"));
    public final ColorSetting accentColor = add(new ColorSetting("Цвет рамки", 0xFF6C5CE7));

    private float animatedSlotX = 0.0f;

    public CustomHotbarModule() {
        super("Custom Hotbar", "Современная стеклянная панель быстрой выдачи (хотбар)", Category.INTERFACE);
    }

    public void renderBackground(DrawContext context, int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        int hotbarW = 182;
        int hotbarH = 22;
        int x = (screenWidth - hotbarW) / 2;
        int y = screenHeight - hotbarH;

        if (glassBackground.get()) {
            HudDraw.glass(context, x - 2, y - 1, hotbarW + 4, hotbarH + 2, 4f);
        }
    }

    public void renderOverlay(DrawContext context, int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        int hotbarW = 182;
        int hotbarH = 22;
        int x = (screenWidth - hotbarW) / 2;
        int y = screenHeight - hotbarH;

        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        float targetSlotX = x + selectedSlot * 20.0f;

        if (animatedSlotX < 1.0f || Math.abs(animatedSlotX - targetSlotX) > 300.0f) {
            animatedSlotX = targetSlotX;
        } else if (smoothSelector.get()) {
            animatedSlotX += (targetSlotX - animatedSlotX) * 0.25f;
        } else {
            animatedSlotX = targetSlotX;
        }

        int col = resolveAccentColor();
        if (glowBorder.get()) {
            // Мягкая подсветка активного слота поверх слотов
            Render2D.rect(context, animatedSlotX, y, 22f, 22f, 3f, 0x22000000 | (col & 0x00FFFFFF));
            Render2D.rect(context, animatedSlotX, y, 22f, 2f, 0, col);
            Render2D.rect(context, animatedSlotX, y + 20f, 22f, 2f, 0, col);
            Render2D.rect(context, animatedSlotX, y, 2f, 22f, 0, col);
            Render2D.rect(context, animatedSlotX + 20f, y, 2f, 22f, 0, col);
        }
    }

    @Deprecated
    public void render(DrawContext context, int screenWidth, int screenHeight) {
        renderOverlay(context, screenWidth, screenHeight);
    }

    private int resolveAccentColor() {
        if ("Неон".equals(theme.get())) return 0xFF00CEC9;
        if ("Радуга".equals(theme.get())) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
            return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
        }
        return accentColor.getEffectiveColor();
    }
}
