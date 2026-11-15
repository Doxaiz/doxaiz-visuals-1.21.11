package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.Notifications;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.Render3D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import com.lowcode.svintusclient.client.util.SoundPreview;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class WaypointsModule extends Module implements Render3DListener, TickListener {

    // 3D Beacons and Waypoints
    public final BooleanSetting showDeathPoint = add(new BooleanSetting("Точка смерти в мире", true));
    public final BooleanSetting showSpawnPoint = add(new BooleanSetting("Точка спавна в мире", true));
    public final BooleanSetting beaconBeam = add(new BooleanSetting("Световой луч (Маяк)", true));
    public final NumberSetting beamRadius = add(new NumberSetting("Радиус луча", 0.45f, 0.2f, 1.5f, 0.05f));
    public final ColorSetting deathColor = add(new ColorSetting("Цвет точки смерти", 0xFFFF4757));
    public final ColorSetting spawnColor = add(new ColorSetting("Цвет спавна", 0xFF2ED573));

    // GPS Screen Compass
    public final BooleanSetting showGps = add(new BooleanSetting("GPS Стрелка на экране", true));
    public final ModeSetting targetMode = add(new ModeSetting("Цель GPS",
            "Точка смерти", "Спавн мира (0, 0)", "Кастомные XYZ", "Ближайшая метка"));
    public final NumberSetting customX = add(new NumberSetting("Цель X", 0.0f, -10000.0f, 10000.0f, 10.0f));
    public final NumberSetting customZ = add(new NumberSetting("Цель Z", 0.0f, -10000.0f, 10000.0f, 10.0f));
    public final NumberSetting gpsRadius = add(new NumberSetting("Радиус от прицела", 65.0f, 30.0f, 140.0f, 5.0f));
    public final NumberSetting arrowSize = add(new NumberSetting("Размер стрелки", 18.0f, 10.0f, 36.0f, 2.0f));
    public final BooleanSetting showDistance = add(new BooleanSetting("Дистанция (метры)", true));
    public final ModeSetting colorMode = add(new ModeSetting("Цвет стрелки", "Акцент темы", "Кастом"));
    public final ColorSetting customColor = add(new ColorSetting("Свой цвет стрелки", 0xFF00F0FF));

    // Death coordinates notifications
    public final BooleanSetting chatDeathAlert = add(new BooleanSetting("Координаты смерти в чат", true));
    public final BooleanSetting deathSound = add(new BooleanSetting("Звук при смерти", true));

    public record Waypoint(String name, BlockPos pos, int color) {}

    private static final List<Waypoint> CUSTOM_WAYPOINTS = new ArrayList<>();
    private static BlockPos deathPos = null;
    private static String deathDimension = "";
    private static final Identifier ARROW_TEX = Identifier.of("doxaiz", "textures/particles/gui/gps_arrow.png");
    private boolean wasDead = false;

    public WaypointsModule() {
        super("Waypoints & GPS", "3D световые лучи, метки в мире, GPS-компас у прицела и координаты смерти", Category.NAVIGATION);
    }

    public static void setDeathPos(BlockPos pos) {
        deathPos = pos;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null) {
            deathDimension = mc.world.getRegistryKey().getValue().getPath();
        }
        WaypointsModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(WaypointsModule.class);
        if (mod != null && mod.isEnabled() && pos != null) {
            if (mod.chatDeathAlert.get() && mc.player != null) {
                String msg = "§c[Doxaiz] §fТочка гибели: §eX: " + pos.getX() +
                        " Y: " + pos.getY() + " Z: " + pos.getZ() +
                        " §7(" + deathDimension + ")";
                mc.player.sendMessage(Text.literal(msg), false);
            }
            if (mod.deathSound.get()) {
                SoundPreview.playPreview("minecraft:block.bell.use", 0.9f);
            }
        }
    }

    public static BlockPos getDeathPos() {
        return deathPos;
    }

    public static void addWaypoint(String name, BlockPos pos, int color) {
        CUSTOM_WAYPOINTS.add(new Waypoint(name, pos, color));
        com.lowcode.svintusclient.client.Config.save();
    }

    public static List<Waypoint> getWaypoints() {
        return CUSTOM_WAYPOINTS;
    }

    public static void removeWaypoint(int index) {
        if (index >= 0 && index < CUSTOM_WAYPOINTS.size()) {
            CUSTOM_WAYPOINTS.remove(index);
            com.lowcode.svintusclient.client.Config.save();
        }
    }

    public static void clearWaypoints() {
        CUSTOM_WAYPOINTS.clear();
    }

    public static void quickWaypoint() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            BlockPos pos = mc.player.getBlockPos();
            int idx = CUSTOM_WAYPOINTS.size() + 1;
            addWaypoint("Метка #" + idx, pos, Theme.accent());
            Notifications.push("Метка #" + idx + " сохранена", true);
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        boolean isDead = !mc.player.isAlive() || mc.player.getHealth() <= 0.0f;
        if (isDead && !wasDead) {
            wasDead = true;
            setDeathPos(mc.player.getBlockPos());
        } else if (!isDead && wasDead) {
            wasDead = false;
        }
    }

    public void renderGps(DrawContext ctx, float screenW, float screenH) {
        if (!isEnabled() || !showGps.get()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;

        double targetX = 0;
        double targetZ = 0;
        String mode = targetMode.get();

        if ("Спавн мира (0, 0)".equals(mode)) {
            targetX = 0;
            targetZ = 0;
        } else if ("Точка смерти".equals(mode)) {
            if (deathPos == null) return;
            targetX = deathPos.getX();
            targetZ = deathPos.getZ();
        } else if ("Ближайшая метка".equals(mode)) {
            if (CUSTOM_WAYPOINTS.isEmpty() && deathPos == null) return;
            Waypoint closest = null;
            double minDist = Double.MAX_VALUE;
            for (Waypoint wp : CUSTOM_WAYPOINTS) {
                double d = mc.player.squaredDistanceTo(wp.pos.getX(), wp.pos.getY(), wp.pos.getZ());
                if (d < minDist) {
                    minDist = d;
                    closest = wp;
                }
            }
            if (closest != null) {
                targetX = closest.pos.getX();
                targetZ = closest.pos.getZ();
            } else if (deathPos != null) {
                targetX = deathPos.getX();
                targetZ = deathPos.getZ();
            } else {
                return;
            }
        } else {
            targetX = customX.get();
            targetZ = customZ.get();
        }

        double dx = targetX - mc.player.getX();
        double dz = targetZ - mc.player.getZ();
        double distSq = dx * dx + dz * dz;
        if (distSq < 16.0) return;

        int distMeters = (int) Math.sqrt(distSq);
        double targetAngle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double diffAngle = MathHelper.wrapDegrees(targetAngle - mc.player.getYaw());
        double rad = Math.toRadians(diffAngle - 90.0);

        float cx = screenW / 2.0f;
        float cy = screenH / 2.0f;
        float r = gpsRadius.get();
        float sz = arrowSize.get();

        float arrowX = MathHelper.clamp(cx + (float) (Math.cos(rad) * r), sz + 4f, screenW - sz - 4f);
        float arrowY = MathHelper.clamp(cy + (float) (Math.sin(rad) * r), sz + 4f, screenH - sz - 14f);
        int col = "Акцент темы".equals(colorMode.get()) ? Theme.accent() : customColor.getEffectiveColor();

        try {
            AbstractTexture texture = mc.getTextureManager().getTexture(ARROW_TEX);
            if (texture != null && texture.getGlTextureView() != null) {
                TextureSetup setup = TextureSetup.of(texture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));

                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(arrowX, arrowY);
                ctx.getMatrices().rotate((float) Math.toRadians(diffAngle));

                Render2D.texture(ctx, setup, -sz / 2.0f, -sz / 2.0f, sz, sz, 0.0f, 0.0f, 1.0f, 1.0f, col);
                ctx.getMatrices().popMatrix();

                if (showDistance.get()) {
                    String distStr = distMeters + "m";
                    float tw = Fonts.REGULAR.width(distStr, 6.0f);
                    float tx = arrowX - tw / 2.0f;
                    float ty = arrowY + sz / 2.0f + 2.0f;
                    Fonts.REGULAR.draw(ctx, distStr, tx, ty, 6.0f, 0xFFE0E0E0);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.world == null || mc.player == null) return;

        Vec3d camPos = camera.getCameraPos();
        TextRenderer textRenderer = mc.textRenderer;

        List<Waypoint> active = new ArrayList<>(CUSTOM_WAYPOINTS);
        if (showDeathPoint.get() && deathPos != null) {
            active.add(new Waypoint("Точка смерти", deathPos, deathColor.getEffectiveColor()));
        }
        if (showSpawnPoint.get()) {
            active.add(new Waypoint("Спавн (0, 0)", new BlockPos(0, 64, 0), spawnColor.getEffectiveColor()));
        }

        float bRadius = beamRadius.get();
        for (Waypoint wp : active) {
            Vec3d base = new Vec3d(wp.pos.getX() + 0.5, wp.pos.getY(), wp.pos.getZ() + 0.5);
            double dist = mc.player.getEntityPos().distanceTo(base);
            int col = wp.color;

            // 1. Отрисовка вертикального светового луча в небо
            if (beaconBeam.get()) {
                Vec3d top = base.add(0, 256.0, 0);
                int beamFill = (0x33000000) | (col & 0x00FFFFFF);
                int beamLine = (0xCC000000) | (col & 0x00FFFFFF);

                // Кольца у основания
                Render3D.drawCircle(base.add(0, 0.05, 0), bRadius * 2.0, 24, beamLine, 2.0f);
                Render3D.drawCircle(base.add(0, 0.05, 0), bRadius, 16, beamFill, 1.5f);

                // 4 вертикальные грани луча
                Vec3d p1 = base.add(-bRadius, 0, -bRadius);
                Vec3d p2 = base.add(bRadius, 0, -bRadius);
                Vec3d p3 = base.add(bRadius, 0, bRadius);
                Vec3d p4 = base.add(-bRadius, 0, bRadius);

                Vec3d t1 = top.add(-bRadius, 0, -bRadius);
                Vec3d t2 = top.add(bRadius, 0, -bRadius);
                Vec3d t3 = top.add(bRadius, 0, bRadius);
                Vec3d t4 = top.add(-bRadius, 0, bRadius);

                Render3D.drawGradientQuad(p1, p2, t2, t1, beamFill, beamFill, 0, 0);
                Render3D.drawGradientQuad(p2, p3, t3, t2, beamFill, beamFill, 0, 0);
                Render3D.drawGradientQuad(p3, p4, t4, t3, beamFill, beamFill, 0, 0);
                Render3D.drawGradientQuad(p4, p1, t1, t4, beamFill, beamFill, 0, 0);
            }

            // 2. 3D билборд с названием и дистанцией
            matrices.push();
            double tagY = base.y + 2.0;
            matrices.translate(base.x - camPos.x, tagY - camPos.y, base.z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float scale = Math.max(0.025f, (float) (dist * 0.0022));
            matrices.scale(-scale, -scale, scale);

            String label = String.format("%s §7[%dм]", wp.name, (int) dist);
            int textW = textRenderer.getWidth(label);
            int bg = 0x88000000;

            textRenderer.draw(label, -textW / 2.0f, 0, col, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.SEE_THROUGH, bg, 0xF000F0);
            textRenderer.draw(label, -textW / 2.0f, 0, col, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);

            matrices.pop();
        }
    }
}
