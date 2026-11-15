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
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class CooldownsWidget extends HudWidget {

    private static final float SIZE = 6.0f, LINE_H = 14f, PAD = 6f;

    private final BooleanSetting showProgressBar = option("Полоса прогресса", true);

    public record CooldownEntry(String name, float progress, String text, int color) {}
    private final List<CooldownEntry> entries = new ArrayList<>();

    private static final Item[] TRACKED_ITEMS = {
            Items.ENDER_PEARL,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.SHIELD,
            Items.CHORUS_FRUIT,
            Items.WIND_CHARGE,
            Items.COMPASS
    };

    public CooldownsWidget() {
        super("cooldowns", "Кулдауны", 0f, 0.4f);
    }

    @Override
    public boolean isVisible() {
        com.lowcode.svintusclient.client.module.impl.render.CooldownsHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CooldownsHudModule.class);
        if (mod != null) return mod.isEnabled();
        return ModuleManager.HUD != null && ModuleManager.HUD.cooldowns.get();
    }

    @Override
    public void setVisible(boolean visible) {
        com.lowcode.svintusclient.client.module.impl.render.CooldownsHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CooldownsHudModule.class);
        if (mod != null) mod.setEnabled(visible);
        if (ModuleManager.HUD != null) ModuleManager.HUD.cooldowns.set(visible);
    }

    @Override
    protected void layout(boolean editing) {
        entries.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;

        if (player != null) {
            com.lowcode.svintusclient.client.module.impl.render.CooldownsHudModule chm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CooldownsHudModule.class);
            String setMode = chm != null ? chm.trackedSet.get() : "Все PvP предметы";
            int cdColor = Theme.accent();
            if (chm != null) {
                String cm = chm.colorMode.get();
                if ("Золотой".equals(cm)) {
                    cdColor = 0xFFFFD700;
                } else if ("Градиент".equals(cm)) {
                    float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
                    cdColor = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
                } else if ("Свой цвет".equals(cm)) {
                    cdColor = chm.customColor.getEffectiveColor();
                }
            }

            ItemCooldownManager mgr = player.getItemCooldownManager();
            Item[] itemsToCheck = "Только Перл и Яблоки".equals(setMode)
                    ? new Item[]{Items.ENDER_PEARL, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE}
                    : TRACKED_ITEMS;

            for (Item item : itemsToCheck) {
                ItemStack stack = item.getDefaultStack();
                if (mgr.isCoolingDown(stack)) {
                    float progress = mgr.getCooldownProgress(stack, 0f); // 1.0 -> 0.0
                    String name = item.getName().getString();
                    String sec = String.format(java.util.Locale.US, "%.1fs", progress * 10f); // approximate remaining display
                    entries.add(new CooldownEntry(name, progress, sec, cdColor));
                }
            }

            com.lowcode.svintusclient.client.module.impl.misc.CompassCdModule ccd = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.CompassCdModule.class);
            if (ccd != null && ccd.isEnabled()) {
                String cTimer = ccd.getTimerText();
                if (!cTimer.isEmpty() && !"§aГотов".equals(cTimer)) {
                    entries.add(new CooldownEntry("Компас", 0.5f, cTimer, 0xFF00CEC9));
                }
            }
        }

        if (entries.isEmpty() && editing) {
            entries.add(new CooldownEntry("Жемчуг Края", 0.65f, "3.2s", 0xFF00D2D3));
            entries.add(new CooldownEntry("Щит", 0.30f, "1.5s", 0xFFFF9F43));
        }

        float maxW = 85f;
        for (CooldownEntry ce : entries) {
            float lineW = PAD * 2f + Fonts.REGULAR.width(ce.name, SIZE) + 12f + Fonts.SEMIBOLD.width(ce.text, SIZE);
            if (lineW > maxW) maxW = lineW;
        }

        w = maxW;
        h = Math.max(LINE_H + PAD, entries.size() * LINE_H + PAD + 12f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        if (entries.isEmpty() && !editing) return;

        com.lowcode.svintusclient.client.module.impl.render.CooldownsHudModule chm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CooldownsHudModule.class);
        String st = chm != null ? chm.style.get() : "Полосы (Bars)";

        if ("Неон".equals(st)) {
            HudDraw.glass(ctx, x, y, w, h, 6f);
            Render2D.outline(ctx, x, y, w, h, 6f, 1.2f, 0xFF00D2D3);
        } else if ("Компактный".equals(st)) {
            Render2D.rect(ctx, x, y, w, h, 4f, 0xCC111115);
        } else {
            HudDraw.glass(ctx, x, y, w, h, 6f);
        }

        // Header
        float curY = y + 4f;
        Render2D.rect(ctx, x + PAD, curY + 2f, 3f, 7f, 1.5f, 0xFF00D2D3);
        Fonts.SEMIBOLD.draw(ctx, "Кулдауны", x + PAD + 6f, curY + 1f, SIZE + 0.4f, Theme.TEXT);
        curY += 13f;

        for (CooldownEntry ce : entries) {
            // Name
            Fonts.REGULAR.draw(ctx, ce.name, x + PAD, curY + 2f, SIZE, Theme.TEXT);

            // Time text
            float txtW = Fonts.SEMIBOLD.width(ce.text, SIZE);
            Fonts.SEMIBOLD.draw(ctx, ce.text, x + w - PAD - txtW, curY + 2f, SIZE, Theme.accent());

            // Progress bar
            boolean usePbar = chm != null ? chm.showProgressBar.get() : showProgressBar.get();
            if (usePbar) {
                float barW = w - PAD * 2f;
                float fillW = Math.max(1f, barW * (1.0f - ce.progress));
                Render2D.rect(ctx, x + PAD, curY + 11f, barW, 1.5f, 0.75f, 0x22FFFFFF);
                Render2D.rect(ctx, x + PAD, curY + 11f, fillW, 1.5f, 0.75f, ce.color);
            }
            curY += LINE_H;
        }
    }
}
