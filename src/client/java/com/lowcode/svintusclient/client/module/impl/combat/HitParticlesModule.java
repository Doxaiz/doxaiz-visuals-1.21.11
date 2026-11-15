package com.lowcode.svintusclient.client.module.impl.combat;

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
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class HitParticlesModule extends Module implements TickListener, Render3DListener {

    private static final Identifier TEX_POINT = Identifier.of("doxaiz", "textures/particles/point.png");
    private static final Identifier TEX_STAR = Identifier.of("doxaiz", "textures/particles/star.png");
    private static final Identifier TEX_LIGHTNING = Identifier.of("doxaiz", "textures/particles/lighting.png");
    private static final Identifier TEX_CROSS = Identifier.of("doxaiz", "textures/particles/cross.png");
    private static final Identifier TEX_CROWN = Identifier.of("doxaiz", "textures/particles/crown.png");
    private static final Identifier TEX_HEART = Identifier.of("doxaiz", "textures/particles/heart.png");
    private static final Identifier TEX_LINE = Identifier.of("doxaiz", "textures/particles/line.png");
    private static final Identifier TEX_RHOMBUS = Identifier.of("doxaiz", "textures/particles/rhombus.png");
    private static final Identifier TEX_DOLLAR = Identifier.of("doxaiz", "textures/particles/dollar.png");
    private static final Identifier TEX_SNOWFLAKE = Identifier.of("doxaiz", "textures/particles/snowflake.png");
    private static final Identifier TEX_TRIANGLE = Identifier.of("doxaiz", "textures/particles/triangle.png");
    private static final Identifier TEX_ASTRIX_1 = Identifier.of("doxaiz", "textures/particles/hit_particle1.png");
    private static final Identifier TEX_ASTRIX_2 = Identifier.of("doxaiz", "textures/particles/hit_particle2.png");
    private static final Identifier TEX_ASTRIX_3 = Identifier.of("doxaiz", "textures/particles/hit_particle3.png");
    private static final Identifier TEX_ASTRIX_4 = Identifier.of("doxaiz", "textures/particles/hit_particle4.png");

    private static final Identifier[] ALL_TEXTURES = {
            TEX_POINT, TEX_STAR, TEX_LIGHTNING, TEX_CROSS, TEX_CROWN, TEX_HEART,
            TEX_LINE, TEX_RHOMBUS, TEX_DOLLAR, TEX_SNOWFLAKE, TEX_TRIANGLE,
            TEX_ASTRIX_1, TEX_ASTRIX_2, TEX_ASTRIX_3, TEX_ASTRIX_4
    };

    // Settings
    public final ModeSetting particleMode = add(new ModeSetting("Текстура частиц",
            "Отображать всё", "Звезда", "Молния", "Крест", "Корона", "Сердце", "Линия", "Ромб",
            "Доллар", "Снежинка", "Треугольник", "Точка", "Искры Astrix 1", "Искры Astrix 2",
            "Кристаллы Astrix 3", "Вспышка Astrix 4", "Кровь", "Огонь", "Выкл"));
    public final NumberSetting particleCount = add(new NumberSetting("Количество частиц", 16.0f, 3.0f, 48.0f, 1.0f));
    public final NumberSetting size = add(new NumberSetting("Размер", 38.0f, 15.0f, 75.0f, 1.0f));
    public final BooleanSetting scaleWithAlpha = add(new BooleanSetting("Масштаб по прозрачности", true));
    public final NumberSetting spread = add(new NumberSetting("Разброс спавна", 1.5f, 0.3f, 4.0f, 0.1f));
    public final ModeSetting spawnDirection = add(new ModeSetting("Пресет полёта",
            "Случайно", "От цели", "К цели", "Закручивание"));
    public final NumberSetting lifetime = add(new NumberSetting("Время жизни (мс)", 1200.0f, 350.0f, 2500.0f, 50.0f));
    public final NumberSetting speed = add(new NumberSetting("Начальная скорость", 0.35f, 0.05f, 1.0f, 0.05f));
    public final NumberSetting gravity = add(new NumberSetting("Гравитация", 0.20f, -0.5f, 1.0f, 0.05f));
    public final ModeSetting collideMode = add(new ModeSetting("При столкновении с блоками",
            "Отскок", "Сильный отскок", "Слабый отскок", "Скольжение", "Прилипание", "Исчезновение", "Игнорировать"));
    public final BooleanSetting cancelVanilla = add(new BooleanSetting("Скрыть ванильные партиклы удара", true));

    // Color
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Клиент", "Радуга", "Кастом", "Градиент"));
    public final ColorSetting customColor = add(new ColorSetting("Основной цвет", 0xFFFF3366));
    public final ColorSetting customSecondColor = add(new ColorSetting("Второй цвет", 0xFF6C5CE7));
    public final BooleanSetting useSecondColor = add(new BooleanSetting("Использовать второй цвет", true));

    // Shockwave
    public final BooleanSetting hitCircle = add(new BooleanSetting("3D ударная волна (Shockwave)", true));
    public final NumberSetting hitCircleRadius = add(new NumberSetting("Радиус волны", 1.8f, 0.5f, 4.0f, 0.1f));
    public final ColorSetting hitCircleColor = add(new ColorSetting("Цвет волны", 0xFFFF2A6D));

    public static final class Particle {
        public double posX, posY, posZ;
        public double prevPosX, prevPosY, prevPosZ;
        public double motionX, motionY, motionZ;
        public float rotationDeg;
        public final float lifetimeMs;
        public final Identifier texture;
        public final float gradientT;
        public float twist;
        public boolean holdCollide;
        public boolean dead;
        public final long spawnTime;

        public Particle(Vec3d pos, Vec3d motion, float rotationDeg, float lifetimeMs, Identifier texture, float gradientT) {
            this.posX = pos.x;
            this.posY = pos.y;
            this.posZ = pos.z;
            this.prevPosX = pos.x;
            this.prevPosY = pos.y;
            this.prevPosZ = pos.z;
            this.motionX = motion.x;
            this.motionY = motion.y;
            this.motionZ = motion.z;
            this.rotationDeg = rotationDeg;
            this.lifetimeMs = lifetimeMs;
            this.texture = texture;
            this.gradientT = gradientT;
            this.spawnTime = System.currentTimeMillis();
        }

        public boolean isDead(long now) {
            return dead || (now - spawnTime) >= lifetimeMs;
        }

        public void beginStep() {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
        }

        public float alpha01(long now) {
            long elapsed = now - spawnTime;
            if (elapsed >= lifetimeMs) return 0.0f;
            return 1.0f - (float) elapsed / lifetimeMs;
        }

        public Vec3d renderPos(float partialTicks) {
            double x = prevPosX + (posX - prevPosX) * (double) partialTicks;
            double y = prevPosY + (posY - prevPosY) * (double) partialTicks;
            double z = prevPosZ + (posZ - prevPosZ) * (double) partialTicks;
            return new Vec3d(x, y, z);
        }
    }

    public static final class HitWave {
        public final Vec3d pos;
        public final float maxRadius;
        public final int color;
        public final long spawnTime;

        public HitWave(Vec3d pos, float maxRadius, int color) {
            this.pos = pos;
            this.maxRadius = maxRadius;
            this.color = color;
            this.spawnTime = System.currentTimeMillis();
        }

        public boolean isAlive() {
            return System.currentTimeMillis() - spawnTime < 450L;
        }

        public float getProgress() {
            return (float) (System.currentTimeMillis() - spawnTime) / 450.0f;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final List<HitWave> waves = new ArrayList<>();
    private final Random random = new Random();

    public HitParticlesModule() {
        super("HitParticles", "Эффектные частицы и визуальные эффекты при ударах", Category.BATTLE);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            particles.clear();
            waves.clear();
            return;
        }

        long now = System.currentTimeMillis();
        particles.removeIf(p -> p.isDead(now));
        waves.removeIf(w -> !w.isAlive());

        float grav = gravity.get();
        String cMode = collideMode.get();

        for (Particle p : particles) {
            p.beginStep();

            if (p.twist != 0.0f && !p.holdCollide) {
                double turn = 0.09 * (double) p.twist;
                double cos = Math.cos(turn);
                double sin = Math.sin(turn);
                double mx = p.motionX * cos - p.motionZ * sin;
                double mz = p.motionX * sin + p.motionZ * cos;
                p.motionX = mx;
                p.motionZ = mz;
            }

            p.motionY -= (double) grav / 17.5;
            if (grav > 0.0f && p.motionY > 0.0) {
                p.motionY /= 1.0 + (double) grav * 0.05;
            }

            if (!stepCollision(mc, p, cMode, grav)) {
                p.posX += p.motionX;
                p.posY += p.motionY;
                p.posZ += p.motionZ;
            }
        }
    }

    private boolean stepCollision(MinecraftClient mc, Particle p, String mode, float grav) {
        if ("Игнорировать".equals(mode)) return false;

        boolean hitX = checkSolid(mc, p.posX + p.motionX, p.posY, p.posZ);
        boolean hitY = checkSolid(mc, p.posX, p.posY + p.motionY, p.posZ);
        boolean hitZ = checkSolid(mc, p.posX, p.posY, p.posZ + p.motionZ);

        if (hitX || hitY || hitZ || p.holdCollide) {
            if ("Прилипание".equals(mode)) {
                p.motionX = 0;
                p.motionY = 0;
                p.motionZ = 0;
                p.holdCollide = true;
                return true;
            }
            if ("Исчезновение".equals(mode)) {
                p.dead = true;
                return true;
            }

            double bounceMult = switch (mode) {
                case "Сильный отскок" -> 0.85;
                case "Слабый отскок" -> 0.35;
                case "Скольжение" -> 0.0;
                default -> 0.60; // "Отскок"
            };

            if (hitY) {
                p.motionY = -p.motionY * bounceMult;
                if (Math.abs(p.motionY) < 0.02) p.motionY = 0;
                p.motionX *= 0.85;
                p.motionZ *= 0.85;
            }
            if (hitX) {
                p.motionX = -p.motionX * bounceMult;
            }
            if (hitZ) {
                p.motionZ = -p.motionZ * bounceMult;
            }
            return false;
        }
        return false;
    }

    private boolean checkSolid(MinecraftClient mc, double x, double y, double z) {
        if (mc.world == null) return false;
        BlockPos pos = BlockPos.ofFloored(x, y, z);
        BlockState state = mc.world.getBlockState(pos);
        return state.isSolidBlock(mc.world, pos);
    }

    public void onEntityHit(LivingEntity entity) {
        if (!isEnabled() || entity == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        String mode = particleMode.get();
        if (!"Выкл".equals(mode)) {
            int n = (int) particleCount.get().floatValue();
            float lt = lifetime.get();
            float sp = spread.get();
            float velSpeed = speed.get();
            Vec3d center = entity.getEyePos().add(0, -entity.getStandingEyeHeight() * 0.5, 0);
            float twistSign = "Закручивание".equals(spawnDirection.get()) ? (random.nextBoolean() ? 1.0f : -1.0f) : 0.0f;

            for (int i = 0; i < n && particles.size() < 600; i++) {
                Vec3d spawnPos = center.add(
                        (random.nextDouble() * 2.0 - 1.0) * sp,
                        (random.nextDouble() * 2.0 - 1.0) * sp * 0.7,
                        (random.nextDouble() * 2.0 - 1.0) * sp
                );

                double rndSpeed = velSpeed * (0.75 + random.nextDouble() * 0.65);
                Vec3d motion = computeInitialMotion(center, spawnPos, rndSpeed, twistSign);
                float gradT = n <= 1 ? 0.0f : (float) i / (float) (n - 1);
                Identifier tex = pickTexture(mode);

                Particle p = new Particle(spawnPos, motion, (float) (random.nextDouble() * 360.0), lt, tex, gradT);
                p.twist = twistSign;
                particles.add(p);
            }
        }

        if (hitCircle.get()) {
            waves.add(new HitWave(entity.getEntityPos(), hitCircleRadius.get(), hitCircleColor.getEffectiveColor()));
        }
    }

    private Identifier pickTexture(String mode) {
        return switch (mode) {
            case "Точка" -> TEX_POINT;
            case "Звезда" -> TEX_STAR;
            case "Молния" -> TEX_LIGHTNING;
            case "Крест" -> TEX_CROSS;
            case "Корона" -> TEX_CROWN;
            case "Сердце" -> TEX_HEART;
            case "Линия" -> TEX_LINE;
            case "Ромб" -> TEX_RHOMBUS;
            case "Доллар" -> TEX_DOLLAR;
            case "Снежинка" -> TEX_SNOWFLAKE;
            case "Треугольник" -> TEX_TRIANGLE;
            case "Искры Astrix 1" -> TEX_ASTRIX_1;
            case "Искры Astrix 2" -> TEX_ASTRIX_2;
            case "Кристаллы Astrix 3" -> TEX_ASTRIX_3;
            case "Вспышка Astrix 4" -> TEX_ASTRIX_4;
            case "Кровь" -> TEX_POINT;
            case "Огонь" -> TEX_ASTRIX_1;
            default -> ALL_TEXTURES[random.nextInt(ALL_TEXTURES.length)];
        };
    }

    private Vec3d computeInitialMotion(Vec3d center, Vec3d spawnPos, double spd, float twist) {
        String dir = spawnDirection.get();
        if ("От цели".equals(dir)) {
            Vec3d diff = spawnPos.subtract(center);
            return diff.lengthSquared() > 0.001 ? diff.normalize().multiply(spd) : randomUnitVec().multiply(spd);
        }
        if ("К цели".equals(dir)) {
            Vec3d diff = center.subtract(spawnPos);
            return diff.lengthSquared() > 0.001 ? diff.normalize().multiply(spd) : randomUnitVec().multiply(spd);
        }
        if ("Закручивание".equals(dir)) {
            Vec3d diff = spawnPos.subtract(center);
            Vec3d horiz = new Vec3d(diff.x, 0, diff.z);
            if (horiz.lengthSquared() < 0.001) horiz = new Vec3d(1, 0, 0);
            else horiz = horiz.normalize();
            Vec3d tangent = new Vec3d(-horiz.z * twist, 0, horiz.x * twist);
            Vec3d blend = new Vec3d(tangent.x * 0.85 + horiz.x * 0.25, random.nextDouble() * 0.35 + 0.1, tangent.z * 0.85 + horiz.z * 0.25).normalize();
            return blend.multiply(spd);
        }
        return randomUnitVec().multiply(spd);
    }

    private Vec3d randomUnitVec() {
        double theta = random.nextDouble() * 2.0 * Math.PI;
        double y = (random.nextDouble() * 2.0 - 1.0) * 0.8;
        double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        return new Vec3d(Math.cos(theta) * r, y, Math.sin(theta) * r);
    }

    private int resolveParticleColor(Particle p, float alpha) {
        String cm = colorMode.get();
        int baseA = (int) (alpha * 255.0f);
        if (baseA <= 2) return 0;

        int rgb;
        if ("Радуга".equals(cm)) {
            float hue = ((System.currentTimeMillis() / 8L + (long) (p.gradientT * 360f)) % 360L) / 360.0f;
            rgb = java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f) & 0x00FFFFFF;
        } else if ("Клиент".equals(cm)) {
            int themeCol = Theme.accent();
            rgb = themeCol & 0x00FFFFFF;
        } else if ("Градиент".equals(cm)) {
            int c1 = customColor.getEffectiveColor();
            int c2 = useSecondColor.get() ? customSecondColor.getEffectiveColor() : 0xFF6C5CE7;
            rgb = Colors.lerp(c1, c2, p.gradientT) & 0x00FFFFFF;
        } else {
            rgb = customColor.getEffectiveColor() & 0x00FFFFFF;
        }

        return (baseA << 24) | rgb;
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || camera == null) return;

        float partialTicks = tickCounter != null ? tickCounter.getTickProgress(false) : 0.0f;
        long now = System.currentTimeMillis();
        float baseSz = size.get() * 0.012f;
        boolean scaleAlpha = scaleWithAlpha.get();

        // 1. Render custom 3D billboarding particles
        for (Particle p : particles) {
            float alpha = p.alpha01(now);
            if (alpha <= 0.005f) continue;

            Vec3d rPos = p.renderPos(partialTicks);
            int col = resolveParticleColor(p, alpha);
            if (col == 0) continue;

            float sz = scaleAlpha ? baseSz * alpha : baseSz;
            Render3D.drawBillboardTexture(matrices, consumers, camera, p.texture, rPos, sz, sz, p.rotationDeg, col);
        }

        // 2. Render ground shockwave rings
        if (!waves.isEmpty()) {
            Vec3d camPos = camera.getCameraPos();
            int segs = 28;

            for (HitWave w : waves) {
                if (!w.isAlive()) continue;
                if (w.pos.squaredDistanceTo(camPos) > 1024) continue;

                float prog = w.getProgress();
                float curR = w.maxRadius * (float) Math.sin(prog * Math.PI * 0.5);
                int baseA = (w.color >> 24) & 0xFF;
                if (baseA == 0) baseA = 220;
                int fadeA = Math.round(baseA * (1.0f - prog));
                if (fadeA <= 2) continue;

                int activeCol = (fadeA << 24) | (w.color & 0x00FFFFFF);
                double ringY = w.pos.y + 0.04;

                for (int i = 0; i < segs; i++) {
                    double a1 = (i * 2.0 * Math.PI) / segs;
                    double a2 = ((i + 1) * 2.0 * Math.PI) / segs;
                    Vec3d p1 = new Vec3d(w.pos.x + Math.cos(a1) * curR, ringY, w.pos.z + Math.sin(a1) * curR);
                    Vec3d p2 = new Vec3d(w.pos.x + Math.cos(a2) * curR, ringY, w.pos.z + Math.sin(a2) * curR);
                    Render3D.drawLine(p1, p2, activeCol, 2.2f);
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        particles.clear();
        waves.clear();
    }
}
