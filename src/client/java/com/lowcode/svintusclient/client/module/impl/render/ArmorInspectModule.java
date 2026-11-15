package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArmorInspectModule extends Module {

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public final ModeSetting position = add(new ModeSetting("Расположение", "Над игроком", "Под прицелом", "Сбоку экрана"));
    public final BooleanSetting showDurability = add(new BooleanSetting("Прочность (%)", true));
    public final BooleanSetting showHeldItems = add(new BooleanSetting("Предметы в руках", true));
    public final NumberSetting maxDistance = add(new NumberSetting("Макс. дистанция", 15.0f, 5.0f, 30.0f, 1.0f));

    private PlayerEntity lastTarget;
    private long lastTargetTime;

    public ArmorInspectModule() {
        super("Armor Inspect", "Быстрый осмотр брони, предметов и прочности при взгляде на игрока", Category.UTILITIES);
        enableByDefault();
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        if (mc.targetedEntity instanceof PlayerEntity p && p != mc.player && p.isAlive()) {
            if (mc.player.distanceTo(p) <= maxDistance.get()) {
                lastTarget = p;
                lastTargetTime = System.currentTimeMillis();
            }
        } else {
            // Check TargetMarker target
            TargetMarkerModule tm = com.lowcode.svintusclient.client.module.ModuleManager.get(TargetMarkerModule.class);
            if (tm != null && tm.getCurrentTarget() instanceof PlayerEntity tp && tp != mc.player && tp.isAlive()) {
                if (mc.player.distanceTo(tp) <= maxDistance.get()) {
                    lastTarget = tp;
                    lastTargetTime = System.currentTimeMillis();
                }
            } else {
                // Raycast players within maxDistance
                float maxDist = maxDistance.get();
                net.minecraft.util.math.Vec3d camPos = mc.player.getCameraPosVec(1.0f);
                net.minecraft.util.math.Vec3d lookVec = mc.player.getRotationVec(1.0f);
                net.minecraft.util.math.Vec3d endPos = camPos.add(lookVec.multiply(maxDist));

                PlayerEntity closest = null;
                double closestDistSq = maxDist * maxDist;

                for (PlayerEntity other : mc.world.getPlayers()) {
                    if (other == mc.player || !other.isAlive() || other.isSpectator()) continue;
                    java.util.Optional<net.minecraft.util.math.Vec3d> hit = other.getBoundingBox().expand(0.35).raycast(camPos, endPos);
                    if (hit.isPresent()) {
                        double distSq = camPos.squaredDistanceTo(hit.get());
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            closest = other;
                        }
                    }
                }
                if (closest != null) {
                    lastTarget = closest;
                    lastTargetTime = System.currentTimeMillis();
                }
            }
        }

        if (lastTarget == null || !lastTarget.isAlive()) return;
        long elapsed = System.currentTimeMillis() - lastTargetTime;
        if (elapsed > 2000L) return;
        if (mc.player.distanceTo(lastTarget) > maxDistance.get() + 3.0f) return;

        float alpha = elapsed > 1500L ? (1.0f - (elapsed - 1500L) / 500.0f) : 1.0f;
        Render2D.setAlpha(alpha);

        List<ItemStack> items = new ArrayList<>();
        if (showHeldItems.get()) {
            items.add(lastTarget.getMainHandStack());
            items.add(lastTarget.getOffHandStack());
        }
        for (EquipmentSlot slot : ARMOR) {
            items.add(lastTarget.getEquippedStack(slot));
        }

        boolean hasAny = false;
        for (ItemStack st : items) {
            if (!st.isEmpty()) {
                hasAny = true;
                break;
            }
        }
        if (!hasAny) return;

        int slotCount = items.size();
        float slotSize = 18f;
        float gap = 3f;
        float pad = 6f;

        float slotsW = slotCount * slotSize + (slotCount - 1) * gap;
        String name = lastTarget.getName().getString();
        String dist = String.format(Locale.US, "%.1fm", mc.player.distanceTo(lastTarget));
        int headerTextW = mc.textRenderer.getWidth(name) + mc.textRenderer.getWidth(dist) + 24;

        float boxW = Math.max(slotsW, headerTextW) + pad * 2f;
        float boxH = 46f;

        float x;
        float y;
        String posMode = position.get();
        if ("Под прицелом".equals(posMode)) {
            x = (screenWidth - boxW) / 2f;
            y = screenHeight / 2f + 20f;
        } else if ("Сбоку экрана".equals(posMode)) {
            x = screenWidth - boxW - 12f;
            y = (screenHeight - boxH) / 2f;
        } else {
            // "Над игроком"
            x = (screenWidth - boxW) / 2f;
            y = 38f;
        }

        // Акриловое стекло SvintusClient
        HudDraw.glass(context, x, y, boxW, boxH, 6f);
        Render2D.outline(context, x, y, boxW, boxH, 6f, 1.0f, 0x25FFFFFF);

        // Голова скина и ник
        HudDraw.head(context, lastTarget, x + pad, y + pad, 14f);
        context.drawTextWithShadow(mc.textRenderer, name, (int) (x + pad + 18f), (int) (y + pad + 3f), 0xFFFFFFFF);
        int distW = mc.textRenderer.getWidth(dist);
        context.drawTextWithShadow(mc.textRenderer, dist, (int) (x + boxW - pad - distW), (int) (y + pad + 3f), 0xFFAAAAAA);

        // Ячейки экипировки
        float startX = x + (boxW - slotsW) / 2f;
        float itemY = y + 23f;

        for (int i = 0; i < slotCount; i++) {
            ItemStack stack = items.get(i);
            float ix = startX + i * (slotSize + gap);

            Render2D.rect(context, ix, itemY, slotSize, slotSize, 3f, 0x14FFFFFF);

            if (!stack.isEmpty()) {
                HudDraw.item(context, stack, ix + 1f, itemY + 1f, 16f);
                HudDraw.durability(context, stack, ix + 1f, itemY + 16.5f, 16f);

                if (showDurability.get() && stack.isDamageable() && stack.getMaxDamage() > 0) {
                    int percent = HudDraw.durabilityPercent(stack);
                    if (percent >= 0) {
                        String pStr = percent + "%";
                        int col = percent > 50 ? 0xFF55FF55 : (percent > 20 ? 0xFFFFFF55 : 0xFFFF5555);
                        context.getMatrices().pushMatrix();
                        context.getMatrices().translate(ix + slotSize / 2f, itemY + slotSize - 2f);
                        context.getMatrices().scale(0.5f, 0.5f);
                        int pW = mc.textRenderer.getWidth(pStr);
                        context.drawTextWithShadow(mc.textRenderer, pStr, -pW / 2, 0, col);
                        context.getMatrices().popMatrix();
                    }
                }
            }
        }
        Render2D.setAlpha(1.0f);
    }
}
