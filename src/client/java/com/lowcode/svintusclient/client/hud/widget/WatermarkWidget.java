package com.lowcode.svintusclient.client.hud.widget;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.hud.HudWidget;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.render.PigHead;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.render.icon.Icon;
import com.lowcode.svintusclient.client.render.icon.Icons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class WatermarkWidget extends HudWidget {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final float H = 20f, TITLE = 7.4f, VALUE = 6.4f, UNIT = 5.6f;

    private final BooleanSetting nick = option("Ник", true);
    private final BooleanSetting fps = option("FPS", true);
    private final BooleanSetting ping = option("Пинг", true);
    private final BooleanSetting tps = option("TPS", false);
    private final BooleanSetting ram = option("ОЗУ (RAM)", false);
    private final BooleanSetting time = option("Время", true);

    private record Segment(Icon icon, String value, String unit) {
    }

    private final List<Segment> segments = new ArrayList<>();

    public WatermarkWidget() {
        super("watermark", "Ватермарка", 0f, 0f);
    }

    @Override
    public boolean isVisible() {
        com.lowcode.svintusclient.client.module.impl.render.WatermarkHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.WatermarkHudModule.class);
        if (mod != null) return mod.isEnabled();
        return ModuleManager.HUD != null && ModuleManager.HUD.watermark.get();
    }

    @Override
    public void setVisible(boolean visible) {
        com.lowcode.svintusclient.client.module.impl.render.WatermarkHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.WatermarkHudModule.class);
        if (mod != null) mod.setEnabled(visible);
        if (ModuleManager.HUD != null) ModuleManager.HUD.watermark.set(visible);
    }

    @Override
    protected void layout(boolean editing) {
        MinecraftClient client = MinecraftClient.getInstance();
        com.lowcode.svintusclient.client.module.impl.render.WatermarkHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.WatermarkHudModule.class);
        boolean showNick = mod != null ? mod.nick.get() : nick.get();
        boolean showFps = mod != null ? mod.fps.get() : fps.get();
        boolean showPing = mod != null ? mod.ping.get() : ping.get();
        boolean showTps = mod != null ? mod.tps.get() : tps.get();
        boolean showRam = mod != null ? mod.ram.get() : ram.get();
        boolean showTime = mod != null ? mod.time.get() : time.get();

        segments.clear();
        if (showNick) {
            String real = client.getSession() != null ? client.getSession().getUsername() : "Player";
            String name = real;
            com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule sm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule.class);
            if (sm != null && sm.isEnabled()) {
                name = sm.filterText(name, real);
            }
            segments.add(new Segment(Icons.get("player"), name, ""));
        }
        if (showFps) segments.add(new Segment(Icons.get("activity"), String.valueOf(client.getCurrentFps()), " fps"));
        if (showPing) segments.add(new Segment(Icons.get("signal"), String.valueOf(ping(client)), " ms"));
        if (showTps) segments.add(new Segment(Icons.get("activity"), "20.0", " tps"));
        if (showRam) {
            long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long usedMem = totalMem - freeMem;
            segments.add(new Segment(Icons.get("settings"), String.valueOf(usedMem), " MB"));
        }
        if (showTime) segments.add(new Segment(Icons.get("clock"), LocalTime.now().format(TIME), ""));

        if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            String srv = client.getCurrentServerEntry().address;
            com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule smm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule.class);
            if (smm != null && smm.isEnabled()) {
                srv = smm.filterServerIp(srv);
            }
        }

        float width = 6f + 13f + 5f + Fonts.SEMIBOLD.width("Doxaiz", TITLE) + 7f;
        for (Segment s : segments) {
            width += 8f + 7f + 3.5f + Fonts.SEMIBOLD.width(s.value, VALUE) + Fonts.REGULAR.width(s.unit, UNIT);
        }
        w = width;
        h = H;
    }

    private static int ping(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null) return 0;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        com.lowcode.svintusclient.client.module.impl.render.WatermarkHudModule mod = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.WatermarkHudModule.class);
        String st = mod != null ? mod.style.get() : "Стильный чип";
        String cMode = mod != null ? mod.colorMode.get() : "Акцент темы";

        int accentCol;
        if ("Неон".equals(cMode)) {
            accentCol = 0xFF05D9E8;
        } else if ("Хрома".equals(cMode)) {
            float hue = (System.currentTimeMillis() % 3500L) / 3500.0f;
            accentCol = java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
        } else if ("Свой цвет".equals(cMode) && mod != null) {
            accentCol = mod.customColor.getEffectiveColor();
        } else {
            accentCol = Theme.accent();
        }

        float cy = y + h / 2f;

        if ("Классический текст".equals(st)) {
            float tx = x + 2f;
            tx += Fonts.SEMIBOLD.drawGradient(ctx, "Doxaiz", tx, cy, TITLE, Theme.accent2(), accentCol) + 3f;
            for (Segment s : segments) {
                tx += 4f;
                tx += Fonts.SEMIBOLD.drawCentered(ctx, s.value, tx, cy, VALUE, Theme.TEXT);
                if (!s.unit.isEmpty()) tx += Fonts.REGULAR.drawCentered(ctx, s.unit, tx, cy, UNIT, Theme.TEXT_DIM);
            }
            return;
        }

        HudDraw.glass(ctx, x, y, w, h, 7f);
        if ("Неоновый бейдж".equals(st)) {
            Render2D.outline(ctx, x, y, w, h, 7f, 1.2f, accentCol);
        }
        Render2D.gradientH(ctx, x, y, 40f, h, 7f, (0x28 << 24) | (accentCol & 0x00FFFFFF), accentCol & 0x00FFFFFF);

        Render2D.shadow(ctx, x + 6f, y + 3.5f, 13f, 13f, 3f, 7f, (0x70 << 24) | (accentCol & 0x00FFFFFF));
        PigHead.draw(ctx, x + 6f, y + 3.5f, 13f);

        float tx = x + 24f;
        tx += Fonts.SEMIBOLD.drawGradient(ctx, "Doxaiz", tx, cy, TITLE, Theme.accent2(), accentCol) + 3f;

        for (Segment s : segments) {
            tx += 4f;
            Render2D.rect(ctx, tx, y + 6f, 0.8f, h - 12f, 0.4f, 0x1EFFFFFF);
            tx += 4f;
            s.icon.drawCentered(ctx, tx + 3.5f, cy, 7f, accentCol);
            tx += 7f + 3.5f;
            tx += Fonts.SEMIBOLD.drawCentered(ctx, s.value, tx, cy, VALUE, Theme.TEXT);
            if (!s.unit.isEmpty()) tx += Fonts.REGULAR.drawCentered(ctx, s.unit, tx, cy, UNIT, Theme.TEXT_DIM);
        }
    }
}
