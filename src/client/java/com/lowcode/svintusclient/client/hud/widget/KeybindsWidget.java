package com.lowcode.svintusclient.client.hud.widget;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.hud.HudWidget;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class KeybindsWidget extends HudWidget {

    private static final float SIZE = 6.0f, LINE_H = 13f, PAD = 6f;

    private final BooleanSetting onlyEnabled = option("Только включённые", true);

    public record KeyEntry(String keyName, String moduleName, boolean enabled) {}
    private final List<KeyEntry> entries = new ArrayList<>();

    public KeybindsWidget() {
        super("keybinds", "Бинды клавиш", 0f, 0.6f);
    }

    @Override
    public boolean isVisible() {
        com.lowcode.svintusclient.client.module.impl.render.KeybindsHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeybindsHudModule.class);
        if (mod != null) return mod.isEnabled();
        return ModuleManager.HUD != null && ModuleManager.HUD.keybinds.get();
    }

    @Override
    public void setVisible(boolean visible) {
        com.lowcode.svintusclient.client.module.impl.render.KeybindsHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeybindsHudModule.class);
        if (mod != null) mod.setEnabled(visible);
        if (ModuleManager.HUD != null) ModuleManager.HUD.keybinds.set(visible);
    }

    @Override
    protected void layout(boolean editing) {
        entries.clear();
        com.lowcode.svintusclient.client.module.impl.render.KeybindsHudModule khm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeybindsHudModule.class);
        boolean useOnlyEn = khm != null ? khm.onlyActive.get() : onlyEnabled.get();

        for (Module m : ModuleManager.all()) {
            if (m.getKey() > 0) {
                if (useOnlyEn && !m.isEnabled()) continue;
                String kName = getKeyName(m.getKey());
                entries.add(new KeyEntry(kName, m.getName(), m.isEnabled()));
            }
        }

        if (entries.isEmpty() && editing) {
            entries.add(new KeyEntry("R", "Trails", true));
            entries.add(new KeyEntry("G", "Jump Circles", true));
            entries.add(new KeyEntry("V", "Crosshair", false));
        }

        float maxW = 75f;
        for (KeyEntry ke : entries) {
            float lineW = PAD * 2f + Fonts.SEMIBOLD.width("[" + ke.keyName + "]", SIZE) + 8f + Fonts.REGULAR.width(ke.moduleName, SIZE) + 20f;
            if (lineW > maxW) maxW = lineW;
        }

        w = maxW;
        h = Math.max(LINE_H + PAD, entries.size() * LINE_H + PAD + 12f);
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        if (entries.isEmpty() && !editing) return;

        com.lowcode.svintusclient.client.module.impl.render.KeybindsHudModule khm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.KeybindsHudModule.class);
        String st = khm != null ? khm.style.get() : "Стекло";
        String cMode = khm != null ? khm.colorMode.get() : "Акцент темы";

        int accColor;
        if ("Радуга".equals(cMode)) {
            float hue = (System.currentTimeMillis() % 3500L) / 3500.0f;
            accColor = java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
        } else if ("Свой цвет".equals(cMode) && khm != null) {
            accColor = khm.customColor.getEffectiveColor();
        } else {
            accColor = Theme.accent();
        }

        if ("Контур".equals(st)) {
            HudDraw.glass(ctx, x, y, w, h, 6f);
            Render2D.outline(ctx, x, y, w, h, 6f, 1.2f, accColor);
        } else if ("Минимал".equals(st)) {
            Render2D.rect(ctx, x, y, w, h, 4f, 0xAA111115);
        } else {
            HudDraw.glass(ctx, x, y, w, h, 6f);
        }

        // Header
        float curY = y + 4f;
        Render2D.rect(ctx, x + PAD, curY + 2f, 3f, 7f, 1.5f, 0xFFE056FD);
        Fonts.SEMIBOLD.draw(ctx, "Бинды", x + PAD + 6f, curY + 1f, SIZE + 0.4f, Theme.TEXT);
        curY += 13f;

        for (KeyEntry ke : entries) {
            // Key badge
            String kTag = "[" + ke.keyName + "]";
            Fonts.SEMIBOLD.draw(ctx, kTag, x + PAD, curY + 2f, SIZE, accColor);

            float tagW = Fonts.SEMIBOLD.width(kTag, SIZE);
            Fonts.REGULAR.draw(ctx, ke.moduleName, x + PAD + tagW + 5f, curY + 2f, SIZE, Theme.TEXT);

            // State tag [ВКЛ / ВЫКЛ]
            String stateTag = ke.enabled ? "ВКЛ" : "ВЫКЛ";
            int stateColor = ke.enabled ? 0xFF2ED573 : 0xFFFF4757;
            float stateW = Fonts.SEMIBOLD.width(stateTag, SIZE - 0.5f);
            Fonts.SEMIBOLD.draw(ctx, stateTag, x + w - PAD - stateW, curY + 2.5f, SIZE - 0.5f, stateColor);

            curY += LINE_H;
        }
    }

    private static String getKeyName(int key) {
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null && !name.isEmpty()) {
            return name.toUpperCase();
        }
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            default -> "KEY_" + key;
        };
    }
}
