package com.lowcode.svintusclient.client.hud.widget;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.hud.HudWidget;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.impl.render.FPSPlusModule;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class FPSPlusWidget extends HudWidget {

    public FPSPlusWidget() {
        super("fps_plus", "FPS+ Телеметрия", 0.01f, 0.08f);
    }

    private FPSPlusModule mod() {
        return ModuleManager.get(FPSPlusModule.class);
    }

    @Override
    public boolean isVisible() {
        FPSPlusModule m = mod();
        return m != null && m.isEnabled();
    }

    @Override
    public void setVisible(boolean visible) {
        FPSPlusModule m = mod();
        if (m != null) m.setEnabled(visible);
    }

    @Override
    protected void layout(boolean editing) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int fps = editing ? 144 : mc.getCurrentFps();
        String str = "FPS: " + fps + "   TPS: 20.0   PING: 15ms";
        this.w = Fonts.SEMIBOLD.width(str, 5.8f) + 14f;
        this.h = 16f;
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        FPSPlusModule m = mod();
        if (m == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        int fps = editing ? 144 : mc.getCurrentFps();
        long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        int memPercent = (int) ((usedMem * 100) / totalMem);

        int ping = 15;
        if (!editing && mc.getNetworkHandler() != null && mc.player != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }

        StringBuilder sb = new StringBuilder();
        if (m.showFps.get()) sb.append("FPS: ").append(fps).append("   ");
        if (m.showTps.get()) sb.append("TPS: 20.0   ");
        if (m.showPing.get()) sb.append("PING: ").append(ping).append("ms   ");
        if (m.showRam.get()) sb.append("RAM: ").append(usedMem).append("MB (").append(memPercent).append("%)");

        String str = sb.toString().trim();
        HudDraw.glass(ctx, x, y, w, h, 4.5f);

        int textColor = 0xFFFFFFFF;
        if ("Градация производительности".equals(m.colorMode.get())) {
            textColor = fps >= 60 ? 0xFF2ECC71 : (fps >= 30 ? 0xFFF1C40F : 0xFFE74C3C);
        } else if ("Акцент темы".equals(m.colorMode.get())) {
            textColor = Theme.accent();
        }

        Fonts.SEMIBOLD.draw(ctx, str, x + 6f, y + 5f, 5.8f, textColor);
    }
}
