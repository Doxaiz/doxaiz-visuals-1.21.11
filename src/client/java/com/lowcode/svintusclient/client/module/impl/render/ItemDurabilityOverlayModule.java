package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;

public final class ItemDurabilityOverlayModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль прочности",
            "Проценты (%)", "Число (Остаток)", "Цветная полоска", "Индикатор точки"));
    public final ModeSetting colorGradient = add(new ModeSetting("Градиент цвета",
            "Зелёный -> Красный", "Акцент темы", "Неоновый циановый"));
    public final BooleanSetting lowDurabilityWarning = add(new BooleanSetting("Мигание при < 15%", true));
    public final BooleanSetting showInHotbar = add(new BooleanSetting("Показывать в хотбаре", true));
    public final BooleanSetting showInContainers = add(new BooleanSetting("Показывать в инвентаре", true));
    public final BooleanSetting showEnchantsCount = add(new BooleanSetting("Счётчик зачарований", true));
    public final ColorSetting customAccent = add(new ColorSetting("Кастомный акцент", 0xFF05D9E8));

    public ItemDurabilityOverlayModule() {
        super("Item Durability", "Точный оверлей прочности (%) и зачарований поверх ячеек предметов", Category.INTERFACE);
    }

    public void renderSlotOverlay(DrawContext ctx, ItemStack stack, int slotX, int slotY, boolean isContainer) {
        if (!isEnabled() || stack == null || stack.isEmpty()) return;
        if (isContainer && !showInContainers.get()) return;
        if (!isContainer && !showInHotbar.get()) return;

        // 1. Durability
        if (stack.isDamageable()) {
            int max = stack.getMaxDamage();
            int dmg = stack.getDamage();
            int remaining = Math.max(0, max - dmg);
            float ratio = max > 0 ? (float) remaining / (float) max : 1.0f;
            ratio = Math.max(0.0f, Math.min(1.0f, ratio));

            int col = resolveColor(ratio);
            if (lowDurabilityWarning.get() && ratio <= 0.15f) {
                if ((System.currentTimeMillis() / 250L) % 2L == 0L) {
                    col = 0xFFFF2A6D;
                }
            }

            String st = style.get();
            if ("Проценты (%)".equals(st)) {
                int pct = Math.round(ratio * 100.0f);
                String text = pct + "%";
                ctx.getMatrices().pushMatrix();
                float tx = slotX + 8.0f;
                float ty = slotY + 11.5f;
                Fonts.REGULAR.drawCentered(ctx, text, tx, ty, 4.4f, col);
                ctx.getMatrices().popMatrix();
            } else if ("Число (Остаток)".equals(st)) {
                String text = remaining >= 1000 ? String.format("%.1fk", remaining / 1000.0f) : String.valueOf(remaining);
                ctx.getMatrices().pushMatrix();
                float tx = slotX + 8.0f;
                float ty = slotY + 11.5f;
                Fonts.REGULAR.drawCentered(ctx, text, tx, ty, 4.2f, col);
                ctx.getMatrices().popMatrix();
            } else if ("Цветная полоска".equals(st)) {
                float barW = 14.0f * ratio;
                Render2D.rect(ctx, slotX + 1.0f, slotY + 14.0f, 14.0f, 1.5f, 0.5f, 0x80000000);
                Render2D.rect(ctx, slotX + 1.0f, slotY + 14.0f, barW, 1.5f, 0.5f, col);
            } else if ("Индикатор точки".equals(st)) {
                Render2D.circle(ctx, slotX + 14.0f, slotY + 2.5f, 1.8f, col);
            }
        }

        // 2. Enchantments count
        if (showEnchantsCount.get()) {
            ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants != null && !enchants.isEmpty()) {
                int count = enchants.getSize();
                if (count > 0) {
                    String encText = String.valueOf(count);
                    Render2D.rect(ctx, slotX + 1.0f, slotY + 1.0f, 5.0f, 5.0f, 1.0f, 0xCC101018);
                    Fonts.SEMIBOLD.drawCentered(ctx, encText, slotX + 3.5f, slotY + 3.5f, 3.8f, 0xFFFFD700);
                }
            }
        }
    }

    private int resolveColor(float ratio) {
        String mode = colorGradient.get();
        if ("Акцент темы".equals(mode)) {
            return Theme.accent();
        } else if ("Неоновый циановый".equals(mode)) {
            return customAccent.getEffectiveColor();
        }
        // Green -> Yellow -> Red
        int r, g, b = 40;
        if (ratio > 0.5f) {
            float t = (ratio - 0.5f) * 2.0f;
            r = (int) (255 * (1.0f - t));
            g = 230;
        } else {
            float t = ratio * 2.0f;
            r = 240;
            g = (int) (230 * t);
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
