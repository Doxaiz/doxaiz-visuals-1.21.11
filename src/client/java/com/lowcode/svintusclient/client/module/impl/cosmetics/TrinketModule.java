package com.lowcode.svintusclient.client.module.impl.cosmetics;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class TrinketModule extends Module implements Render3DListener {

    public final ModeSetting style = add(new ModeSetting("Стиль",
            "Атом (Atom)", "Призрак (Ghost)", "Котик (Cat)", "Огонь (Flame)",
            "Пончик (Donut)", "Банан (Banana)", "Кристалл (Gem)", "Цветок (Flower)",
            "McTiers", "Шар (Ball)", "Рунические сферы", "Кристальные кубы",
            "Огненный лотос", "Неоновые кольца"));
    public final ColorSetting color = add(new ColorSetting("Цвет амулетов", 0xFF60A5FA));
    public final NumberSetting count = add(new NumberSetting("Количество", 3.0f, 1.0f, 6.0f, 1.0f));
    public final NumberSetting orbitRadius = add(new NumberSetting("Радиус орбиты", 0.85f, 0.4f, 2.0f, 0.05f));
    public final NumberSetting rotationSpeed = add(new NumberSetting("Скорость", 1.0f, 0.2f, 3.0f, 0.1f));

    private static final Identifier TEX_ATOM = Identifier.of("doxaiz", "textures/trinket/atom.png");
    private static final Identifier TEX_GHOST = Identifier.of("doxaiz", "textures/trinket/ghost.png");
    private static final Identifier TEX_CAT = Identifier.of("doxaiz", "textures/trinket/cat.png");
    private static final Identifier TEX_FLAME = Identifier.of("doxaiz", "textures/trinket/flame.png");
    private static final Identifier TEX_DONUT = Identifier.of("doxaiz", "textures/trinket/donut.png");
    private static final Identifier TEX_BANANA = Identifier.of("doxaiz", "textures/trinket/banana.png");
    private static final Identifier TEX_GEM = Identifier.of("doxaiz", "textures/trinket/gem.png");
    private static final Identifier TEX_FLOWER = Identifier.of("doxaiz", "textures/trinket/flower.png");
    private static final Identifier TEX_MCTIERS = Identifier.of("doxaiz", "textures/trinket/mctiers.png");
    private static final Identifier TEX_BALL = Identifier.of("doxaiz", "textures/trinket/ball.png");

    public TrinketModule() {
        super("Trinket Auras", "Парящие магические амулеты и рунические обереги вокруг персонажа", Category.COSMETICS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null) return;

        PlayerEntity player = mc.player;
        float delta = tickCounter.getTickProgress(false);
        Vec3d pPos = player.getLerpedPos(delta);

        int n = Math.round(count.get());
        float rad = orbitRadius.get();
        double speed = rotationSpeed.get();
        double time = (System.currentTimeMillis() / 1000.0) * speed;

        double baseHeight = pPos.y + player.getHeight() * 0.55;
        int col = color.get();

        String st = style.get();
        Identifier tex = switch (st) {
            case "Атом (Atom)" -> TEX_ATOM;
            case "Призрак (Ghost)" -> TEX_GHOST;
            case "Котик (Cat)" -> TEX_CAT;
            case "Огонь (Flame)" -> TEX_FLAME;
            case "Пончик (Donut)" -> TEX_DONUT;
            case "Банан (Banana)" -> TEX_BANANA;
            case "Кристалл (Gem)" -> TEX_GEM;
            case "Цветок (Flower)" -> TEX_FLOWER;
            case "McTiers" -> TEX_MCTIERS;
            case "Шар (Ball)" -> TEX_BALL;
            default -> null;
        };

        for (int i = 0; i < n; i++) {
            double angle = time + (i * Math.PI * 2.0 / n);
            double bob = Math.sin(time * 2.0 + i) * 0.12;
            double x = pPos.x + Math.cos(angle) * rad;
            double z = pPos.z + Math.sin(angle) * rad;
            double y = baseHeight + bob;
            Vec3d charmPos = new Vec3d(x, y, z);

            if (tex != null) {
                float rot = (float) ((time * 30.0 + i * 45.0) % 360.0);
                Render3D.drawBillboardTexture(matrices, consumers, camera, tex, charmPos, 0.28f, 0.28f, rot, col);
            } else if ("Кристальные кубы".equals(st)) {
                Render3D.drawRotatedCube(charmPos, 0.12, time * 3.0, time * 2.0, 0, col, 0xFFFFFFFF, true, true, 1.5f);
            } else if ("Огненный лотос".equals(st)) {
                Render3D.drawCircle(charmPos, 0.14, 16, col, 2.0f);
                Render3D.drawRotatedCube(charmPos, 0.08, 0, time * 4.0, 0, 0xFFF59E0B, 0xFFFFFFFF, true, true, 1.2f);
            } else if ("Неоновые кольца".equals(st)) {
                Render3D.drawCircle(charmPos, 0.16, 20, col, 2.2f);
                Render3D.drawCircle(charmPos, 0.08, 12, 0xFFFFFFFF, 1.5f);
            } else {
                // Рунические сферы
                Render3D.drawCircle(charmPos, 0.12, 16, col, 2.0f);
                Render3D.drawRotatedCube(charmPos, 0.09, time * 2.5, time * 1.5, 0, col, 0xFFFFFFFF, true, true, 1.2f);
            }

            // Тонкая световая нить связи с телом игрока
            Vec3d core = new Vec3d(pPos.x, baseHeight, pPos.z);
            int lineColor = (col & 0x00FFFFFF) | 0x33000000;
            Render3D.drawLine(core, charmPos, lineColor, 1.0f);
        }
    }
}
