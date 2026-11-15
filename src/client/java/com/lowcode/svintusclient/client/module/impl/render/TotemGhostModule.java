package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TotemGhostModule extends Module implements Render3DListener {

    public final ModeSetting style = add(new ModeSetting("Стиль призрака",
            "Ангел с крыльями", "Призрак Тотема", "Светящийся аватар", "Сфера вознесения"));
    public final BooleanSetting halo = add(new BooleanSetting("Нимб над головой", true));
    public final NumberSetting ascentSpeed = add(new NumberSetting("Скорость взлёта", 1.8f, 0.5f, 4.0f, 0.1f));
    public final ColorSetting ghostColor = add(new ColorSetting("Цвет призрака", 0xFFFFD700));
    public final BooleanSetting orbitRings = add(new BooleanSetting("Орбитальные кольца", true));

    public static final class GhostInstance {
        public final Vec3d spawnPos;
        public final long spawnTime;
        public final int color;

        public GhostInstance(Vec3d spawnPos, int color) {
            this.spawnPos = spawnPos;
            this.spawnTime = System.currentTimeMillis();
            this.color = color;
        }

        public boolean isAlive() {
            return System.currentTimeMillis() - spawnTime < 1800L;
        }

        public float getProgress() {
            return (float) (System.currentTimeMillis() - spawnTime) / 1800.0f;
        }
    }

    private static final List<GhostInstance> GHOSTS = new ArrayList<>();

    public TotemGhostModule() {
        super("Totem Animation", "Облегчённая и плавная 3D анимация срабатывания тотема бессмертия", Category.BATTLE);
    }

    public static void spawnGhost(Vec3d pos) {
        TotemGhostModule mod = com.lowcode.svintusclient.client.module.ModuleManager.get(TotemGhostModule.class);
        if (mod == null || !mod.isEnabled() || pos == null) return;
        GHOSTS.add(new GhostInstance(pos, mod.ghostColor.getEffectiveColor()));
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled() || GHOSTS.isEmpty()) return;

        Iterator<GhostInstance> it = GHOSTS.iterator();
        while (it.hasNext()) {
            GhostInstance g = it.next();
            if (!g.isAlive()) {
                it.remove();
                continue;
            }

            float p = g.getProgress();
            float fade = 1.0f - p;
            int a = Math.round(fade * 240.0f);
            if (a <= 5) continue;

            int col = (a << 24) | (g.color & 0x00FFFFFF);
            int outlineCol = (a << 24) | 0xFFFFFF;

            double riseY = g.spawnPos.y + 0.5 + p * ascentSpeed.get() * 2.5;
            Vec3d curPos = new Vec3d(g.spawnPos.x, riseY, g.spawnPos.z);
            String st = style.get();

            // 1. Ghost Body
            Box body = new Box(curPos.x - 0.20, curPos.y, curPos.z - 0.12,
                    curPos.x + 0.20, curPos.y + 0.70, curPos.z + 0.12);
            Render3D.drawBox(body, col & 0x70FFFFFF, outlineCol, true, true, 1.2f);

            // 2. Ghost Head
            Box head = new Box(curPos.x - 0.14, curPos.y + 0.72, curPos.z - 0.14,
                    curPos.x + 0.14, curPos.y + 1.00, curPos.z + 0.14);
            Render3D.drawBox(head, col & 0x90FFFFFF, outlineCol, true, true, 1.2f);

            // 3. Halo
            if (halo.get()) {
                Render3D.drawCircle(new Vec3d(curPos.x, curPos.y + 1.10, curPos.z), 0.22, 16, 0xFFFFD700 & (a << 24 | 0x00FFFFFF), 1.5f);
            }

            // 4. Wings
            if ("Ангел с крыльями".equals(st)) {
                double wingFlap = Math.sin((System.currentTimeMillis() - g.spawnTime) / 120.0) * 0.25;
                Box wingL = new Box(curPos.x - 0.60, curPos.y + 0.35 + wingFlap, curPos.z - 0.05,
                        curPos.x - 0.20, curPos.y + 0.65 + wingFlap, curPos.z + 0.05);
                Box wingR = new Box(curPos.x + 0.20, curPos.y + 0.35 - wingFlap, curPos.z - 0.05,
                        curPos.x + 0.60, curPos.y + 0.65 - wingFlap, curPos.z + 0.05);
                Render3D.drawBox(wingL, col & 0x60FFFFFF, outlineCol, true, true, 1.0f);
                Render3D.drawBox(wingR, col & 0x60FFFFFF, outlineCol, true, true, 1.0f);
            }

            // 5. Orbit Rings
            if (orbitRings.get()) {
                Render3D.drawCircle(curPos.add(0, 0.35, 0), 0.45 + p * 0.5, 20, col, 1.0f);
            }
        }
    }
}
