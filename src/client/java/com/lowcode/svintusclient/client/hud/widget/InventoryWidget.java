package com.lowcode.svintusclient.client.hud.widget;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.hud.HudWidget;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class InventoryWidget extends HudWidget {

    private static final float SLOT_SIZE = 17f;
    private static final float SLOT_GAP = 2f;
    private static final float PAD = 5f;
    private static final float HEADER_H = 12f;

    private final BooleanSetting showEmptySlots = option("Отображать сетку ячеек", true);

    public InventoryWidget() {
        super("inventory", "Инвентарь", 0.5f, 0.75f);
    }

    @Override
    public boolean isVisible() {
        com.lowcode.svintusclient.client.module.impl.render.InventoryHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.InventoryHudModule.class);
        if (mod != null) return mod.isEnabled();
        return ModuleManager.HUD != null && ModuleManager.HUD.inventory.get();
    }

    @Override
    public void setVisible(boolean visible) {
        com.lowcode.svintusclient.client.module.impl.render.InventoryHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.InventoryHudModule.class);
        if (mod != null) mod.setEnabled(visible);
        if (ModuleManager.HUD != null) ModuleManager.HUD.inventory.set(visible);
    }

    @Override
    protected void layout(boolean editing) {
        w = PAD * 2f + 9 * SLOT_SIZE + 8 * SLOT_GAP;
        h = PAD * 2f + HEADER_H + 3 * SLOT_SIZE + 2 * SLOT_GAP;
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        com.lowcode.svintusclient.client.module.impl.render.InventoryHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.InventoryHudModule.class);
        float alpha = mod != null ? mod.opacity.get() : 1.0f;
        float prev = Render2D.alpha();
        Render2D.setAlpha(prev * alpha);

        String st = mod != null ? mod.style.get() : "Стекло (Matrix)";

        if ("Неон".equals(st)) {
            HudDraw.glass(ctx, x, y, w, h, 6f);
            Render2D.outline(ctx, x, y, w, h, 6f, 1.2f, 0xFFFFA502);
        } else if ("Минимализм".equals(st)) {
            Render2D.rect(ctx, x, y, w, h, 4f, 0xCC111115);
        } else {
            HudDraw.glass(ctx, x, y, w, h, 6f);
        }

        // Header
        Render2D.rect(ctx, x + PAD, y + 4f, 3f, 7f, 1.5f, 0xFFFFA502);
        Fonts.SEMIBOLD.draw(ctx, "Инвентарь", x + PAD + 6f, y + 3f, 6.2f, Theme.TEXT);

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        PlayerInventory inv = player != null ? player.getInventory() : null;

        float startY = y + PAD + HEADER_H;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                float slotX = x + PAD + col * (SLOT_SIZE + SLOT_GAP);
                float slotY = startY + row * (SLOT_SIZE + SLOT_GAP);

                boolean empty = mod != null ? mod.showEmptySlots.get() : showEmptySlots.get();

                // Slot background
                if (empty) {
                    Render2D.rect(ctx, slotX, slotY, SLOT_SIZE, SLOT_SIZE, 3f, 0x1AFFFFFF);
                }

                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = ItemStack.EMPTY;

                if (inv != null && slotIndex < inv.size()) {
                    stack = inv.getStack(slotIndex);
                } else if (editing) {
                    if (row == 0 && col == 0) stack = Items.GOLDEN_APPLE.getDefaultStack();
                    else if (row == 0 && col == 1) stack = Items.ENDER_PEARL.getDefaultStack();
                    else if (row == 0 && col == 2) stack = Items.TOTEM_OF_UNDYING.getDefaultStack();
                }

                if (!stack.isEmpty()) {
                    int ix = Math.round(slotX + 0.5f);
                    int iy = Math.round(slotY + 0.5f);
                    ctx.drawItem(stack, ix, iy);
                    if (mod != null && !mod.showItemCounts.get()) {
                        ctx.drawStackOverlay(mc.textRenderer, stack, ix, iy, "");
                    } else {
                        ctx.drawStackOverlay(mc.textRenderer, stack, ix, iy);
                    }
                }
            }
        }
        Render2D.setAlpha(prev);
    }
}
