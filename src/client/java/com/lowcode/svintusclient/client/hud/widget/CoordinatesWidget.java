package com.lowcode.svintusclient.client.hud.widget;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.hud.HudWidget;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.render.icon.Icons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class CoordinatesWidget extends HudWidget {

    private static final float SIZE = 6.2f, H = 15f;

    private final BooleanSetting direction = option("Направление", true);
    private final BooleanSetting nether = option("Незер / Верхний мир", false);
    private final BooleanSetting showFps = option("FPS", false);
    private final BooleanSetting showBps = option("Скорость (BPS)", false);
    private final BooleanSetting showPing = option("Пинг", false);

    private String coords = "";
    private String dir = "";
    private String other = "";
    private String biome = "";
    private String fps = "";
    private String bps = "";
    private String ping = "";

    public CoordinatesWidget() {
        super("coordinates", "Координаты и Инфо", 0f, 1f);
    }

    @Override
    public boolean isVisible() {
        com.lowcode.svintusclient.client.module.impl.render.CoordsHudModule chm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CoordsHudModule.class);
        if (chm != null && chm.isEnabled()) return true;
        return ModuleManager.HUD != null && ModuleManager.HUD.coordinates.get();
    }

    @Override
    public void setVisible(boolean v) {
        com.lowcode.svintusclient.client.module.impl.render.CoordsHudModule chm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CoordsHudModule.class);
        if (chm != null) chm.setEnabled(v);
        if (ModuleManager.HUD != null) ModuleManager.HUD.coordinates.set(v);
    }

    @Override
    protected void layout(boolean editing) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;

        if (player == null) {
            coords = "0, 0, 0";
            dir = "";
            other = "";
            fps = showFps.get() ? "60 FPS" : "";
            bps = showBps.get() ? "0.0 BPS" : "";
            ping = showPing.get() ? "25 ms" : "";
        } else {
            int px = (int) Math.floor(player.getX()), py = (int) Math.floor(player.getY()), pz = (int) Math.floor(player.getZ());
            com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule sm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule.class);
            boolean hideCoords = sm != null && sm.isEnabled() && sm.hideCoords.get();
            coords = hideCoords ? "***, ***, ***" : (px + ", " + py + ", " + pz);
            com.lowcode.svintusclient.client.module.impl.render.CoordsHudModule chm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CoordsHudModule.class);
            boolean useDir = chm != null ? chm.showDirection.get() : direction.get();
            boolean useNether = !hideCoords && (chm != null ? chm.showNether.get() : nether.get());
            boolean useSpeed = chm != null ? chm.showSpeed.get() : showBps.get();

            if (useDir) {
                var facing = player.getHorizontalFacing();
                String fmt = chm != null ? chm.directionFormat.get() : "Русский полный (Север, Юг...)";
                String baseDir = switch (facing) {
                    case NORTH -> fmt.contains("Русский краткий") ? "С" : (fmt.contains("Русский") ? "Север" : (fmt.contains("Английский краткий") ? "N" : "North"));
                    case SOUTH -> fmt.contains("Русский краткий") ? "Ю" : (fmt.contains("Русский") ? "Юг" : (fmt.contains("Английский краткий") ? "S" : "South"));
                    case WEST -> fmt.contains("Русский краткий") ? "З" : (fmt.contains("Русский") ? "Запад" : (fmt.contains("Английский краткий") ? "W" : "West"));
                    case EAST -> fmt.contains("Русский краткий") ? "В" : (fmt.contains("Русский") ? "Восток" : (fmt.contains("Английский краткий") ? "E" : "East"));
                    default -> "С";
                };
                if (chm != null && chm.showDegrees.get()) {
                    int deg = (int) Math.round((player.getYaw() % 360 + 360) % 360);
                    dir = baseDir + " " + deg + "°";
                } else {
                    dir = baseDir;
                }
            } else {
                dir = "";
            }
            boolean inNether = player.getEntityWorld().getRegistryKey() == World.NETHER;
            other = useNether ? (inNether ? (px * 8) + ", " + (pz * 8) : (px / 8) + ", " + (pz / 8)) : "";

            fps = showFps.get() ? mc.getCurrentFps() + " FPS" : "";

            if (useSpeed) {
                double dx = player.getX() - player.lastRenderX;
                double dz = player.getZ() - player.lastRenderZ;
                double bpsVal = Math.hypot(dx, dz) * 20.0;
                bps = String.format("%.1f BPS", bpsVal);
            } else {
                bps = "";
            }

            if (showPing.get()) {
                int latency = 0;
                if (mc.getNetworkHandler() != null) {
                    PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                    if (entry != null) latency = entry.getLatency();
                }
                ping = latency + " ms";
            } else {
                ping = "";
            }

            boolean useBiome = !hideCoords && (chm != null && chm.showBiome.get());
            if (useBiome && player.getEntityWorld() != null) {
                var biomeEntry = player.getEntityWorld().getBiome(player.getBlockPos());
                String biomeId = biomeEntry.getKey().map(k -> k.getValue().getPath()).orElse("");
                if (!biomeId.isEmpty()) {
                    biome = biomeId.substring(0, 1).toUpperCase() + biomeId.substring(1).replace('_', ' ');
                } else {
                    biome = "";
                }
            } else {
                biome = "";
            }
        }

        float width = 7f + Fonts.SEMIBOLD.width("XYZ", SIZE) + 5f + Fonts.REGULAR.width(coords, SIZE) + 7f;
        if (!dir.isEmpty()) width += 21f + Fonts.SEMIBOLD.width(dir, SIZE);
        if (!other.isEmpty()) width += 10f + Fonts.REGULAR.width(other, SIZE - 0.6f);
        if (!biome.isEmpty()) width += 10f + Fonts.REGULAR.width(biome, SIZE);
        if (!fps.isEmpty()) width += 10f + Fonts.REGULAR.width(fps, SIZE);
        if (!bps.isEmpty()) width += 10f + Fonts.REGULAR.width(bps, SIZE);
        if (!ping.isEmpty()) width += 10f + Fonts.REGULAR.width(ping, SIZE);

        w = width;
        h = H;
    }

    @Override
    protected void draw(DrawContext ctx, boolean editing) {
        com.lowcode.svintusclient.client.module.impl.render.CoordsHudModule chm = ModuleManager.get(com.lowcode.svintusclient.client.module.impl.render.CoordsHudModule.class);
        String st = chm != null ? chm.style.get() : "Стеклянная карта (Modern Glass)";

        if (st.contains("Капсула")) {
            Render2D.rect(ctx, x, y, w, h, h / 2f, 0xB0121218);
            Render2D.outline(ctx, x, y, w, h, h / 2f, 1.0f, Theme.accent(0.6f));
        } else if (st.contains("Компактный")) {
            Render2D.rect(ctx, x, y, w, h, 3.5f, 0xD0101014);
            Render2D.outline(ctx, x, y, w, h, 3.5f, 0.9f, 0x30FFFFFF);
        } else if (st.contains("Неоновая")) {
            HudDraw.glass(ctx, x, y, w, h, 5f);
            Render2D.rect(ctx, x + 2f, y, w - 4f, 2f, 1f, Theme.accent());
            Render2D.rect(ctx, x + 6f, y + 0.5f, w - 12f, 1f, 0.5f, 0xAAFFFFFF);
        } else {
            HudDraw.glass(ctx, x, y, w, h, 5f);
        }
        float cy = y + h / 2f, tx = x + 7f;

        tx += Fonts.SEMIBOLD.drawCentered(ctx, "XYZ", tx, cy, SIZE, Theme.accent()) + 5f;
        tx += Fonts.REGULAR.drawCentered(ctx, coords, tx, cy, SIZE, Theme.TEXT);

        if (!other.isEmpty()) {
            tx += 5f;
            Render2D.rect(ctx, tx, y + 4.5f, 0.8f, h - 9f, 0.4f, 0x1EFFFFFF);
            tx += 5f;
            tx += Fonts.REGULAR.drawCentered(ctx, other, tx, cy, SIZE - 0.6f, Theme.TEXT_DIM);
        }

        if (!biome.isEmpty()) {
            tx += 4f;
            Render2D.rect(ctx, tx, y + 4.5f, 0.8f, h - 9f, 0.4f, 0x1EFFFFFF);
            tx += 5f;
            tx += Fonts.REGULAR.drawCentered(ctx, biome, tx, cy, SIZE, 0xFF4FC3F7) + 4f;
        }

        if (!dir.isEmpty()) {
            tx += 6f;
            Render2D.rect(ctx, tx, y + 4.5f, 0.8f, h - 9f, 0.4f, 0x1EFFFFFF);
            tx += 6f;
            Icons.get("compass").drawCentered(ctx, tx + 3f, cy, 7f, Theme.accent());
            tx += Fonts.SEMIBOLD.drawCentered(ctx, dir, tx + 9f, cy, SIZE, Theme.TEXT) + 6f;
        }

        if (!fps.isEmpty()) {
            tx += 4f;
            Render2D.rect(ctx, tx, y + 4.5f, 0.8f, h - 9f, 0.4f, 0x1EFFFFFF);
            tx += 5f;
            tx += Fonts.REGULAR.drawCentered(ctx, fps, tx, cy, SIZE, Theme.accent()) + 4f;
        }

        if (!bps.isEmpty()) {
            tx += 4f;
            Render2D.rect(ctx, tx, y + 4.5f, 0.8f, h - 9f, 0.4f, 0x1EFFFFFF);
            tx += 5f;
            tx += Fonts.REGULAR.drawCentered(ctx, bps, tx, cy, SIZE, Theme.TEXT_DIM) + 4f;
        }

        if (!ping.isEmpty()) {
            tx += 4f;
            Render2D.rect(ctx, tx, y + 4.5f, 0.8f, h - 9f, 0.4f, 0x1EFFFFFF);
            tx += 5f;
            Fonts.REGULAR.drawCentered(ctx, ping, tx, cy, SIZE, 0xFF2ED573);
        }
    }
}
