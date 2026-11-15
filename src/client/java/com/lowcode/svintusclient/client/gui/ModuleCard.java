package com.lowcode.svintusclient.client.gui;

import com.lowcode.svintusclient.client.gui.component.BindComponent;
import com.lowcode.svintusclient.client.gui.component.BooleanComponent;
import com.lowcode.svintusclient.client.gui.component.ColorComponent;
import com.lowcode.svintusclient.client.gui.component.ModeComponent;
import com.lowcode.svintusclient.client.gui.component.SettingComponent;
import com.lowcode.svintusclient.client.gui.component.SliderComponent;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BindSetting;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.module.setting.Setting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.render.icon.Icons;
import com.lowcode.svintusclient.client.util.Anim;
import com.lowcode.svintusclient.client.util.Colors;
import com.lowcode.svintusclient.client.util.I18n;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleCard {

    public static final float HEADER_H = 32f;
    private static final float RADIUS = 10f;
    private static final float PAD = 10f;
    private static final float INSET = 6f;
    private static final float ROW_GAP = 4f;

    private record Row(Setting<?> setting, SettingComponent component) {
    }

    final Module module;
    private final List<Row> rows = new ArrayList<>();
    private final Anim enabled;
    private final Anim hover = new Anim(14f);
    private final Anim expand = new Anim(12f);
    private final Anim chevronHover = new Anim(16f);
    private boolean expanded;

    private float helpX, helpY, helpW = 9f, helpH = 9f;

    public float x, y, w, h;

    public ModuleCard(Module module) {
        this.module = module;
        this.enabled = new Anim(module.isEnabled() ? 1f : 0f, 14f);
        for (Setting<?> setting : module.getSettings()) {
            SettingComponent component = switch (setting) {
                case BooleanSetting b -> new BooleanComponent(b);
                case NumberSetting n -> new SliderComponent(n);
                case ModeSetting m -> new ModeComponent(m);
                case ColorSetting c -> new ColorComponent(c);
                case BindSetting b -> new BindComponent(b.getName(), b::get, b::set);
                default -> null;
            };
            if (component != null) rows.add(new Row(setting, component));
        }
        if (module.isToggleable()) rows.add(new Row(null, new BindComponent(I18n.get("Бинд"), module::getKey, module::setKey)));
    }

    public boolean hasSettings() {
        return !rows.isEmpty();
    }

    private List<Row> visibleRows() {
        List<Row> list = new ArrayList<>(rows.size());
        for (Row row : rows) if (row.setting == null || row.setting.isVisible()) list.add(row);
        return list;
    }

    private float rowsHeight() {
        float total = 0;
        for (Row row : visibleRows()) total += row.component.height() + ROW_GAP;
        return total;
    }

    private float expandProgress() {
        return Anim.easeOutCubic(expand.to(expanded).get());
    }

    public float height() {
        float e = expandProgress();
        return e <= 0.001f ? HEADER_H : HEADER_H + (rowsHeight() + INSET - ROW_GAP + 2f) * e;
    }

    private float switchX() {
        return x + w - PAD - Widgets.SWITCH_W;
    }

    private float chevronX() {
        return module.isToggleable() ? switchX() - 12f : x + w - PAD - 8f;
    }

    private boolean overChevron(float mx, float my) {
        return hasSettings() && Widgets.inside(mx, my, chevronX() - 6f, y + 4f, 20f, HEADER_H - 8f);
    }

    public boolean isOverHeader(float mx, float my) {
        return Widgets.inside(mx, my, x, y, w, HEADER_H);
    }

    public void render(DrawContext ctx, float x, float y, float w, float mx, float my, boolean interactive) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = height();

        float e = enabled.to(module.isEnabled()).get();
        float hv = hover.to(interactive && isOverHeader(mx, my)).get();
        float ex = expandProgress();

        float rad = Theme.cardRadius();
        float bw = Theme.borderWidth();
        int bg = Colors.lerp(Theme.cardBg(), Theme.cardHoverBg(), Math.max(hv, ex * 0.4f));

        if (e > 0.01f && Theme.hasGlow()) Render2D.shadow(ctx, x, y, w, h, rad, 10f, Theme.accent(0.12f * e));
        Render2D.rect(ctx, x, y, w, h, rad, bg);
        if (e > 0.01f) {
            Render2D.gradientH(ctx, x, y, w, h, rad, Theme.accent(0.13f * e), Theme.accent(0.02f * e));
            Render2D.outlineGradientH(ctx, x, y, w, h, rad, bw,
                    Colors.withAlpha(Theme.accent(), 0.75f * e), Colors.withAlpha(Theme.accent2(), 0.30f * e));
        } else {
            Render2D.outlineGradientV(ctx, x, y, w, h, rad, bw * 0.8f, 0x14FFFFFF, 0x06FFFFFF);
        }

        float right = (hasSettings() ? chevronX() : module.isToggleable() ? switchX() : x + w - PAD) - 6f;
        if (module.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
            String key = BindComponent.keyName(module.getKey());
            float kw = Fonts.REGULAR.width(key, 5.4f) + 8f;
            right -= kw;
            Render2D.rect(ctx, right, y + 7f, kw, 9f, 3f, 0x14FFFFFF);
            Fonts.REGULAR.drawCentered(ctx, key, right + 4f, y + 11.5f, 5.4f, Theme.TEXT_DIM);
            right -= 4f;
        }

        float nameMaxW = Math.max(20f, right - (x + PAD) - 15f);
        String name = Fonts.SEMIBOLD.trim(I18n.get(module.getName()), 7.4f, nameMaxW);
        Fonts.SEMIBOLD.drawCentered(ctx, name, x + PAD, y + 11.5f, 7.4f,
                Colors.lerp(Theme.TEXT_DIM, Theme.TEXT, Math.max(e, Math.max(hv * 0.6f, module.isToggleable() ? 0f : 0.8f))));

        float nw = Fonts.SEMIBOLD.width(name, 7.4f);
        this.helpX = Math.min(x + PAD + nw + 5f, right - helpW - 2f);
        this.helpY = y + 7f;
        boolean qHover = interactive && isOverHelp(mx, my);
        Render2D.rect(ctx, helpX, helpY, helpW, helpH, 4.5f, qHover ? Theme.accent(0.25f) : 0x14FFFFFF);
        Render2D.outline(ctx, helpX, helpY, helpW, helpH, 4.5f, 0.7f, qHover ? Theme.accent() : 0x24FFFFFF);
        Fonts.REGULAR.drawCentered(ctx, "?", helpX + 2.5f, helpY + 4.5f, 5.0f, qHover ? Theme.accent() : Theme.TEXT_DIM);

        String desc = Fonts.REGULAR.trim(I18n.get(module.getDescription()), 5.8f, nameMaxW);
        Fonts.REGULAR.drawCentered(ctx, desc, x + PAD, y + 22f, 5.8f, Colors.lerp(Theme.TEXT_MUTE, Theme.TEXT_DIM, Math.max(e, hv * 0.5f)));

        if (module.isToggleable()) Widgets.toggle(ctx, switchX(), y + (HEADER_H - Widgets.SWITCH_H) / 2f, e, hv);

        if (hasSettings()) {
            boolean chOver = interactive && overChevron(mx, my);
            float ch = chevronHover.to(chOver).get();
            float cx = chevronX() + 4f, cy = y + HEADER_H / 2f;
            if (ch > 0.01f || ex > 0.01f) {
                Render2D.rect(ctx, cx - 6.5f, cy - 6.5f, 13f, 13f, 3.5f,
                        Colors.lerp(0x12FFFFFF, Theme.accent(0.22f), Math.max(ch, ex)));
                Render2D.outline(ctx, cx - 6.5f, cy - 6.5f, 13f, 13f, 3.5f, 0.6f,
                        Colors.lerp(0x20FFFFFF, Theme.accent(0.6f), Math.max(ch, ex)));
            }
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(cx, cy);
            ctx.getMatrices().rotate((float) Math.PI * ex);
            Icons.CHEVRON.drawCentered(ctx, 0, 0, 7.5f, Colors.lerp(Colors.lerp(Theme.TEXT_MUTE, Theme.TEXT, ch), Theme.accent(), ex));
            ctx.getMatrices().popMatrix();
        }

        if (ex > 0.001f) {
            float prev = Render2D.alpha();
            Render2D.setAlpha(prev * ex);
            Render2D.rect(ctx, x + PAD, y + HEADER_H - 0.5f, w - PAD * 2f, 0.7f, 0.35f, 0x10FFFFFF);
            Render2D.pushScissor(ctx, x, y + HEADER_H, w, h - HEADER_H);
            boolean rowsInteractive = interactive && expanded && ex > 0.9f;
            float rmx = rowsInteractive ? mx : -1e4f, rmy = rowsInteractive ? my : -1e4f;
            float yy = y + HEADER_H + INSET - 2f;
            for (Row row : visibleRows()) {
                row.component.setBounds(x + INSET, yy, w - INSET * 2f);
                row.component.render(ctx, rmx, rmy);
                yy += row.component.height() + ROW_GAP;
            }
            Render2D.popScissor(ctx);
            Render2D.setAlpha(prev);
        }
    }

    public void renderOverlays(DrawContext ctx, float mx, float my) {
        if (!expanded) return;
        for (Row row : visibleRows()) row.component.renderOverlay(ctx, mx, my);
    }

    public boolean hasOverlay() {
        for (Row row : rows) if (row.component.hasOverlay()) return true;
        return false;
    }

    public boolean overlayClicked(float mx, float my, int button) {
        boolean consumed = false;
        for (Row row : rows) {
            if (row.component.hasOverlay() && row.component.overlayClicked(mx, my, button)) consumed = true;
        }
        return consumed;
    }

    public boolean isPointerOver(float mx, float my) {
        if (isOverHelp(mx, my) || overChevron(mx, my)) return true;
        if (isOverHeader(mx, my)) return true;
        if (!expanded) return false;
        for (Row row : visibleRows()) if (row.component.isHovered(mx, my)) return true;
        return false;
    }

    public boolean isOverHelp(float mx, float my) {
        return Widgets.inside(mx, my, helpX - 2f, helpY - 2f, helpW + 4f, helpH + 4f);
    }

    public void renderHelpTooltip(DrawContext ctx, float screenW, float screenH, float mx, float my) {
        if (!isOverHelp(mx, my)) return;
        com.lowcode.svintusclient.client.util.ModuleHelp.HelpInfo info = com.lowcode.svintusclient.client.util.ModuleHelp.get(module);
        if (info == null) return;

        float tw = 210f;
        float pad = 8f;
        float textW = tw - pad * 2f;

        List<String> whatLines = Fonts.REGULAR.wrap(info.whatItDoes(), 5.6f, textW);
        List<String> lookLines = Fonts.REGULAR.wrap(info.howItLooks(), 5.6f, textW);
        List<String> condLines = Fonts.REGULAR.wrap(info.conditions(), 5.6f, textW);

        float lineH = 7.5f;
        float titleH = 14f;
        float secH = 10f;
        float th = pad * 2f + titleH + (secH + whatLines.size() * lineH) + (secH + lookLines.size() * lineH) + (secH + condLines.size() * lineH) + 6f;

        float tx = Math.max(8f, Math.min(screenW - tw - 8f, mx + 12f));
        float ty = Math.max(8f, Math.min(screenH - th - 8f, my + 8f));

        Render2D.shadow(ctx, tx, ty, tw, th, 8f, 14f, 0xEE000000);
        Render2D.rect(ctx, tx, ty, tw, th, 8f, 0xF516171E);
        Render2D.outline(ctx, tx, ty, tw, th, 8f, 1f, Theme.accent(0.65f));

        float curY = ty + pad;
        Fonts.SEMIBOLD.drawCentered(ctx, info.title(), tx + pad, curY + 5f, 7.2f, Theme.accent());
        curY += titleH;

        Fonts.SEMIBOLD.drawCentered(ctx, "Что делает:", tx + pad, curY + 4f, 5.8f, Theme.TEXT);
        curY += secH;
        for (String line : whatLines) {
            Fonts.REGULAR.drawCentered(ctx, line, tx + pad, curY + 3f, 5.5f, Theme.TEXT_DIM);
            curY += lineH;
        }

        Fonts.SEMIBOLD.drawCentered(ctx, "Как выглядит эффект:", tx + pad, curY + 4f, 5.8f, Theme.TEXT);
        curY += secH;
        for (String line : lookLines) {
            Fonts.REGULAR.drawCentered(ctx, line, tx + pad, curY + 3f, 5.5f, Theme.TEXT_DIM);
            curY += lineH;
        }

        Fonts.SEMIBOLD.drawCentered(ctx, "Ограничения / условия:", tx + pad, curY + 4f, 5.8f, Theme.TEXT);
        curY += secH;
        for (String line : condLines) {
            Fonts.REGULAR.drawCentered(ctx, line, tx + pad, curY + 3f, 5.5f, Theme.TEXT_MUTE);
            curY += lineH;
        }
    }

    public boolean mouseClicked(float mx, float my, int button) {
        if (isOverHelp(mx, my)) {
            return true;
        }
        if (isOverHeader(mx, my)) {
            boolean expandClick = button == 1 || (button == 0 && (overChevron(mx, my) || !module.isToggleable()));
            if (expandClick && hasSettings()) expanded = !expanded;
            else if (button == 0) module.toggle();
            return true;
        }
        if (expanded && Widgets.inside(mx, my, x, y, w, h)) {
            for (Row row : visibleRows()) {
                if (row.component.mouseClicked(mx, my, button)) {
                    return true;
                }
            }
            return true;
        }
        for (Row row : rows) row.component.mouseClicked(-1e4f, -1e4f, button);
        return false;
    }

    public void mouseReleased() {
        for (Row row : rows) row.component.mouseReleased();
    }

    public void mouseReleased(float mx, float my, int button) {
        mouseReleased();
    }

    public boolean mouseScrolled(float mx, float my, float amount) {
        return false;
    }

    public boolean keyPressed(int key) {
        for (Row row : rows) if (row.component.keyPressed(key)) return true;
        return false;
    }
}
