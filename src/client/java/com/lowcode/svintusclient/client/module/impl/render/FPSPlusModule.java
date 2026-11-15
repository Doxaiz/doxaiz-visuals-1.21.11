package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class FPSPlusModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль панели",
            "Стеклянная плашка", "Компактные бейджи", "Минимализм", "Неоновый чип"));
    public final BooleanSetting showFps = add(new BooleanSetting("Отображать FPS", true));
    public final BooleanSetting showTps = add(new BooleanSetting("Отображать TPS сервера", true));
    public final BooleanSetting showPing = add(new BooleanSetting("Отображать Ping (мс)", true));
    public final BooleanSetting showRam = add(new BooleanSetting("Отображать ОЗУ (MB / %)", true));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Градация производительности", "Акцент темы", "Радуга", "Белый"));

    public FPSPlusModule() {
        super("FPS+", "Расширенный мониторинг производительности: FPS, TPS сервера, Ping и расход памяти", Category.INTERFACE);
    }

    public void render(DrawContext context, int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        int fps = mc.getCurrentFps();
        long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        int memPercent = (int) ((usedMem * 100) / totalMem);

        int ping = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }

        float tps = 20.0f;

        int x = 6;
        int y = 48;

        StringBuilder sb = new StringBuilder();
        if (showFps.get()) sb.append("FPS: ").append(fps).append("   ");
        if (showTps.get()) sb.append("TPS: ").append(String.format("%.1f", tps)).append("   ");
        if (showPing.get()) sb.append("PING: ").append(ping).append("ms   ");
        if (showRam.get()) sb.append("RAM: ").append(usedMem).append("MB (").append(memPercent).append("%)");

        String str = sb.toString().trim();
        float w = Fonts.SEMIBOLD.width(str, 5.8f) + 12f;
        float h = 15f;

        String st = style.get();
        if ("Компактные бейджи".equals(st)) {
            Render2D.rect(context, x, y, w, h, 3.5f, 0xB0121218);
            Render2D.outline(context, x, y, w, h, 3.5f, 1f, 0x40FFFFFF);
        } else if ("Неоновый чип".equals(st)) {
            HudDraw.glass(context, x, y, w, h, 4f);
            Render2D.outline(context, x, y, w, h, 4f, 1.2f, 0xFF00CEC9);
        } else if ("Минимализм".equals(st)) {
            Fonts.SEMIBOLD.draw(context, str, (float) x, (float) (y + 3), 5.8f, 0xFFE0E0E0);
            return;
        } else {
            HudDraw.glass(context, x, y, w, h, 4.5f);
        }

        int textColor = 0xFFFFFFFF;
        if ("Градация производительности".equals(colorMode.get())) {
            textColor = fps >= 60 ? 0xFF2ECC71 : (fps >= 30 ? 0xFFF1C40F : 0xFFE74C3C);
        } else if ("Акцент темы".equals(colorMode.get())) {
            textColor = Theme.accent();
        }

        Fonts.SEMIBOLD.draw(context, str, (float) (x + 6), (float) (y + 4.5f), 5.8f, textColor);
    }
}
