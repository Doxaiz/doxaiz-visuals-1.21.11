package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public final class ItemUseProgressModule extends Module {

    public final NumberSetting ringRadius = add(new NumberSetting("Радиус кольца", 22.0f, 14.0f, 40.0f, 1.0f));
    public final BooleanSetting showIcon = add(new BooleanSetting("Иконка предмета", true));
    public final BooleanSetting showPercent = add(new BooleanSetting("Процент готовности", true));
    public final ColorSetting color = add(new ColorSetting("Цвет кольца", 0xFF00CEC9));

    public ItemUseProgressModule() {
        super("Item Use Progress", "Круговой индикатор использования предметов, еды и лука у прицела", Category.INTERFACE);
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !mc.player.isUsingItem() || mc.options.hudHidden) return;

        ItemStack stack = mc.player.getActiveItem();
        if (stack.isEmpty()) return;

        int useTicks = mc.player.getItemUseTime();
        float progress;
        if (stack.getItem() instanceof net.minecraft.item.BowItem) {
            progress = Math.min(1.0f, useTicks / 20.0f);
        } else if (stack.getItem() instanceof net.minecraft.item.CrossbowItem) {
            int pullTime = net.minecraft.item.CrossbowItem.getPullTime(stack, mc.player);
            progress = Math.min(1.0f, (float) useTicks / (float) Math.max(1, pullTime));
        } else if (stack.getItem() instanceof net.minecraft.item.TridentItem) {
            progress = Math.min(1.0f, useTicks / 10.0f);
        } else {
            int maxTicks = stack.getMaxUseTime(mc.player);
            int expected = (maxTicks > 100 || maxTicks <= 0) ? 32 : maxTicks;
            progress = Math.min(1.0f, (float) useTicks / (float) expected);
        }

        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        float rad = ringRadius.get();

        int col = color.getEffectiveColor();
        int bgCol = 0x40FFFFFF;

        // Отрисовка кругового прогресс-бара вокруг прицела
        int segments = 36;
        int filledSegments = (int) Math.ceil(segments * progress);
        double step = (Math.PI * 2.0) / segments;

        for (int i = 0; i < segments; i++) {
            double a = i * step - Math.PI / 2.0;
            float px = (float) (cx + Math.cos(a) * rad);
            float py = (float) (cy + Math.sin(a) * rad);

            boolean filled = (i < filledSegments);
            int segColor = filled ? col : bgCol;
            float dotSize = filled ? 3.0f : 1.8f;
            Render2D.circle(context, px, py, dotSize, segColor);
        }

        // Иконка используемого предмета рядом
        if (showIcon.get()) {
            context.drawItem(stack, cx + (int) rad + 6, cy - 8);
        }

        // Процент готовности
        if (showPercent.get()) {
            String pct = (int) (progress * 100) + "%";
            int pctCol = progress >= 1.0f ? 0xFF2ED573 : 0xFFFFFFFF;
            context.drawTextWithShadow(mc.textRenderer, pct, cx + (int) rad + 26, cy - 4, pctCol);
        }
    }
}
