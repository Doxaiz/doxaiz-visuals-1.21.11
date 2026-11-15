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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PotionsWidget extends HudWidget {

    private static final float SIZE = 6.0f, LINE_H = 14f, PAD = 6f;

    private final BooleanSetting showAmplifier = option("Уровень эффекта", true);
    private final BooleanSetting showDuration = option("Время действия", true);
    public final BooleanSetting hideVanilla = option("Скрыть ванильные иконки", true);

    public record PotionEntry(String name, String duration, int color) {}
    private final List<PotionEntry> entries = new ArrayList<>();

    public PotionsWidget() {
        super("potions", "Зелья / Эффекты", 0f, 0.2f);
    }

    @Override
    public boolean isVisible() {
        com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule.class);
        if (mod != null) return mod.isEnabled();
        return ModuleManager.HUD != null && ModuleManager.HUD.potions.get();
    }

    @Override
    public void setVisible(boolean visible) {
        com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule.class);
        if (mod != null) mod.setEnabled(visible);
        if (ModuleManager.HUD != null) ModuleManager.HUD.potions.set(visible);
    }

    @Override
    protected void layout(boolean editing) {
        entries.clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;

        if (player != null) {
            com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule phm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule.class);
            boolean useAmp = phm != null ? phm.showAmplifier.get() : showAmplifier.get();
            boolean useDur = phm != null ? phm.showDuration.get() : showDuration.get();

            Collection<StatusEffectInstance> effects = player.getStatusEffects();
            com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule pMod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule.class);
            for (StatusEffectInstance inst : effects) {
                String name = inst.getEffectType().value().getName().getString();
                if (useAmp && inst.getAmplifier() > 0) {
                    name += " " + toRoman(inst.getAmplifier() + 1);
                }
                String dur = "";
                if (useDur) {
                    int totalSec = inst.getDuration() / 20;
                    int m = totalSec / 60;
                    int s = totalSec % 60;
                    dur = String.format("%02d:%02d", m, s);
                }

                int color = inst.getEffectType().value().getColor() | 0xFF000000;
                if (pMod != null) {
                    String cm = pMod.colorMode.get();
                    if ("Акцент темы".equals(cm)) {
                        color = Theme.accent();
                    } else if ("Радуга".equals(cm)) {
                        float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
                        color = 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
                    } else if ("Пользовательский".equals(cm)) {
                        color = pMod.customColor.getEffectiveColor();
                    }
                }

                if (pMod != null && pMod.blinkWarning.get() && inst.getDuration() < 200 && inst.getDuration() > 0) {
                    if ((System.currentTimeMillis() / 250L) % 2 == 0) {
                        color = 0xFFFF4757;
                    }
                }

                entries.add(new PotionEntry(name, dur, color));
            }
        }

        if (entries.isEmpty() && editing) {
            entries.add(new PotionEntry("Скорость II", "01:45", 0xFF33D9B2));
            entries.add(new PotionEntry("Сила I", "00:50", 0xFFFF5252));
        }

        float maxW = 75f;
        for (PotionEntry pe : entries) {
            float lineW = PAD * 2f + Fonts.SEMIBOLD.width(pe.name, SIZE) + 12f + Fonts.REGULAR.width(pe.duration, SIZE);
            if (lineW > maxW) maxW = lineW;
        }

        w = maxW;
        h = Math.max(LINE_H + PAD, entries.size() * LINE_H + PAD + 12f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        if (entries.isEmpty() && !editing) return;

        com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule pMod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.PotionsHudModule.class);
        String st = pMod != null ? pMod.style.get() : "Стекло (Glass)";

        if ("Неон".equals(st)) {
            HudDraw.glass(ctx, x, y, w, h, 6f);
            Render2D.outline(ctx, x, y, w, h, 6f, 1.2f, Theme.accent());
        } else if ("Минимализм".equals(st)) {
            Render2D.rect(ctx, x, y, w, h, 4f, 0xCC111115);
        } else {
            HudDraw.glass(ctx, x, y, w, h, 6f);
        }

        // Header
        float curY = y + 4f;
        Render2D.rect(ctx, x + PAD, curY + 2f, 3f, 7f, 1.5f, Theme.accent());
        Fonts.SEMIBOLD.draw(ctx, "Эффекты", x + PAD + 6f, curY + 1f, SIZE + 0.4f, Theme.TEXT);
        curY += 13f;

        for (PotionEntry pe : entries) {
            // Accent colored dot
            Render2D.rect(ctx, x + PAD, curY + 4f, 4f, 4f, 2f, pe.color);

            // Name
            Fonts.REGULAR.draw(ctx, pe.name, x + PAD + 8f, curY + 2f, SIZE, Theme.TEXT);

            // Duration
            if (!pe.duration.isEmpty()) {
                float durW = Fonts.REGULAR.width(pe.duration, SIZE);
                Fonts.REGULAR.draw(ctx, pe.duration, x + w - PAD - durW, curY + 2f, SIZE, Theme.TEXT_DIM);
            }
            curY += LINE_H;
        }
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
