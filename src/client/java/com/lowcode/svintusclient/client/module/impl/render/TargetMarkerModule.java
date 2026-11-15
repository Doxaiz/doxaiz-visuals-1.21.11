package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import com.lowcode.svintusclient.client.util.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class TargetMarkerModule extends Module implements TickListener, Render3DListener {

    public final ModeSetting mode = add(new ModeSetting("Режим",
            "Astrix 1", "Astrix 2", "Astrix 3", "Череп (Skull)", "Цепь (Chain)", "Котност (Kotnost)",
            "Сфера (Orb)", "Захват (Capture)", "Блум (Bloom)", "Метка (Marker)",
            "Кристалл", "Ромб", "Кольцо", "Неоновые дуги (Kimiko)", "Бокс", "Аура Soup", "Свинка Svintus", "Шоквейв (Pulse)", "Блум-круг (Bloom)", "Прицельный крест (Reticle)", "Комбо / Все"));
    public final NumberSetting size = add(new NumberSetting("Размер", 0.75f, 0.3f, 1.8f, 0.05f));
    public final NumberSetting speed = add(new NumberSetting("Скорость", 1.2f, 0.2f, 3.0f, 0.1f));
    public final NumberSetting yOffset = add(new NumberSetting("Смещение Y", 0.45f, -0.5f, 1.5f, 0.05f));
    public final BooleanSetting rotate = add(new BooleanSetting("Вращение", true));
    public final BooleanSetting bobbing = add(new BooleanSetting("Плавающая анимация", true));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Кастом", "Акцент темы", "Радуга"));
    public final ColorSetting color = add(new ColorSetting("Цвет метки", 0xFFFF2A6D));
    public final BooleanSetting smoothSwitch = add(new BooleanSetting("Плавное переключение цели", true));
    public final BooleanSetting hurtPulse = add(new BooleanSetting("Пульсация при ударе", true));

    // Astrix & Doxaiz Textures
    private static final Identifier TEX_ASTRIX_1 = Identifier.of("doxaiz", "textures/target_marker.png");
    private static final Identifier TEX_ASTRIX_2 = Identifier.of("doxaiz", "textures/target_marker2.png");
    private static final Identifier TEX_ASTRIX_3 = Identifier.of("doxaiz", "textures/target_marker3.png");

    private static final Identifier[] TEX_SKULLS = {
            Identifier.of("doxaiz", "textures/target_marker_skull_0.png"),
            Identifier.of("doxaiz", "textures/target_marker_skull_1.png"),
            Identifier.of("doxaiz", "textures/target_marker_skull_2.png")
    };

    private static final Identifier TEX_CHAIN = Identifier.of("doxaiz", "textures/target_marker_chain.png");
    private static final Identifier TEX_KOTNOST = Identifier.of("doxaiz", "textures/target_marker_kotnost.png");
    private static final Identifier TEX_ORB = Identifier.of("doxaiz", "textures/targetesp/orb.png");
    private static final Identifier TEX_CAPTURE = Identifier.of("doxaiz", "textures/targetesp/capture.png");
    private static final Identifier TEX_BLOOM = Identifier.of("doxaiz", "textures/targetesp/bloom.png");

    private static final Identifier[] TEX_MARKERS = {
            Identifier.of("doxaiz", "textures/targetesp/marker/marker.png"),
            Identifier.of("doxaiz", "textures/targetesp/marker/marker1.png"),
            Identifier.of("doxaiz", "textures/targetesp/marker/marker2.png"),
            Identifier.of("doxaiz", "textures/targetesp/marker/marker3.png")
    };

    private LivingEntity target;
    private long lastTargetLockTime;
    private Vec3d smoothedPos;
    private Vec3d transitionStartPos;
    private long switchStartMs;
    private boolean switching;
    private int currentTargetId = Integer.MIN_VALUE;
    private float alpha;

    public TargetMarkerModule() {
        super("Таргет элемент", "3D анимированная метка над атакуемой целью", Category.BATTLE);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            target = null;
            clearState();
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Crosshair acquisition
        if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            if (isValidTarget(living, mc)) {
                updateTargetInternal(living, now);
            }
        } else if (mc.targetedEntity instanceof LivingEntity te && isValidTarget(te, mc)) {
            updateTargetInternal(te, now);
        }

        // 2. Validate active target
        if (target != null) {
            boolean dead = !target.isAlive() || target.isRemoved();
            boolean tooFar = target.squaredDistanceTo(mc.player) > 1296; // 36 blocks
            boolean timedOut = (now - lastTargetLockTime) > 5000L;
            if (dead || tooFar || timedOut) {
                target = null;
            }
        }

        // 3. Smooth alpha transition
        boolean active = target != null;
        float targetAlpha = active ? 1.0f : 0.0f;
        alpha += (targetAlpha - alpha) * 0.25f;
        if (alpha < 0.01f && !active) {
            clearState();
        }
    }

    private boolean isValidTarget(LivingEntity entity, MinecraftClient mc) {
        if (entity == null || entity == mc.player) return false;
        if (!entity.isAlive() || entity.isRemoved() || entity.getHealth() <= 0.0f) return false;
        return entity.squaredDistanceTo(mc.player) <= 1296;
    }

    private void updateTargetInternal(LivingEntity entity, long now) {
        if (entity == null) return;
        this.target = entity;
        this.lastTargetLockTime = now;
    }

    public void setTarget(LivingEntity entity) {
        if (entity != null && entity.isAlive() && !entity.isRemoved()) {
            this.target = entity;
            this.lastTargetLockTime = System.currentTimeMillis();
        }
    }

    public LivingEntity getTarget() {
        return target;
    }

    public LivingEntity getCurrentTarget() {
        return target;
    }

    private void clearState() {
        target = null;
        smoothedPos = null;
        transitionStartPos = null;
        switching = false;
        currentTargetId = Integer.MIN_VALUE;
        alpha = 0.0f;
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled() || camera == null || alpha <= 0.01f) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        float delta = tickCounter != null ? tickCounter.getTickProgress(false) : 0.0f;
        long now = System.currentTimeMillis();

        if (target != null && target.isAlive()) {
            Vec3d rawPos = target.getLerpedPos(delta);

            if (target.getId() != currentTargetId) {
                if (smoothedPos != null && currentTargetId != Integer.MIN_VALUE && smoothSwitch.get()) {
                    transitionStartPos = smoothedPos;
                    switchStartMs = now;
                    switching = true;
                } else {
                    transitionStartPos = rawPos;
                    switching = false;
                }
                currentTargetId = target.getId();
            }

            if (switching && transitionStartPos != null) {
                float f = MathHelper.clamp((float) (now - switchStartMs) / 250.0f, 0.0f, 1.0f);
                float ease = easeInOutQuad(f);
                smoothedPos = transitionStartPos.lerp(rawPos, ease);
                if (f >= 1.0f) {
                    switching = false;
                    transitionStartPos = null;
                }
            } else {
                smoothedPos = smoothedPos == null ? rawPos : smoothedPos.lerp(rawPos, 0.45);
            }
        }

        if (smoothedPos == null) return;
        Vec3d targetPos = smoothedPos;

        // Color computation
        int col = computeColor();
        int baseAlpha = (int) (alpha * 255.0f);
        if (baseAlpha <= 2) return;
        int activeColor = (baseAlpha << 24) | (col & 0x00FFFFFF);

        // Size & Animation
        float sz = size.get();
        if (hurtPulse.get() && target != null && target.hurtTime > 0) {
            float pulse = (float) Math.sin((target.hurtTime / 10.0f) * Math.PI) * 0.25f;
            sz *= (1.0f + pulse);
        }

        double time = (now % 100000L) * 0.001 * speed.get();
        String currentMode = mode.get();

        // 1. Textured Billboard Markers
        Identifier texture = resolveTexture(currentMode, now);
        if (texture != null) {
            double bob = bobbing.get() ? Math.sin(time * 3.5) * 0.12 : 0.0;
            double targetHeight = target != null ? target.getHeight() : 1.8;
            double hoverY = targetPos.y + targetHeight + yOffset.get() + bob;
            Vec3d markPos = new Vec3d(targetPos.x, hoverY, targetPos.z);
            float rot = rotate.get() ? (float) ((time * 75.0) % 360.0) : 0.0f;
            Render3D.drawBillboardTexture(matrices, consumers, camera, texture, markPos, sz, sz, rot, activeColor);
            if (!"Комбо / Все".equals(currentMode)) {
                return;
            }
        }

        // 2. 3D Procedural Modes
        renderProcedural(matrices, consumers, camera, targetPos, sz, time, currentMode, activeColor);
    }

    private void renderProcedural(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera,
                                  Vec3d targetPos, float sz, double time, String currentMode, int activeColor) {
        double targetHeight = target != null ? target.getHeight() : 1.8;
        double targetWidth = target != null ? target.getWidth() : 0.6;
        Vec3d camPos = camera.getCameraPos();

        if ("Кристалл".equals(currentMode) || "Ромб".equals(currentMode)) {
            double hoverY = targetPos.y + targetHeight + 0.45 + Math.sin(time * 3.5) * 0.12;
            Vec3d center = new Vec3d(targetPos.x, hoverY, targetPos.z);
            Vec3d top = center.add(0, sz * 0.65, 0);
            Vec3d bot = center.add(0, -sz * 0.65, 0);

            int fillA = (int) (((activeColor >>> 24) & 0xFF) * 0.45f);
            int fillColor = (fillA << 24) | (activeColor & 0x00FFFFFF);

            int segs = 4;
            for (int i = 0; i < segs; i++) {
                double a1 = (i * 2.0 * Math.PI / segs) + time * 2.0;
                double a2 = ((i + 1) * 2.0 * Math.PI / segs) + time * 2.0;

                Vec3d p1 = center.add(Math.cos(a1) * sz * 0.45, 0, Math.sin(a1) * sz * 0.45);
                Vec3d p2 = center.add(Math.cos(a2) * sz * 0.45, 0, Math.sin(a2) * sz * 0.45);

                Render3D.drawGradientTriangle(top, p1, p2, fillColor, fillColor, fillColor);
                Render3D.drawGradientTriangle(bot, p2, p1, fillColor, fillColor, fillColor);

                Render3D.drawLine(top, p1, activeColor, 2.0f);
                Render3D.drawLine(bot, p1, activeColor, 2.0f);
                Render3D.drawLine(p1, p2, activeColor, 2.2f);
            }
        } else if ("Кольцо".equals(currentMode) || "Неоновые дуги (Kimiko)".equals(currentMode)) {
            // High-fidelity Kimiko dual crescent glowing arcs with RenderLayers.lightning()
            float radius = (float) ((targetWidth * 0.75 + 0.25) * sz);
            float thickness = 0.14f * sz;
            double scanY = targetPos.y + (Math.sin(time * 2.5) + 1.0) * 0.5 * targetHeight;

            VertexConsumer consumer = consumers.getBuffer(RenderLayers.lightning());
            matrices.push();
            matrices.translate(targetPos.x - camPos.x, scanY - camPos.y, targetPos.z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) (time * 2.5)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.0f));

            MatrixStack.Entry entry = matrices.peek();
            float innerR = Math.max(0.1f, radius - thickness * 0.5f);
            float outerR = radius + thickness * 0.5f;

            int segs = 32;
            for (int arc = 0; arc < 2; arc++) {
                float startAngle = arc * (float) Math.PI;
                float arcLength = (float) Math.PI * 0.8f;

                for (int i = 0; i < segs; i++) {
                    float f1 = (float) i / segs;
                    float f2 = (float) (i + 1) / segs;
                    float a1 = startAngle + f1 * arcLength;
                    float a2 = startAngle + f2 * arcLength;

                    float sin1 = MathHelper.sin(a1);
                    float cos1 = MathHelper.cos(a1);
                    float sin2 = MathHelper.sin(a2);
                    float cos2 = MathHelper.cos(a2);

                    float fade1 = (float) Math.sin(f1 * Math.PI) * alpha;
                    float fade2 = (float) Math.sin(f2 * Math.PI) * alpha;

                    int c1 = Colors.withAlpha(activeColor, fade1);
                    int c2 = Colors.withAlpha(activeColor, fade2);

                    consumer.vertex(entry, cos1 * innerR, 0.0f, sin1 * innerR).color(c1);
                    consumer.vertex(entry, cos1 * outerR, 0.0f, sin1 * outerR).color(c1);
                    consumer.vertex(entry, cos2 * outerR, 0.0f, sin2 * outerR).color(c2);
                    consumer.vertex(entry, cos2 * innerR, 0.0f, sin2 * innerR).color(c2);
                }
            }
            matrices.pop();
        } else if ("Бокс".equals(currentMode)) {
            double halfW = targetWidth / 2.0 + 0.08;
            Box box = new Box(
                    targetPos.x - halfW, targetPos.y, targetPos.z - halfW,
                    targetPos.x + halfW, targetPos.y + targetHeight + 0.08, targetPos.z + halfW
            );
            int fillA = (int) (((activeColor >>> 24) & 0xFF) * 0.25f);
            int fillCol = (fillA << 24) | (activeColor & 0x00FFFFFF);
            Render3D.drawBox(box, fillCol, activeColor, true, true, 2.0f);
        } else if ("Аура Soup".equals(currentMode) || "Комбо / Все".equals(currentMode)) {
            double baseRadius = targetWidth * 0.95 + Math.sin(time * 3.5) * 0.12;
            Vec3d feet = targetPos.add(0, 0.06, 0);
            int segs = 32;
            for (int i = 0; i < segs; i++) {
                double a1 = Math.toRadians((i * 360.0 / segs) + (time * 60.0));
                double a2 = Math.toRadians(((i + 1) * 360.0 / segs) + (time * 60.0));
                Vec3d p1 = feet.add(Math.cos(a1) * baseRadius, 0, Math.sin(a1) * baseRadius);
                Vec3d p2 = feet.add(Math.cos(a2) * baseRadius, 0, Math.sin(a2) * baseRadius);
                Render3D.drawLine(p1, p2, activeColor, 2.5f);
            }
        } else if ("Шоквейв (Pulse)".equals(currentMode)) {
            double progress = (time * 1.5) % 1.0;
            double pr = (targetWidth * 0.5 + progress * 1.6) * sz;
            Vec3d feet = targetPos.add(0, 0.05, 0);
            int segs = 28;
            int waveCol = Colors.withAlpha(activeColor, (float) (1.0 - progress));
            for (int i = 0; i < segs; i++) {
                double a1 = (i * 2.0 * Math.PI / segs);
                double a2 = ((i + 1) * 2.0 * Math.PI / segs);
                Vec3d p1 = feet.add(Math.cos(a1) * pr, 0, Math.sin(a1) * pr);
                Vec3d p2 = feet.add(Math.cos(a2) * pr, 0, Math.sin(a2) * pr);
                Render3D.drawLine(p1, p2, waveCol, 2.5f);
            }
        } else if ("Блум-круг (Bloom)".equals(currentMode)) {
            double r = targetWidth * 0.9 * sz;
            Vec3d feet = targetPos.add(0, 0.04, 0);
            int segs = 32;
            int discCol = Colors.withAlpha(activeColor, 0.28f);
            for (int i = 0; i < segs; i++) {
                double a1 = (i * 2.0 * Math.PI / segs) + time;
                double a2 = ((i + 1) * 2.0 * Math.PI / segs) + time;
                Vec3d p1 = feet.add(Math.cos(a1) * r, 0, Math.sin(a1) * r);
                Vec3d p2 = feet.add(Math.cos(a2) * r, 0, Math.sin(a2) * r);
                Render3D.drawLine(p1, p2, activeColor, 2.2f);
                Render3D.drawQuad(feet, p1, p2, feet, discCol);
            }
        } else if ("Свинка Svintus".equals(currentMode)) {
            double hoverY = targetPos.y + targetHeight + 0.45 + Math.sin(time * 4.0) * 0.12;
            Box pigHead = new Box(targetPos.x - 0.22 * sz, hoverY, targetPos.z - 0.22 * sz,
                    targetPos.x + 0.22 * sz, hoverY + 0.40 * sz, targetPos.z + 0.22 * sz);
            Render3D.drawBox(pigHead, 0x88FFAAA6, 0xFFFF7675, true, true, 2.0f);
        } else if ("Прицельный крест (Reticle)".equals(currentMode)) {
            double hoverY = targetPos.y + targetHeight * 0.55;
            double len = 0.45 * sz;
            Render3D.drawLine(new Vec3d(targetPos.x - len, hoverY, targetPos.z), new Vec3d(targetPos.x + len, hoverY, targetPos.z), activeColor, 2.5f);
            Render3D.drawLine(new Vec3d(targetPos.x, hoverY - len, targetPos.z), new Vec3d(targetPos.x, hoverY + len, targetPos.z), activeColor, 2.5f);
            Render3D.drawLine(new Vec3d(targetPos.x, hoverY, targetPos.z - len), new Vec3d(targetPos.x, hoverY, targetPos.z + len), activeColor, 2.5f);
        }
    }

    private Identifier resolveTexture(String currentMode, long now) {
        return switch (currentMode) {
            case "Astrix 1" -> TEX_ASTRIX_1;
            case "Astrix 2" -> TEX_ASTRIX_2;
            case "Astrix 3" -> TEX_ASTRIX_3;
            case "Череп (Skull)" -> {
                int frame = (int) ((now / 130L) % TEX_SKULLS.length);
                yield TEX_SKULLS[frame];
            }
            case "Цепь (Chain)" -> TEX_CHAIN;
            case "Котност (Kotnost)" -> TEX_KOTNOST;
            case "Сфера (Orb)" -> TEX_ORB;
            case "Захват (Capture)" -> TEX_CAPTURE;
            case "Блум (Bloom)" -> TEX_BLOOM;
            case "Метка (Marker)" -> {
                int frame = (int) ((now / 110L) % TEX_MARKERS.length);
                yield TEX_MARKERS[frame];
            }
            case "Комбо / Все" -> TEX_ASTRIX_1;
            default -> null;
        };
    }

    private int computeColor() {
        String cm = colorMode.get();
        if ("Радуга".equals(cm)) {
            float hue = (System.currentTimeMillis() % 3000L) / 3000.0f;
            return java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f);
        } else if ("Акцент темы".equals(cm)) {
            return Theme.accent();
        }
        return color.getEffectiveColor();
    }

    private static float easeInOutQuad(float t) {
        return t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
    }
}
