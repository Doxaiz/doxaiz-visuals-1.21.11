package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class ItemHighlightModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль подсветки", "Фон слота (Fill)", "Светящаяся рамка (Border)", "Неоновый контур (Glow)", "Точка-маркер (Dot)"));
    public final BooleanSetting totem = add(new BooleanSetting("Тотем бессмертия", true));
    public final BooleanSetting pearl = add(new BooleanSetting("Жемчуг Края", true));
    public final BooleanSetting gapple = add(new BooleanSetting("Золотые яблоки", true));
    public final BooleanSetting shield = add(new BooleanSetting("Щит", true));
    public final BooleanSetting chorus = add(new BooleanSetting("Хорус", true));
    public final BooleanSetting windCharge = add(new BooleanSetting("Ветряной заряд", true));
    public final BooleanSetting mace = add(new BooleanSetting("Булава (Mace)", true));
    public final BooleanSetting crossbow = add(new BooleanSetting("Арбалет", true));
    public final BooleanSetting holyworldItems = add(new BooleanSetting("Сферы и талисманы HolyWorld", true));
    public final BooleanSetting compassItem = add(new BooleanSetting("Компас анархии HolyWorld", true));
    public final NumberSetting opacity = add(new NumberSetting("Прозрачность подсветки", 0.45f, 0.1f, 1.0f, 0.05f));

    // Matching Items Highlight Integration
    public final BooleanSetting highlightMatching = add(new BooleanSetting("Одинаковые предметы при наведении", true));
    public final ModeSetting matchingStyle = add(new ModeSetting("Стиль подсветки копий",
            "Неоновая рамка", "Мягкая заливка", "Пульсирующий контур"));
    public final ColorSetting matchingColor = add(new ColorSetting("Цвет подсветки копий", 0xFF00CEC9));

    public ItemHighlightModule() {
        super("Item Highlight", "Цветная подсветка слотов для ключевых PvP предметов и одинаковых стаков при наведении", Category.HOLYWORLD);
    }

    public int getHighlightColor(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty()) return 0;

        int rgb = 0;
        if (totem.get() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
            rgb = 0xFFD700;
        } else if (pearl.get() && stack.isOf(Items.ENDER_PEARL)) {
            rgb = 0x00D2D3;
        } else if (gapple.get() && (stack.isOf(Items.GOLDEN_APPLE) || stack.isOf(Items.ENCHANTED_GOLDEN_APPLE))) {
            rgb = 0xFFA502;
        } else if (shield.get() && stack.isOf(Items.SHIELD)) {
            rgb = 0x3742FA;
        } else if (chorus.get() && stack.isOf(Items.CHORUS_FRUIT)) {
            rgb = 0xA55EEA;
        } else if (windCharge.get() && stack.isOf(Items.WIND_CHARGE)) {
            rgb = 0x70A1FF;
        } else if (mace.get() && stack.isOf(Items.MACE)) {
            rgb = 0xFF4757;
        } else if (crossbow.get() && stack.isOf(Items.CROSSBOW)) {
            rgb = 0x2ED573;
        } else if (compassItem.get() && stack.isOf(Items.COMPASS)) {
            rgb = 0x9B59B6;
        } else if (holyworldItems.get() && (stack.getName().getString().contains("Сфера") || stack.getName().getString().contains("Талисман"))) {
            rgb = 0xF39C12;
        }

        if (rgb == 0) return 0;
        int a = Math.round(opacity.get() * 255.0f);
        return (a << 24) | rgb;
    }

    public void renderMatchingHighlight(DrawContext context, ItemStack current, ItemStack hovered, int x, int y) {
        if (!isEnabled() || !highlightMatching.get()) return;
        if (current == null || hovered == null || current.isEmpty() || hovered.isEmpty()) return;
        if (!ItemStack.areItemsEqual(current, hovered)) return;

        int c = matchingColor.getEffectiveColor();
        String st = matchingStyle.get();
        if ("Мягкая заливка".equals(st)) {
            Render2D.rect(context, x, y, 16f, 16f, 2f, (c & 0x00FFFFFF) | 0x40000000);
        } else if ("Пульсирующий контур".equals(st)) {
            float pulse = (float) (0.6 + 0.4 * Math.sin(System.currentTimeMillis() * 0.008));
            int a = (int) (pulse * 255f);
            Render2D.outline(context, x - 1, y - 1, 18f, 18f, 2.5f, 1.2f, (c & 0x00FFFFFF) | (a << 24));
        } else {
            Render2D.outline(context, x - 1, y - 1, 18f, 18f, 2.5f, 1.2f, (c & 0x00FFFFFF) | 0xCC000000);
        }
    }
}
