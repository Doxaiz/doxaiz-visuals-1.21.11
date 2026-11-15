package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render3D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public final class FriendHighlightModule extends Module implements Render3DListener {

    public final ModeSetting visualStyle = add(new ModeSetting("Стиль подсветки", "Зеленый круг под ногами", "Неоновый столб", "Двойное кольцо"));
    public final ColorSetting friendColor = add(new ColorSetting("Цвет друзей", 0xFF10B981));
    public final NumberSetting ringRadius = add(new NumberSetting("Радиус круга", 0.7f, 0.4f, 1.5f, 0.05f));
    public final BooleanSetting showFriendTag = add(new BooleanSetting("Метка над головой", true));
    public final BooleanSetting glowingOutline = add(new BooleanSetting("Неоновая обводка", true));
    public final BooleanSetting customCape = add(new BooleanSetting("Плащ союзника", true));
    public final BooleanSetting middleClickAdd = add(new BooleanSetting("Добавлять на СКМ", true));
    public final BooleanSetting includeTeammates = add(new BooleanSetting("Союзники по команде", true));

    private static final java.util.Set<String> FRIENDS = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().keySet(true);

    public boolean shouldShowFriendTag() {
        return isEnabled() && showFriendTag.get();
    }

    public boolean hasGlowingOutline() {
        return isEnabled() && glowingOutline.get();
    }

    public boolean hasCustomCape() {
        return isEnabled() && customCape.get();
    }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        return FRIENDS.contains(name.toLowerCase(java.util.Locale.ROOT));
    }

    public static void addFriend(String name) {
        if (name != null) {
            FRIENDS.add(name.toLowerCase(java.util.Locale.ROOT));
            com.lowcode.svintusclient.client.Config.save();
        }
    }

    public static void removeFriend(String name) {
        if (name != null) {
            FRIENDS.remove(name.toLowerCase(java.util.Locale.ROOT));
            com.lowcode.svintusclient.client.Config.save();
        }
    }

    public static boolean toggleFriend(String name) {
        if (name == null || name.isBlank()) return false;
        String low = name.toLowerCase(java.util.Locale.ROOT);
        boolean added;
        if (FRIENDS.contains(low)) {
            FRIENDS.remove(low);
            added = false;
        } else {
            FRIENDS.add(low);
            added = true;
        }
        com.lowcode.svintusclient.client.Config.save();
        return added;
    }

    public static java.util.Set<String> getFriends() {
        return FRIENDS;
    }

    public FriendHighlightModule() {
        super("Friend Highlight", "Визуальная подсветка союзников и друзей в мире", Category.FRIENDS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isEnabled() || mc.world == null || mc.player == null) return;

        float delta = tickCounter.getTickProgress(false);
        int col = friendColor.get();
        float r = ringRadius.get();

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isInvisible()) continue;

            boolean isFr = isFriend(player.getNameForScoreboard()) || isFriend(player.getName().getString());
            boolean isAlly = isFr || (includeTeammates.get() && mc.player.isTeammate(player));
            if (!isAlly) continue;

            Vec3d pos = player.getLerpedPos(delta);
            double distSq = mc.player.squaredDistanceTo(player);
            if (distSq > 1600.0) continue; // max 40 blocks

            Vec3d ground = new Vec3d(pos.x, pos.y + 0.02, pos.z);
            Render3D.drawCircle(ground, r, 28, col, 2.0f);

            if ("Двойное кольцо".equals(visualStyle.get())) {
                Render3D.drawCircle(ground.add(0, 0.15, 0), r * 0.85, 24, (col & 0x00FFFFFF) | 0x88000000, 1.5f);
            } else if ("Неоновый столб".equals(visualStyle.get())) {
                Render3D.drawLine(ground, ground.add(0, player.getHeight(), 0), col, 2.0f);
            }

            if (showFriendTag.get()) {
                matrices.push();
                double tagY = pos.y + player.getHeight() + 0.35;
                matrices.translate(pos.x - camera.getCameraPos().x, tagY - camera.getCameraPos().y, pos.z - camera.getCameraPos().z);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.scale(-0.02f, -0.02f, 0.02f);
                String tagText = isFr ? "§e★ §aДРУГ" : "§a🛡 СОЮЗНИК";
                int tw = mc.textRenderer.getWidth(tagText);
                mc.textRenderer.draw(tagText, -tw / 2.0f, 0, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), consumers, net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0x80000000, 0xF000F0);
                matrices.pop();
            }
        }
    }
}
