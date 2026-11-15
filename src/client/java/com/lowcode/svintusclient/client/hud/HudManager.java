package com.lowcode.svintusclient.client.hud;

import com.lowcode.svintusclient.client.SvintusclientClient;
import com.lowcode.svintusclient.client.hud.widget.ArmorHudWidget;
import com.lowcode.svintusclient.client.hud.widget.CooldownsWidget;
import com.lowcode.svintusclient.client.hud.widget.CoordinatesWidget;
import com.lowcode.svintusclient.client.hud.widget.FPSPlusWidget;
import com.lowcode.svintusclient.client.hud.widget.InventoryWidget;
import com.lowcode.svintusclient.client.hud.widget.KeyStrokesWidget;
import com.lowcode.svintusclient.client.hud.widget.KeybindsWidget;
import com.lowcode.svintusclient.client.hud.widget.NotificationsWidget;
import com.lowcode.svintusclient.client.hud.widget.PotionsWidget;
import com.lowcode.svintusclient.client.hud.widget.TargetHudWidget;
import com.lowcode.svintusclient.client.hud.widget.WatermarkWidget;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.render.BlurEffect;
import com.lowcode.svintusclient.client.render.Render2D;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HudManager {

    private static final List<HudWidget> WIDGETS = new ArrayList<>();

    public static final WatermarkWidget WATERMARK = add(new WatermarkWidget());
    public static final CoordinatesWidget COORDINATES = add(new CoordinatesWidget());
    public static final TargetHudWidget TARGET = add(new TargetHudWidget());
    public static final ArmorHudWidget ARMOR = add(new ArmorHudWidget());
    public static final PotionsWidget POTIONS = add(new PotionsWidget());
    public static final CooldownsWidget COOLDOWNS = add(new CooldownsWidget());
    public static final KeybindsWidget KEYBINDS = add(new KeybindsWidget());
    public static final KeyStrokesWidget KEYSTROKES = add(new KeyStrokesWidget());
    public static final InventoryWidget INVENTORY = add(new InventoryWidget());
    public static final FPSPlusWidget FPS_PLUS = add(new FPSPlusWidget());
    public static final NotificationsWidget NOTIFICATIONS = add(new NotificationsWidget());

    private HudManager() {
    }

    private static <T extends HudWidget> T add(T widget) {
        WIDGETS.add(widget);
        return widget;
    }

    public static List<HudWidget> widgets() {
        return Collections.unmodifiableList(WIDGETS);
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.of(SvintusclientClient.MOD_ID, "hud"), HudManager::render);
        HudEditor.register();
    }

    static boolean isActive(HudWidget widget) {
        if (!widget.isVisible()) return false;
        return widget == NOTIFICATIONS || ModuleManager.HUD.isEnabled();
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        Render2D.setPixelScale(client.getWindow().getScaleFactor());
        Render2D.setAlpha(1f);
        if (ModuleManager.HUD.blur.get()) BlurEffect.capture(ctx);

        float guiScale = client.getWindow().getScaleFactor();
        boolean editing = HudEditor.isEditing();
        float sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (HudWidget widget : WIDGETS) {
            if (!isActive(widget)) continue;
            widget.layout(editing);
            if (!HudEditor.isDragging(widget)) widget.place(sw, sh);
            Render2D.setPixelScale(guiScale * widget.scale);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(widget.x, widget.y);
            ctx.getMatrices().scale(widget.scale, widget.scale);
            ctx.getMatrices().translate(-widget.x, -widget.y);
            widget.draw(ctx, editing);
            ctx.getMatrices().popMatrix();
        }
        Render2D.setPixelScale(guiScale);
        Render2D.setAlpha(1f);

        // Интерактивные оверлеи экрана

        com.lowcode.svintusclient.client.module.impl.render.ItemUseProgressModule iup = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ItemUseProgressModule.class);
        if (iup != null && iup.isEnabled()) iup.render(ctx, (int) sw, (int) sh);

        com.lowcode.svintusclient.client.module.impl.render.ArmorInspectModule ai = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ArmorInspectModule.class);
        if (ai != null && ai.isEnabled()) ai.render(ctx, (int) sw, (int) sh);

        // Медиаплеер и всплывающий лог подбора предметов
        com.lowcode.svintusclient.client.module.impl.render.MediaPlayerHudModule mp = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.MediaPlayerHudModule.class);
        if (mp != null && mp.isEnabled()) mp.render(ctx);

        com.lowcode.svintusclient.client.module.impl.render.ItemPickupLogModule ipl = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.ItemPickupLogModule.class);
        if (ipl != null && ipl.isEnabled()) ipl.render(ctx, sw, sh);

        // HolyWorld Информационные HUD панели (Ивенты, Курс коина, Вайп-трекер)
        float hwY = 70f;
        com.lowcode.svintusclient.client.module.impl.misc.EventsModule ev = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.EventsModule.class);
        if (ev != null && ev.isEnabled()) {
            ev.renderHud(ctx, 4f, hwY);
            int count = Math.min(com.lowcode.svintusclient.client.holyworld.HolyWorldApiService.getEvents().size(), ev.maxEvents.get().intValue());
            float h = 14f + (count == 0 ? 14f : count * 12f + 4f);
            hwY += h + 4f;
        }

        com.lowcode.svintusclient.client.module.impl.misc.CoinRateModule cr = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.CoinRateModule.class);
        if (cr != null && cr.isEnabled()) {
            cr.renderHud(ctx, 4f, hwY);
            hwY += 30f;
        }

        com.lowcode.svintusclient.client.module.impl.misc.WipeTrackerModule wt = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.misc.WipeTrackerModule.class);
        if (wt != null && wt.isEnabled()) {
            wt.renderHud(ctx, 4f, hwY);
        }
    }
}
