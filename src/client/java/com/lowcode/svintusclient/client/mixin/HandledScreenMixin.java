package com.lowcode.svintusclient.client.mixin;

import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.ShulkerPreviewModule;
import com.lowcode.svintusclient.client.render.Render2D;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected Slot focusedSlot;

    private static long svintus$screenOpenTime = 0L;
    private static boolean svintus$matrixPushed = false;

    @Inject(method = "init", at = @At("TAIL"))
    private void svintus$onScreenInit(CallbackInfo ci) {
        svintus$screenOpenTime = System.currentTimeMillis();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void svintus$animateScreenHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (com.lowcode.svintusclient.client.module.impl.misc.BetterMinecraftModule.isInventoryAnim()) {
            long elapsed = System.currentTimeMillis() - svintus$screenOpenTime;
            if (elapsed < 200L) {
                float progress = elapsed / 200.0f;
                float ease = (float) Math.sin(progress * Math.PI / 2.0);
                float scale = 0.88f + 0.12f * ease;
                context.getMatrices().pushMatrix();
                float cx = context.getScaledWindowWidth() / 2.0f;
                float cy = context.getScaledWindowHeight() / 2.0f;
                context.getMatrices().translate(cx * (1.0f - scale), cy * (1.0f - scale));
                context.getMatrices().scale(scale, scale);
                svintus$matrixPushed = true;
            } else {
                svintus$matrixPushed = false;
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void svintus$animateScreenTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (svintus$matrixPushed) {
            context.getMatrices().popMatrix();
            svintus$matrixPushed = false;
        }
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("TAIL"))
    private void svintus$drawShulkerPreview(DrawContext context, int x, int y, CallbackInfo ci) {
        ShulkerPreviewModule module = ModuleManager.get(ShulkerPreviewModule.class);
        if (module == null || !module.isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean shift = mc.getWindow() != null && (
                org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                || org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS
        );
        if (!module.shouldDisplay(shift)) return;
        if (focusedSlot == null || !focusedSlot.hasStack()) return;

        ItemStack stack = focusedSlot.getStack();
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) return;

        DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        container.copyTo(items);

        int slotSize = 17;
        int gap = 2;
        int pad = 5;
        boolean hasTitle = module.showTitle.get();
        int titleH = hasTitle ? 12 : 0;
        int boxW = pad * 2 + 9 * slotSize + 8 * gap;
        int boxH = pad * 2 + 3 * slotSize + 2 * gap + titleH;

        int boxX = Math.max(4, Math.min(context.getScaledWindowWidth() - boxW - 4, x + 12));
        int boxY = Math.max(4, Math.min(context.getScaledWindowHeight() - boxH - 4, y - boxH - 8));

        context.getMatrices().pushMatrix();
        float previewScale = module.getScale();
        if (previewScale != 1.0f) {
            context.getMatrices().translate(boxX, boxY);
            context.getMatrices().scale(previewScale, previewScale);
            context.getMatrices().translate(-boxX, -boxY);
        }

        String st = module.style.get();
        if ("Тёмный (Dark)".equals(st)) {
            Render2D.rect(context, boxX, boxY, boxW, boxH, 5f, 0xEE141419);
            Render2D.outline(context, boxX, boxY, boxW, boxH, 5f, 1f, 0x30FFFFFF);
        } else if ("Неоновый (Neon)".equals(st)) {
            HudDraw.glass(context, boxX, boxY, boxW, boxH, 5f);
            Render2D.outline(context, boxX, boxY, boxW, boxH, 5f, 1.2f, 0xFF00CEC9);
        } else {
            HudDraw.glass(context, boxX, boxY, boxW, boxH, 5f);
        }

        if (hasTitle) {
            context.drawText(mc.textRenderer, stack.getName(), boxX + pad + 2, boxY + pad, 0xFFE0E0E0, false);
            if (module.showTotalItems.get()) {
                int total = 0;
                for (ItemStack it : items) {
                    if (!it.isEmpty()) total += it.getCount();
                }
                String cntStr = "x" + total;
                int cw = mc.textRenderer.getWidth(cntStr);
                context.drawText(mc.textRenderer, cntStr, boxX + boxW - pad - cw - 2, boxY + pad, 0xFFAAAAAA, false);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int sx = boxX + pad + col * (slotSize + gap);
                int sy = boxY + pad + titleH + row * (slotSize + gap);

                if (module.showEmptySlots.get()) {
                    Render2D.rect(context, sx, sy, slotSize, slotSize, 3f, 0x1AFFFFFF);
                }

                int idx = row * 9 + col;
                if (idx < items.size()) {
                    ItemStack it = items.get(idx);
                    if (!it.isEmpty()) {
                        if (it.isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING)
                                || it.isOf(net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE)
                                || it.isOf(net.minecraft.item.Items.END_CRYSTAL)
                                || it.isOf(net.minecraft.item.Items.RESPAWN_ANCHOR)
                                || it.isOf(net.minecraft.item.Items.ELYTRA)
                                || it.isOf(net.minecraft.item.Items.NETHERITE_INGOT)
                                || it.isOf(net.minecraft.item.Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
                            Render2D.outline(context, sx - 1, sy - 1, slotSize + 2, slotSize + 2, 3f, 1.2f, 0xAAFFB703);
                        }
                        context.drawItem(it, sx, sy);
                        context.drawStackOverlay(mc.textRenderer, it, sx, sy);
                    }
                }
            }
        }

        context.getMatrices().popMatrix();
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void svintus$drawSlotOverlays(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (slot == null || !slot.hasStack()) return;
        ItemStack stack = slot.getStack();

        // 1. Inventory Item Highlight
        com.lowcode.svintusclient.client.module.impl.render.ItemHighlightModule ih = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ItemHighlightModule.class);
        if (ih != null && ih.isEnabled()) {
            int color = ih.getHighlightColor(stack);
            if (color != 0) {
                String st = ih.style.get();
                if ("Светящаяся рамка (Border)".equals(st)) {
                    Render2D.outline(context, slot.x - 1, slot.y - 1, 18f, 18f, 2.5f, 1.2f, color | 0xFF000000);
                } else if ("Неоновый контур (Glow)".equals(st)) {
                    Render2D.outline(context, slot.x - 1, slot.y - 1, 18f, 18f, 2.5f, 1.5f, color | 0xFF000000);
                } else if ("Точка-маркер (Dot)".equals(st)) {
                    Render2D.circle(context, slot.x + 13.5f, slot.y + 2.5f, 1.8f, color | 0xFF000000);
                } else {
                    Render2D.rect(context, slot.x, slot.y, 16f, 16f, 2.0f, color);
                }
            }
        }

        // 2. Item Durability Overlay
        com.lowcode.svintusclient.client.module.impl.render.ItemDurabilityOverlayModule ido = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ItemDurabilityOverlayModule.class);
        if (ido != null && ido.isEnabled()) {
            ido.renderSlotOverlay(context, stack, slot.x, slot.y, true);
        }

        // 3. Matching Item Highlight
        if (ih != null && ih.isEnabled() && focusedSlot != null && focusedSlot.hasStack() && focusedSlot != slot) {
            ih.renderMatchingHighlight(context, stack, focusedSlot.getStack(), slot.x, slot.y);
        }

        // 4. Lock Slot Indicator
        com.lowcode.svintusclient.client.module.impl.render.LockSlotModule ls = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.LockSlotModule.class);
        if (ls != null && ls.isEnabled() && ls.lockIcon.get()) {
            int slotIdx = slot.getIndex();
            if (slotIdx >= 0 && slotIdx < 9 && ls.isSlotLocked(slotIdx)) {
                Render2D.rect(context, slot.x + 12f, slot.y + 1f, 3.5f, 3.5f, 1f, 0xFFE74C3C);
            }
        }

        // 5. Profitable Item & Market Helper Auction Highlighting
        com.lowcode.svintusclient.client.module.impl.misc.ProfitableItemModule pi = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.ProfitableItemModule.class);
        if (pi != null && pi.isEnabled() && pi.isBargainLot(stack)) {
            int glowAlpha = (int) (Math.min(1.0f, Math.max(0.1f, pi.highlightGlow.get())) * 255.0f);
            Render2D.rect(context, slot.x, slot.y, 16f, 16f, 2.0f, ((glowAlpha / 3) << 24) | 0x002ECC71);
            Render2D.outline(context, slot.x - 1, slot.y - 1, 18f, 18f, 2.5f, 1.5f, (glowAlpha << 24) | 0x002ECC71);
        }

        com.lowcode.svintusclient.client.module.impl.misc.MarketHelperModule mh = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.MarketHelperModule.class);
        if (mh != null && mh.isEnabled() && mh.highlightCheap.get() && mh.isCheapLot(stack)) {
            int c = mh.cheapColor.get();
            Render2D.outline(context, slot.x - 1, slot.y - 1, 18f, 18f, 2.5f, 1.2f, c | 0xFF000000);
        }

    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void svintus$preventLockedSlotClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        if (slot == null) return;
        com.lowcode.svintusclient.client.module.impl.render.LockSlotModule ls = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.LockSlotModule.class);
        if (ls != null && ls.isEnabled()) {
            if (actionType == SlotActionType.THROW) {
                int slotIdx = slot.getIndex();
                if (slotIdx >= 0 && slotIdx < 9 && ls.isSlotLocked(slotIdx)) {
                    ci.cancel();
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        mc.player.sendMessage(net.minecraft.text.Text.literal("§c🔒 Слот " + (slotIdx + 1) + " защищен от выброса!"), true);
                        if (ls.soundAlert.get()) {
                            com.lowcode.svintusclient.client.util.SoundPreview.play("Барабан", 0.8f, 0.9f);
                        }
                    }
                }
            }
        }
    }
}
