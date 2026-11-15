package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.util.SoundPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;

import java.util.*;

public final class ItemPickupLogModule extends Module implements TickListener {

    public final ModeSetting style = add(new ModeSetting("Стиль карточек",
            "Современное стекло", "Компактный", "Минимализм"));
    public final NumberSetting duration = add(new NumberSetting("Время показа (сек)", 3.5f, 1.0f, 8.0f, 0.5f));
    public final ModeSetting soundMode = add(new ModeSetting("Звук подбора",
            "Поп", "Bonk", "Клик", "8-бит", "Выкл"));
    public final ModeSetting position = add(new ModeSetting("Позиция на экране",
            "Справа внизу", "Слева по центру", "Справа вверху"));

    public static final class PickupEntry {
        public final ItemStack stack;
        public final int count;
        public final long spawnTime;

        public PickupEntry(ItemStack stack, int count) {
            this.stack = stack.copy();
            this.count = count;
            this.spawnTime = System.currentTimeMillis();
        }
    }

    private final List<PickupEntry> entries = new ArrayList<>();
    private final Map<Item, Integer> lastCounts = new HashMap<>();
    private boolean initialized = false;

    public ItemPickupLogModule() {
        super("Item Pickup Log", "Всплывающий анимированный лог подобранных предметов на экране", Category.INTERFACE);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            lastCounts.clear();
            initialized = false;
            return;
        }

        Map<Item, Integer> currentCounts = new HashMap<>();
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                currentCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }

        if (initialized) {
            for (Map.Entry<Item, Integer> entry : currentCounts.entrySet()) {
                int prev = lastCounts.getOrDefault(entry.getKey(), 0);
                if (entry.getValue() > prev) {
                    int gained = entry.getValue() - prev;
                    onPickup(new ItemStack(entry.getKey()), gained);
                }
            }
        }

        lastCounts.clear();
        lastCounts.putAll(currentCounts);
        initialized = true;
    }

    public void onPickup(ItemStack stack, int count) {
        if (!isEnabled() || stack == null || stack.isEmpty()) return;

        // Group with recent entry if same item
        long now = System.currentTimeMillis();
        boolean merged = false;
        for (int i = entries.size() - 1; i >= 0; i--) {
            PickupEntry e = entries.get(i);
            if (now - e.spawnTime < 1500L && ItemStack.areItemsEqual(e.stack, stack)) {
                entries.set(i, new PickupEntry(e.stack, e.count + count));
                merged = true;
                break;
            }
        }
        if (!merged) {
            entries.add(new PickupEntry(stack, count));
            if (entries.size() > 6) entries.remove(0);
        }

        SoundPreview.playPreview(soundMode.get(), 1.0f);
    }

    public void render(DrawContext ctx, float screenW, float screenH) {
        if (!isEnabled() || entries.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        long now = System.currentTimeMillis();
        long maxLife = (long) (duration.get() * 1000.0f);

        Iterator<PickupEntry> it = entries.iterator();
        while (it.hasNext()) {
            PickupEntry e = it.next();
            if (now - e.spawnTime > maxLife) {
                it.remove();
            }
        }

        if (entries.isEmpty()) return;

        float cardW = 125.0f;
        float cardH = 20.0f;
        float gap = 3.0f;

        float startX;
        float startY;
        String pos = position.get();
        if ("Слева по центру".equals(pos)) {
            startX = 10.0f;
            startY = screenH * 0.45f;
        } else if ("Справа вверху".equals(pos)) {
            startX = screenW - cardW - 10.0f;
            startY = 35.0f;
        } else {
            // Справа внизу
            startX = screenW - cardW - 10.0f;
            startY = screenH - 50.0f - (entries.size() * (cardH + gap));
        }

        String st = style.get();

        for (int i = 0; i < entries.size(); i++) {
            PickupEntry e = entries.get(i);
            float age = (float) (now - e.spawnTime);
            float progress = age / maxLife;

            // Slide & fade animation
            float slide = 1.0f;
            if (age < 250.0f) {
                slide = age / 250.0f;
            } else if (progress > 0.8f) {
                slide = (1.0f - progress) / 0.2f;
            }
            slide = Math.max(0.0f, Math.min(1.0f, slide));

            float curY = startY + i * (cardH + gap);
            float curX = startX + (1.0f - slide) * (startX < screenW / 2 ? -30f : 30f);

            ctx.getMatrices().pushMatrix();

            if ("Компактный".equals(st)) {
                Render2D.rect(ctx, curX, curY, cardW, cardH, 4.0f, 0xCC111116);
            } else if ("Минимализм".equals(st)) {
                Render2D.rect(ctx, curX, curY, cardW, cardH, 3.0f, 0x90000000);
            } else {
                HudDraw.glass(ctx, curX, curY, cardW, cardH, 4.5f);
            }

            // Draw item stack icon
            ctx.drawItem(e.stack, (int) (curX + 2f), (int) (curY + 2f));

            // Name + count
            String name = e.stack.getName().getString();
            if (name.length() > 14) name = name.substring(0, 13) + "…";
            String countText = "+" + e.count;

            Fonts.SEMIBOLD.draw(ctx, countText, curX + 22.0f, curY + 4.0f, 5.6f, Theme.accent());
            Fonts.REGULAR.draw(ctx, name, curX + 22.0f + Fonts.SEMIBOLD.width(countText, 5.6f) + 4.0f, curY + 4.0f, 5.2f, Theme.TEXT);

            ctx.getMatrices().popMatrix();
        }
    }
}
