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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KeyStrokesWidget extends HudWidget {

    private static final float KEY_SIZE = 22f;
    private static final float GAP = 2.5f;
    private static final float MOUSE_H = 16f;
    private static final float SPACE_H = 14f;

    private final BooleanSetting showMouse = option("Кнопки мыши (LMB / RMB)", true);
    private final BooleanSetting showCps = option("Счётчик CPS", true);
    private final BooleanSetting showSpace = option("Пробел (Jump)", true);

    private final List<Long> leftClicks = new ArrayList<>();
    private final List<Long> rightClicks = new ArrayList<>();
    private boolean lastLeftPressed = false;
    private boolean lastRightPressed = false;

    public KeyStrokesWidget() {
        super("keystrokes", "Кейстроуки", 0.05f, 0.5f);
    }

    @Override
    public boolean isVisible() {
        com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule.class);
        if (mod != null) return mod.isEnabled();
        return ModuleManager.HUD != null && ModuleManager.HUD.keystrokes.get();
    }

    @Override
    public void setVisible(boolean visible) {
        com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule.class);
        if (mod != null) mod.setEnabled(visible);
        if (ModuleManager.HUD != null) ModuleManager.HUD.keystrokes.set(visible);
    }

    @Override
    protected void layout(boolean editing) {
        com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule.class);
        boolean mouse = mod != null ? (mod.layout.get().contains("Мышь") || mod.layout.get().contains("Полный")) : showMouse.get();
        boolean space = mod != null ? mod.showSpace.get() : showSpace.get();

        float totalW = KEY_SIZE * 3f + GAP * 2f;
        float totalH = KEY_SIZE * 2f + GAP;

        if (mouse) totalH += GAP + MOUSE_H;
        if (space) totalH += GAP + SPACE_H;

        w = totalW;
        h = totalH;

        // Track clicks
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            boolean left = mc.options.attackKey.isPressed();
            boolean right = mc.options.useKey.isPressed();
            long now = System.currentTimeMillis();

            if (left && !lastLeftPressed) leftClicks.add(now);
            if (right && !lastRightPressed) rightClicks.add(now);

            lastLeftPressed = left;
            lastRightPressed = right;

            pruneClicks(leftClicks, now);
            pruneClicks(rightClicks, now);
        }
    }

    private void pruneClicks(List<Long> clicks, long now) {
        Iterator<Long> it = clicks.iterator();
        while (it.hasNext()) {
            if (now - it.next() > 1000L) {
                it.remove();
            }
        }
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options == null) return;

        com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule.class);
        boolean useCps = mod != null ? mod.showCps.get() : showCps.get();
        String cMode = mod != null ? mod.colorMode.get() : "Акцент темы";

        int accColor;
        if ("Радуга".equals(cMode)) {
            float hue = (System.currentTimeMillis() % 3500L) / 3500.0f;
            accColor = java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
        } else if ("Свой цвет".equals(cMode) && mod != null) {
            accColor = mod.customColor.getEffectiveColor();
        } else {
            accColor = Theme.accent();
        }

        boolean wDown = mc.options.forwardKey.isPressed();
        boolean aDown = mc.options.leftKey.isPressed();
        boolean sDown = mc.options.backKey.isPressed();
        boolean dDown = mc.options.rightKey.isPressed();
        boolean lmbDown = mc.options.attackKey.isPressed();
        boolean rmbDown = mc.options.useKey.isPressed();
        boolean spaceDown = mc.options.jumpKey.isPressed();

        float curY = y;

        // Row 1: W key centered
        float wX = x + KEY_SIZE + GAP;
        drawSingleKey(ctx, wX, curY, KEY_SIZE, KEY_SIZE, "W", wDown);

        // Row 2: A, S, D
        curY += KEY_SIZE + GAP;
        drawSingleKey(ctx, x, curY, KEY_SIZE, KEY_SIZE, "A", aDown);
        drawSingleKey(ctx, x + KEY_SIZE + GAP, curY, KEY_SIZE, KEY_SIZE, "S", sDown);
        drawSingleKey(ctx, x + (KEY_SIZE + GAP) * 2f, curY, KEY_SIZE, KEY_SIZE, "D", dDown);

        // Row 3: LMB & RMB
        if (showMouse.get()) {
            curY += KEY_SIZE + GAP;
            float mouseW = (w - GAP) * 0.5f;

            String lmbText = "LMB";
            if (useCps) {
                lmbText = (editing ? "12" : leftClicks.size()) + " CPS";
            }
            drawSingleKey(ctx, x, curY, mouseW, MOUSE_H, lmbText, lmbDown);

            String rmbText = "RMB";
            if (useCps) {
                rmbText = (editing ? "10" : rightClicks.size()) + " CPS";
            }
            drawSingleKey(ctx, x + mouseW + GAP, curY, mouseW, MOUSE_H, rmbText, rmbDown);
        }

        // Row 4: Space bar
        if (showSpace.get()) {
            curY += (showMouse.get() ? MOUSE_H : KEY_SIZE) + GAP;
            drawSingleKey(ctx, x, curY, w, SPACE_H, "—————", spaceDown);
        }
    }

    private void drawSingleKey(DrawContext ctx, float kx, float ky, float kw, float kh, String text, boolean pressed) {
        com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeyStrokesHudModule.class);
        String st = mod != null ? mod.style.get() : "Стекло";
        String cMode = mod != null ? mod.colorMode.get() : "Акцент темы";

        int accColor;
        if ("Радуга".equals(cMode)) {
            float hue = (System.currentTimeMillis() % 3500L) / 3500.0f;
            accColor = java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
        } else if ("Свой цвет".equals(cMode) && mod != null) {
            accColor = mod.customColor.getEffectiveColor();
        } else {
            accColor = Theme.accent();
        }

        if ("Контур".equals(st)) {
            HudDraw.glass(ctx, kx, ky, kw, kh, 4f);
            Render2D.outline(ctx, kx, ky, kw, kh, 4f, 1.0f, (0x44 << 24) | (accColor & 0x00FFFFFF));
        } else if ("Неон".equals(st)) {
            HudDraw.glass(ctx, kx, ky, kw, kh, 4f);
            Render2D.outline(ctx, kx, ky, kw, kh, 4f, 1.5f, accColor);
        } else {
            HudDraw.glass(ctx, kx, ky, kw, kh, 4f);
        }

        if (pressed) {
            int fill = (accColor & 0x00FFFFFF) | 0x4D000000;
            Render2D.rect(ctx, kx, ky, kw, kh, 4f, fill);
            Render2D.outline(ctx, kx, ky, kw, kh, 4f, 1.2f, accColor);
        }

        int textColor = pressed ? accColor : Theme.TEXT;
        float cy = ky + kh * 0.5f;
        Fonts.SEMIBOLD.drawCentered(ctx, text, kx + kw * 0.5f, cy, 6.5f, textColor);
    }
}
