package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class CrosshairModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль",
            "Крестик", "Круг", "Точка", "Кольцо с точкой", "Крест с точкой", "Шеврон (Chevron)",
            "Ромб (Diamond)", "Треугольник (Triangle)", "CS-Cross", "T-Cross", "Квадрат (Box)", "Снайпер (Sniper)"));
    public final NumberSetting size = add(new NumberSetting("Размер", 5.0f, 2.0f, 16.0f, 0.5f));
    public final NumberSetting thickness = add(new NumberSetting("Толщина", 1.5f, 1.0f, 4.0f, 0.5f));
    public final NumberSetting gap = add(new NumberSetting("Зазор", 2.5f, 0.0f, 12.0f, 0.5f));
    public final BooleanSetting dynamicSpread = add(new BooleanSetting("Динамический разброс", true));
    public final BooleanSetting cooldownRing = add(new BooleanSetting("Индикатор атаки (Кольцо)", true));
    public final BooleanSetting dot = add(new BooleanSetting("Центральная точка", false));
    public final BooleanSetting outline = add(new BooleanSetting("Тёмная обводка", true));
    public final BooleanSetting targetReact = add(new BooleanSetting("Реакция на цель", true));
    public final BooleanSetting inertia = add(new BooleanSetting("Инерция движения мыши", false));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета", "Свой цвет", "Акцент темы"));
    public final ColorSetting color = add(new ColorSetting("Цвет прицела", 0xFF05D9E8));

    private float prevYaw;
    private float prevPitch;
    private boolean prevInit;

    public CrosshairModule() {
        super("Прицел Pro", "Кастомный тактический прицел с настраиваемыми стилями и индикатором удара", Category.BATTLE);
    }

    public void render(DrawContext ctx, float screenW, float screenH) {
        if (!isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden) return;

        float cx = screenW / 2.0f;
        float cy = screenH / 2.0f;

        if (inertia.get() && mc.player != null) {
            float curYaw = mc.player.getYaw();
            float curPitch = mc.player.getPitch();
            if (!prevInit) {
                prevYaw = curYaw;
                prevPitch = curPitch;
                prevInit = true;
            }
            float dYaw = (curYaw - prevYaw) * 0.45f;
            float dPitch = (curPitch - prevPitch) * 0.45f;
            if (Float.isFinite(dYaw)) cx += Math.max(-25.0f, Math.min(25.0f, dYaw));
            if (Float.isFinite(dPitch)) cy += Math.max(-25.0f, Math.min(25.0f, dPitch));
            prevYaw += (curYaw - prevYaw) * 0.15f;
            prevPitch += (curPitch - prevPitch) * 0.15f;
        } else {
            prevInit = false;
        }

        float cooldown = 1.0f;
        float spread = gap.get();
        if (mc.player != null) {
            cooldown = mc.player.getAttackCooldownProgress(0.0f);
            if (dynamicSpread.get()) {
                if (cooldown < 0.99f) spread += (1.0f - cooldown) * 5.0f;
                if (mc.player.isSprinting()) spread += 2.5f;
            }
        }

        int col = "Акцент темы".equals(colorMode.get()) ? Theme.accent() : color.getEffectiveColor();
        if (targetReact.get() && mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr && ehr.getEntity() instanceof net.minecraft.entity.LivingEntity) {
            col = 0xFFFF2244;
        }

        float sz = size.get();
        float th = thickness.get();
        String st = style.get();
        boolean out = outline.get();
        int outCol = 0xAA000000;

        // 1. Center dot
        if (dot.get() || "Точка".equals(st) || "Кольцо с точкой".equals(st) || "Крест с точкой".equals(st) || "Снайпер (Sniper)".equals(st)) {
            float dotSz = Math.max(1.6f, th);
            if (out) Render2D.circle(ctx, cx, cy, dotSz / 2f + 0.8f, outCol);
            Render2D.circle(ctx, cx, cy, dotSz / 2f, col);
        }

        // 2. Styles
        if ("Крестик".equals(st) || "Крест с точкой".equals(st)) {
            if (out) {
                Render2D.rect(ctx, cx - th / 2f - 0.75f, cy - spread - sz - 0.75f, th + 1.5f, sz + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx - th / 2f - 0.75f, cy + spread - 0.75f, th + 1.5f, sz + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx - spread - sz - 0.75f, cy - th / 2f - 0.75f, sz + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx + spread - 0.75f, cy - th / 2f - 0.75f, sz + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
            }
            Render2D.rect(ctx, cx - th / 2f, cy - spread - sz, th, sz, th / 2f, col);
            Render2D.rect(ctx, cx - th / 2f, cy + spread, th, sz, th / 2f, col);
            Render2D.rect(ctx, cx - spread - sz, cy - th / 2f, sz, th, th / 2f, col);
            Render2D.rect(ctx, cx + spread, cy - th / 2f, sz, th, th / 2f, col);
        } else if ("CS-Cross".equals(st)) {
            // Flat sharp Counter-Strike cross (zero radius rounding)
            if (out) {
                Render2D.rect(ctx, cx - th / 2f - 1.0f, cy - spread - sz - 1.0f, th + 2.0f, sz + 2.0f, 0f, outCol);
                Render2D.rect(ctx, cx - th / 2f - 1.0f, cy + spread - 1.0f, th + 2.0f, sz + 2.0f, 0f, outCol);
                Render2D.rect(ctx, cx - spread - sz - 1.0f, cy - th / 2f - 1.0f, sz + 2.0f, th + 2.0f, 0f, outCol);
                Render2D.rect(ctx, cx + spread - 1.0f, cy - th / 2f - 1.0f, sz + 2.0f, th + 2.0f, 0f, outCol);
            }
            Render2D.rect(ctx, cx - th / 2f, cy - spread - sz, th, sz, 0f, col);
            Render2D.rect(ctx, cx - th / 2f, cy + spread, th, sz, 0f, col);
            Render2D.rect(ctx, cx - spread - sz, cy - th / 2f, sz, th, 0f, col);
            Render2D.rect(ctx, cx + spread, cy - th / 2f, sz, th, 0f, col);
        } else if ("T-Cross".equals(st)) {
            // T-shaped reticle (Left, Right, Bottom bars, completely open top for clear headshots)
            if (out) {
                Render2D.rect(ctx, cx - th / 2f - 0.75f, cy + spread - 0.75f, th + 1.5f, sz + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx - spread - sz - 0.75f, cy - th / 2f - 0.75f, sz + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx + spread - 0.75f, cy - th / 2f - 0.75f, sz + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
            }
            Render2D.rect(ctx, cx - th / 2f, cy + spread, th, sz, th / 2f, col);
            Render2D.rect(ctx, cx - spread - sz, cy - th / 2f, sz, th, th / 2f, col);
            Render2D.rect(ctx, cx + spread, cy - th / 2f, sz, th, th / 2f, col);
        } else if ("Круг".equals(st) || "Кольцо с точкой".equals(st)) {
            float radius = spread + sz;
            if (out) Render2D.circleOutline(ctx, cx, cy, radius, th + 1.5f, outCol);
            Render2D.circleOutline(ctx, cx, cy, radius, th, col);
        } else if ("Шеврон (Chevron)".equals(st)) {
            // Chevron arrow chevron pointing down + horizontal wings
            float wingOff = spread + sz * 0.7f;
            float chevY = cy + spread;
            if (out) {
                Render2D.rect(ctx, cx - sz - 0.75f, cy - th / 2f - 0.75f, sz * 0.7f + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx + sz * 0.3f - 0.75f, cy - th / 2f - 0.75f, sz * 0.7f + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx - sz * 0.6f - 0.75f, chevY - 0.75f, sz * 1.2f + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx - th / 2f - 0.75f, chevY + th - 0.75f, th + 1.5f, sz * 0.5f + 1.5f, (th + 1.5f) / 2f, outCol);
            }
            Render2D.rect(ctx, cx - sz, cy - th / 2f, sz * 0.7f, th, th / 2f, col);
            Render2D.rect(ctx, cx + sz * 0.3f, cy - th / 2f, sz * 0.7f, th, th / 2f, col);
            Render2D.rect(ctx, cx - sz * 0.6f, chevY, sz * 1.2f, th, th / 2f, col);
            Render2D.rect(ctx, cx - th / 2f, chevY + th, th, sz * 0.5f, th / 2f, col);
        } else if ("Ромб (Diamond)".equals(st) || "Ромб".equals(st)) {
            float d = spread + sz;
            if (out) {
                Render2D.rect(ctx, cx - th / 2f - 0.75f, cy - d - 0.75f, th + 1.5f, th * 2f + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx - th / 2f - 0.75f, cy + d - th * 2f - 0.75f, th + 1.5f, th * 2f + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx - d - 0.75f, cy - th / 2f - 0.75f, th * 2f + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
                Render2D.rect(ctx, cx + d - th * 2f - 0.75f, cy - th / 2f - 0.75f, th * 2f + 1.5f, th + 1.5f, (th + 1.5f) / 2f, outCol);
            }
            Render2D.rect(ctx, cx - th / 2f, cy - d, th, th * 2f, th / 2f, col);
            Render2D.rect(ctx, cx - th / 2f, cy + d - th * 2f, th, th * 2f, th / 2f, col);
            Render2D.rect(ctx, cx - d, cy - th / 2f, th * 2f, th, th / 2f, col);
            Render2D.rect(ctx, cx + d - th * 2f, cy - th / 2f, th * 2f, th, th / 2f, col);
        } else if ("Треугольник (Triangle)".equals(st)) {
            // Delta triangle around center
            float triR = spread + sz;
            for (int i = 0; i < 3; i++) {
                double a = (i * 2.0 * Math.PI / 3.0) - Math.PI / 2.0;
                float px = (float) (cx + Math.cos(a) * triR);
                float py = (float) (cy + Math.sin(a) * triR);
                if (out) Render2D.circle(ctx, px, py, th / 2f + 1.0f, outCol);
                Render2D.circle(ctx, px, py, th / 2f, col);
            }
        } else if ("Квадрат (Box)".equals(st)) {
            // Corner brackets box reticle
            float b = spread + sz;
            float cornerLen = Math.max(3.0f, sz * 0.45f);
            if (out) {
                // Top-left
                Render2D.rect(ctx, cx - b - 0.75f, cy - b - 0.75f, cornerLen + 1.5f, th + 1.5f, 0f, outCol);
                Render2D.rect(ctx, cx - b - 0.75f, cy - b - 0.75f, th + 1.5f, cornerLen + 1.5f, 0f, outCol);
                // Top-right
                Render2D.rect(ctx, cx + b - cornerLen - 0.75f, cy - b - 0.75f, cornerLen + 1.5f, th + 1.5f, 0f, outCol);
                Render2D.rect(ctx, cx + b - th - 0.75f, cy - b - 0.75f, th + 1.5f, cornerLen + 1.5f, 0f, outCol);
                // Bottom-left
                Render2D.rect(ctx, cx - b - 0.75f, cy + b - th - 0.75f, cornerLen + 1.5f, th + 1.5f, 0f, outCol);
                Render2D.rect(ctx, cx - b - 0.75f, cy + b - cornerLen - 0.75f, th + 1.5f, cornerLen + 1.5f, 0f, outCol);
                // Bottom-right
                Render2D.rect(ctx, cx + b - cornerLen - 0.75f, cy + b - th - 0.75f, cornerLen + 1.5f, th + 1.5f, 0f, outCol);
                Render2D.rect(ctx, cx + b - th - 0.75f, cy + b - cornerLen - 0.75f, th + 1.5f, cornerLen + 1.5f, 0f, outCol);
            }
            // Top-left
            Render2D.rect(ctx, cx - b, cy - b, cornerLen, th, 0f, col);
            Render2D.rect(ctx, cx - b, cy - b, th, cornerLen, 0f, col);
            // Top-right
            Render2D.rect(ctx, cx + b - cornerLen, cy - b, cornerLen, th, 0f, col);
            Render2D.rect(ctx, cx + b - th, cy - b, th, cornerLen, 0f, col);
            // Bottom-left
            Render2D.rect(ctx, cx - b, cy + b - th, cornerLen, th, 0f, col);
            Render2D.rect(ctx, cx - b, cy + b - cornerLen, th, cornerLen, 0f, col);
            // Bottom-right
            Render2D.rect(ctx, cx + b - cornerLen, cy + b - th, cornerLen, th, 0f, col);
            Render2D.rect(ctx, cx + b - th, cy + b - cornerLen, th, cornerLen, 0f, col);
        } else if ("Снайпер (Sniper)".equals(st)) {
            // Extended hairline cross with tactical ring
            float sniperSz = sz * 2.2f;
            float ringR = spread + sz * 0.9f;
            if (out) {
                Render2D.circleOutline(ctx, cx, cy, ringR, th + 1.2f, outCol);
                Render2D.rect(ctx, cx - th / 2f - 0.75f, cy - spread - sniperSz - 0.75f, th + 1.5f, sniperSz + 1.5f, 0f, outCol);
                Render2D.rect(ctx, cx - th / 2f - 0.75f, cy + spread - 0.75f, th + 1.5f, sniperSz + 1.5f, 0f, outCol);
                Render2D.rect(ctx, cx - spread - sniperSz - 0.75f, cy - th / 2f - 0.75f, sniperSz + 1.5f, th + 1.5f, 0f, outCol);
                Render2D.rect(ctx, cx + spread - 0.75f, cy - th / 2f - 0.75f, sniperSz + 1.5f, th + 1.5f, 0f, outCol);
            }
            Render2D.circleOutline(ctx, cx, cy, ringR, Math.max(1.0f, th * 0.8f), col);
            Render2D.rect(ctx, cx - th / 2f, cy - spread - sniperSz, th, sniperSz, 0f, col);
            Render2D.rect(ctx, cx - th / 2f, cy + spread, th, sniperSz, 0f, col);
            Render2D.rect(ctx, cx - spread - sniperSz, cy - th / 2f, sniperSz, th, 0f, col);
            Render2D.rect(ctx, cx + spread, cy - th / 2f, sniperSz, th, 0f, col);
        }

        // 3. Attack recharge cooldown indicator
        if (cooldownRing.get() && cooldown < 0.99f) {
            float ringRadius = spread + sz + 3.5f;
            int ringSegments = (int) (cooldown * 24.0f);
            int ringColor = ((int) (220 * cooldown) << 24) | (col & 0x00FFFFFF);
            for (int i = 0; i < ringSegments; i++) {
                double angle = (i * 2.0 * Math.PI) / 24.0 - Math.PI / 2.0;
                float px = (float) (cx + Math.cos(angle) * ringRadius);
                float py = (float) (cy + Math.sin(angle) * ringRadius);
                Render2D.circle(ctx, px, py, 0.9f, ringColor);
            }
        }
    }
}
