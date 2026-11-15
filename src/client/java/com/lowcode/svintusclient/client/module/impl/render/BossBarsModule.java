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
import net.minecraft.client.gui.hud.ClientBossBar;

public final class BossBarsModule extends Module {

    public final BooleanSetting customStyle = add(new BooleanSetting("Кастомный стиль", true));
    public final BooleanSetting showPercent = add(new BooleanSetting("Процент здоровья", true));
    public final BooleanSetting hideBars = add(new BooleanSetting("Скрыть боссбары", false));
    public final ModeSetting theme = add(new ModeSetting("Тема", "Акриловый неон", "Градиент", "Минимализм"));
    public final ColorSetting customColor = add(new ColorSetting("Цвет полосы", 0xFF6C5CE7));

    public BossBarsModule() {
        super("Boss Bars", "Современный стиль и оформление полосок здоровья боссов", Category.INTERFACE);
    }

    public boolean renderCustomBar(DrawContext context, int x, int y, ClientBossBar bar) {
        if (!isEnabled()) return false;
        if (hideBars.get()) return true; // Отменяем рендер если скрыто
        if (!customStyle.get()) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        int width = 182;
        int height = 18;

        // Акриловая стеклянная подложка
        HudDraw.glass(context, x - 2, y - 2, width + 4, height + 4, 4f);

        // Полоса здоровья
        float percent = bar.getPercent();
        int barW = (int) (width * percent);
        int col = resolveBarColor(bar);

        Render2D.rect(context, x, y + 10, width, 5, 2, 0x55000000);
        Render2D.rect(context, x, y + 10, barW, 5, 2, col);

        // Имя босса и процент
        String title = bar.getName().getString();
        if (showPercent.get()) {
            title += String.format(" §7[%d%%]", (int) (percent * 100));
        }
        int textW = mc.textRenderer.getWidth(title);
        context.drawTextWithShadow(mc.textRenderer, title, x + (width - textW) / 2, y, 0xFFFFFFFF);

        return true;
    }

    private int resolveBarColor(ClientBossBar bar) {
        if ("Градиент".equals(theme.get())) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
            return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
        } else if ("Минимализм".equals(theme.get())) {
            return customColor.getEffectiveColor();
        }

        return switch (bar.getColor()) {
            case PINK -> 0xFFFF7597;
            case BLUE -> 0xFF54A0FF;
            case RED -> 0xFFFF4757;
            case GREEN -> 0xFF2ED573;
            case YELLOW -> 0xFFFECA57;
            case PURPLE -> 0xFF9B59B6;
            case WHITE -> 0xFFF1F2F6;
        };
    }
}
