package com.lowcode.svintusclient.client.module.impl.cosmetics;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class AnimalEarsCosmeticModule extends Module implements Render3DListener {

    public final ModeSetting earType = add(new ModeSetting("Тип ушек", "Кошачьи", "Лисьи", "Эльфийские", "Заячьи"));
    public final ColorSetting outerColor = add(new ColorSetting("Цвет шерсти", 0xFF1E293B));
    public final ColorSetting innerColor = add(new ColorSetting("Внутренний цвет", 0xFFF472B6));
    public final NumberSetting scale = add(new NumberSetting("Размер", 0.35f, 0.2f, 0.8f, 0.05f));
    public final BooleanSetting twitch = add(new BooleanSetting("Анимация подёргивания", true));
    public final NumberSetting twitchSpeed = add(new NumberSetting("Частота подёргивания", 1.0f, 0.2f, 3.0f, 0.1f));

    public boolean shouldTwitch() {
        return isEnabled() && twitch.get();
    }

    public float getTwitchSpeed() {
        return isEnabled() ? twitchSpeed.get() : 1.0f;
    }

    public AnimalEarsCosmeticModule() {
        super("Animal Ears", "3D аниме ушки (кошачьи, лисьи, эльфийские) на голове персонажа", Category.COSMETICS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.player == null) return;

        PlayerEntity player = mc.player;
        float delta = tickCounter.getTickProgress(false);
        Vec3d pPos = player.getLerpedPos(delta);

        float yaw = (float) Math.toRadians(-player.getYaw(delta));
        float sz = scale.get();
        double headY = pPos.y + player.getHeight() + 0.02;

        Vec3d rightOffset = new Vec3d(Math.cos(yaw) * 0.22, headY, Math.sin(yaw) * 0.22);
        Vec3d leftOffset = new Vec3d(-Math.cos(yaw) * 0.22, headY, -Math.sin(yaw) * 0.22);

        Vec3d earRight = pPos.add(rightOffset.x, rightOffset.y - pPos.y, rightOffset.z);
        Vec3d earLeft = pPos.add(leftOffset.x, leftOffset.y - pPos.y, leftOffset.z);

        int outCol = outerColor.get();
        int inCol = innerColor.get();

        String type = earType.get();
        double heightMult = "Заячьи".equals(type) ? 1.4 : "Лисьи".equals(type) ? 1.1 : "Эльфийские".equals(type) ? 0.7 : 0.9;
        double tiltAngle = "Лисьи".equals(type) ? 0.35 : "Эльфийские".equals(type) ? -0.4 : "Заячьи".equals(type) ? 0.08 : 0.2;
        double twitch = Math.sin(System.currentTimeMillis() / 400.0) * ("Лисьи".equals(type) ? 0.035 : 0.02);

        Render3D.drawRotatedCube(earRight.add(0, sz * heightMult + twitch, 0), sz * 0.6, 0, yaw, tiltAngle, outCol, inCol, true, true, 1.2f);
        Render3D.drawRotatedCube(earLeft.add(0, sz * heightMult - twitch, 0), sz * 0.6, 0, yaw, -tiltAngle, outCol, inCol, true, true, 1.2f);
    }
}
