package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
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

public final class PlayerMobModule extends Module implements Render3DListener {

    public final ModeSetting petType = add(new ModeSetting("Питомец", "Свинка", "Лисичка", "Эллей", "Дракончик"));

    public final ModeSetting position = add(new ModeSetting("Позиция", "Правое плечо", "Левое плечо", "Парит рядом"));

    public final NumberSetting scale = add(new NumberSetting("Масштаб", 0.35f, 0.2f, 0.6f, 0.05f));
    public final BooleanSetting bobbing = add(new BooleanSetting("Анимация дыхания", true));
    public final BooleanSetting selfOnly = add(new BooleanSetting("Только у себя", false));
    public final NumberSetting bobbingSpeed = add(new NumberSetting("Скорость покачивания", 1.0f, 0.2f, 3.0f, 0.1f));
    public final BooleanSetting shadow = add(new BooleanSetting("Тень питомца", true));

    public boolean hasShadow() {
        return isEnabled() && shadow.get();
    }

    public PlayerMobModule() {
        super("Player Familiar", "Маленький 3D питомец-фамильяр на плече или парящий рядом с игроком", Category.COSMETICS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float delta = tickCounter.getTickProgress(false);
        Vec3d pPos = Render3D.interpolate(mc.player, delta);
        double time = (System.currentTimeMillis() % 100000L) * 0.003 * bobbingSpeed.get();

        double sideOff = "Левое плечо".equals(position.get()) ? -0.38 : ("Парит рядом".equals(position.get()) ? 0.75 : 0.38);
        double yOff = "Парит рядом".equals(position.get()) ? 1.4 : 1.35;
        if (bobbing.get()) {
            yOff += Math.sin(time * 3.0) * 0.05;
        }

        float yawRad = (float) Math.toRadians(-mc.player.getYaw(delta) - 90);
        Vec3d offset = new Vec3d(Math.cos(yawRad) * sideOff, yOff, Math.sin(yawRad) * sideOff);
        Vec3d mobPos = pPos.add(offset);

        float s = scale.get() * 0.4f;

        String type = petType.get();
        int bodyCol = "Лисичка".equals(type) ? 0xFFE67E22 :
                ("Эллей".equals(type) ? 0xFF00CEC9 :
                        ("Дракончик".equals(type) ? 0xFF9B59B6 : 0xFFFF7675));

        // 1. Тело фамильяра
        Render3D.drawRotatedCube(mobPos, s * 0.8, 0, yawRad, 0, bodyCol, 0xFFFFFFFF, true, true, 1.2f);

        // 2. Голова с ушками/рожками
        Vec3d headPos = mobPos.add(0, s * 0.65, 0);
        Render3D.drawRotatedCube(headPos, s * 0.55, 0, yawRad, 0, bodyCol, 0xFFFFFFFF, true, true, 1.0f);

        // 3. Крылья для Эллея и Дракончика
        if ("Эллей".equals(type) || "Дракончик".equals(type)) {
            double wingFlap = Math.sin(time * 10.0) * s * 0.6;
            Vec3d leftWing = mobPos.add(-Math.sin(yawRad) * (s * 0.6), s * 0.3 + wingFlap, Math.cos(yawRad) * (s * 0.6));
            Vec3d rightWing = mobPos.add(Math.sin(yawRad) * (s * 0.6), s * 0.3 - wingFlap, -Math.cos(yawRad) * (s * 0.6));
            int wingCol = "Эллей".equals(type) ? 0xAA81ECEC : 0xAA6C5CE7;
            Render3D.drawRotatedCube(leftWing, s * 0.35, 0, yawRad, 0.4, wingCol, 0xFFFFFFFF, true, true, 1.0f);
            Render3D.drawRotatedCube(rightWing, s * 0.35, 0, yawRad, -0.4, wingCol, 0xFFFFFFFF, true, true, 1.0f);
        }

        // 4. Тень на земле
        if (shadow.get()) {
            Render3D.drawCircle(new Vec3d(mobPos.x, pPos.y + 0.02, mobPos.z), s * 1.1, 16, 0x55000000, 1.8f);
        }
    }
}
