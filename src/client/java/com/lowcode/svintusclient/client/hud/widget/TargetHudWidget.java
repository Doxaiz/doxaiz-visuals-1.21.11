package com.lowcode.svintusclient.client.hud.widget;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.hud.HudWidget;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.render.icon.Icons;
import com.lowcode.svintusclient.client.util.Anim;
import com.lowcode.svintusclient.client.util.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import java.util.Locale;

public class TargetHudWidget extends HudWidget {

    private static final float W = 150f, HEAD = 30f, PAD = 7f;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final long LINGER = 4000L;

    private final BooleanSetting items = option("Предметы и броня", true);
    private final BooleanSetting distance = option("Дистанция", true);
    private final BooleanSetting neonGlow = option("Неоновое свечение (Neon Glow)", true);
    private final BooleanSetting percentHp = option("Процент здоровья %", false);

    private final Anim show = new Anim(0f, 10f);
    private final Anim health = new Anim(0f, 8f);
    private final Anim damageLagAnim = new Anim(0f, 3.5f);
    private final Anim absorption = new Anim(0f, 8f);
    private final Anim hurt = new Anim(0f, 14f);
    private LivingEntity target;
    private long lastSeen;

    public TargetHudWidget() {
        super("target", "Таргет HUD", 0.5f, 0.66f);
    }

    @Override
    public boolean isVisible() {
        com.lowcode.svintusclient.client.module.impl.render.TargetHudModule thm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.TargetHudModule.class);
        if (thm != null && thm.isEnabled()) return true;
        return ModuleManager.HUD != null && ModuleManager.HUD.targetHud.get();
    }

    @Override
    public void setVisible(boolean v) {
        com.lowcode.svintusclient.client.module.impl.render.TargetHudModule thm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.TargetHudModule.class);
        if (thm != null) thm.setEnabled(v);
        if (ModuleManager.HUD != null) ModuleManager.HUD.targetHud.set(v);
    }

    private void updateTarget(boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        Entity looked = client.targetedEntity;
        long now = System.currentTimeMillis();
        if (looked instanceof LivingEntity living && living.isAlive()) {
            if (target != living) {
                health.snap(living.getHealth() / living.getMaxHealth());
                absorption.snap(0f);
            }
            target = living;
            lastSeen = now;
        } else {
            com.lowcode.svintusclient.client.module.impl.render.TargetMarkerModule tm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.TargetMarkerModule.class);
            if (tm != null && tm.getCurrentTarget() instanceof LivingEntity living && living.isAlive()) {
                if (target != living) {
                    health.snap(living.getHealth() / living.getMaxHealth());
                    absorption.snap(0f);
                }
                target = living;
                lastSeen = now;
            }
        }
        boolean alive = target != null && target.isAlive() && !target.isRemoved();
        boolean active = alive && now - lastSeen < LINGER;
        if (!active && editing && client.player != null) {
            if (target != client.player) health.snap(client.player.getHealth() / client.player.getMaxHealth());
            target = client.player;
            active = true;
        }
        show.to(active);
        if (!active && show.get() < 0.01f) target = null;
    }

    @Override
    protected boolean hasContent(boolean editing) {
        return target != null;
    }

    @Override
    protected void layout(boolean editing) {
        updateTarget(editing);
        com.lowcode.svintusclient.client.module.impl.render.TargetHudModule thm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.TargetHudModule.class);
        if (thm != null) this.scale = thm.scale.get();
        w = W;
        h = PAD * 2f + HEAD;
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        if (target == null) return;
        float a = Anim.easeOutCubic(show.get());
        if (a <= 0.01f) return;

        float prev = Render2D.alpha();
        Render2D.setAlpha(prev * a);
        float cx = x + w / 2f, cy = y + h / 2f, s = 0.92f + 0.08f * a;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(s, s);
        ctx.getMatrices().translate(-cx, -cy);

        HudDraw.glass(ctx, x, y, w, h, 8f);

        float hx = x + PAD, hy = y + PAD;
        float hurtT = hurt.to(target.hurtTime > 0).get();
        Render2D.shadow(ctx, hx, hy, HEAD, HEAD, 6f, 8f, Colors.lerp(Theme.accent(0.30f), 0x80FF3B3B, hurtT));
        Render2D.rect(ctx, hx, hy, HEAD, HEAD, 6f, 0x30000000);
        HudDraw.head(ctx, target, hx + 2f, hy + 2f, HEAD - 4f);
        if (hurtT > 0.01f) Render2D.rect(ctx, hx + 2f, hy + 2f, HEAD - 4f, HEAD - 4f, 2f, Colors.withAlpha(0xFFFF3030, 0.45f * hurtT));
        Render2D.outline(ctx, hx, hy, HEAD, HEAD, 6f, 0.8f, Theme.accent(0.5f));

        com.lowcode.svintusclient.client.module.impl.render.TargetHudModule thm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.TargetHudModule.class);
        if (thm != null && thm.hitSparks.get() && hurtT > 0.1f) {
            float sparkTime = (System.currentTimeMillis() % 1000L) / 1000.0f;
            for (int sp = 0; sp < 4; sp++) {
                float sa = (float) (sp * Math.PI / 2.0 + sparkTime * Math.PI);
                float sx = hx + HEAD / 2f + (float) Math.cos(sa) * (HEAD / 2f + 3f * hurtT);
                float sy = hy + HEAD / 2f + (float) Math.sin(sa) * (HEAD / 2f + 3f * hurtT);
                Render2D.rect(ctx, sx - 1f, sy - 1f, 2f, 2f, 1f, 0xFFFFAA00);
            }
        }

        float tx = hx + HEAD + 7f, right = x + w - PAD, rowW = right - tx;
        String name = target.getName().getString();
        String dist = "";
        MinecraftClient client = MinecraftClient.getInstance();
        boolean useDist = thm != null ? thm.showDistance.get() : distance.get();
        boolean useArmor = thm != null ? thm.showArmor.get() : items.get();

        if (useDist && client.player != null && target != client.player) {
            dist = String.format(Locale.ROOT, "%.1fm", client.player.distanceTo(target));
        }
        float distW = dist.isEmpty() ? 0f : Fonts.REGULAR.width(dist, 5.6f) + 4f;
        Fonts.SEMIBOLD.drawCentered(ctx, Fonts.SEMIBOLD.trim(name, 7.2f, rowW - distW), tx, hy + 4.5f, 7.2f, Theme.TEXT);
        if (!dist.isEmpty()) Fonts.REGULAR.drawCentered(ctx, dist, right - distW + 4f, hy + 4.5f, 5.6f, Theme.TEXT_MUTE);

        float max = Math.max(1f, target.getMaxHealth());
        float targetHpRatio = Math.min(1f, target.getHealth() / max);
        boolean useSmooth = thm == null || thm.smoothHp.get();
        float hp = useSmooth ? health.to(targetHpRatio).get() : targetHpRatio;
        if (!useSmooth) health.snap(targetHpRatio);

        float absTarget = Math.min(1f, target.getAbsorptionAmount() / max);
        float abs = absorption.to(absTarget).get();
        float lag = damageLagAnim.to(targetHpRatio).get();

        float barY = hy + 11.5f, barH = 3.5f;
        Render2D.rect(ctx, tx, barY, rowW - 26f, barH, barH / 2f, 0x30FFFFFF);
        float bw = rowW - 26f;

        // Damage Lag ghost bar
        if (thm != null && thm.damageLag.get() && lag > hp) {
            Render2D.rect(ctx, tx, barY, Math.max(barH, bw * lag), barH, barH / 2f, 0x90FF4757);
        }

        int colLeft = Theme.accent2();
        int colRight = Theme.accent();
        if (thm != null) {
            String cm = thm.colorMode.get();
            if ("Градиент здоровья".equals(cm)) {
                colLeft = Colors.lerp(0xFFFF3838, 0xFF2ED573, hp);
                colRight = Colors.lerp(0xFFFF5252, 0xFF7BED9F, hp);
            } else if ("Радуга".equals(cm)) {
                float hue = (System.currentTimeMillis() % 3000L) / 3000.0f;
                colLeft = java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
                colRight = java.awt.Color.HSBtoRGB((hue + 0.15f) % 1.0f, 0.8f, 1.0f);
            } else if ("Пользовательский".equals(cm)) {
                colLeft = colRight = thm.customColor.get();
            }
        }

        if (hp > 0.001f) {
            if (neonGlow.get()) {
                Render2D.shadow(ctx, tx, barY, bw * hp, barH, barH / 2f, 6f, Theme.accent(0.5f));
            }
            Render2D.gradientH(ctx, tx, barY, Math.max(barH, bw * hp), barH, barH / 2f, colLeft, colRight);
        }

        boolean showAbsorption = thm == null || thm.absorption.get();
        if (showAbsorption && abs > 0.001f) {
            Render2D.rect(ctx, tx, barY, Math.max(barH, bw * abs), barH, barH / 2f, 0xD0F5C542);
        }

        String hpText = percentHp.get()
                ? String.format(Locale.ROOT, "%d%%", Math.round(hp * 100f))
                : String.format(Locale.ROOT, "%.1f", target.getHealth() + target.getAbsorptionAmount());
        Icons.get("heart").drawCentered(ctx, right - 22f, barY + barH / 2f, 6f, 0xFFFF5A6A);
        Fonts.SEMIBOLD.drawCentered(ctx, hpText, right - 17.5f, barY + barH / 2f, 5.8f, Theme.TEXT);

        if (useArmor) {
            float iy = hy + 18f, size = 10f, step = 13f;
            float ix = tx;
            ix = slot(ctx, target.getMainHandStack(), ix, iy, size) + step;
            ix = slot(ctx, target.getOffHandStack(), ix, iy, size) + step + 3f;
            for (EquipmentSlot slot : ARMOR) {
                ix = slot(ctx, target.getEquippedStack(slot), ix, iy, size) + step;
            }
        }

        ctx.getMatrices().popMatrix();
        Render2D.setAlpha(prev);
    }

    private static float slot(DrawContext ctx, ItemStack stack, float x, float y, float size) {
        Render2D.rect(ctx, x - 1f, y - 1f, size + 2f, size + 2f, 2.5f, 0x14FFFFFF);
        HudDraw.item(ctx, stack, x, y, size);
        HudDraw.durability(ctx, stack, x, y + size + 1.2f, size);
        return x;
    }
}
