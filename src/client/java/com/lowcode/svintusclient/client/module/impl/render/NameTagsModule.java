package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudDraw;
import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.impl.render.StreamerModeModule;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.render.Render2D;
import com.lowcode.svintusclient.client.render.font.Fonts;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class NameTagsModule extends Module implements Render3DListener {

    public final BooleanSetting healthBar = add(new BooleanSetting("Полоса здоровья", true));
    public final BooleanSetting armor = add(new BooleanSetting("Отображать броню", true));
    public final BooleanSetting distance = add(new BooleanSetting("Дистанция (м)", true));
    public final BooleanSetting ping = add(new BooleanSetting("Числовой пинг (мс)", true));
    public final BooleanSetting background = add(new BooleanSetting("Акриловый фон", true));
    public final BooleanSetting lineOfSight = add(new BooleanSetting("Только в прямой видимости", false));
    public final NumberSetting scale = add(new NumberSetting("Масштаб", 1.0f, 0.6f, 1.8f, 0.1f));
    public final ModeSetting colorMode = add(new ModeSetting("Режим цвета",
            "Градиент здоровья", "Акцент темы", "Радуга", "Белый"));

    // Self Nametag (F5) integration
    public final BooleanSetting selfNametag = add(new BooleanSetting("Свой ник в F5", true));
    public final ModeSetting badgeStyle = add(new ModeSetting("Стиль бейджа", "Стекло (Glass)", "Минимализм", "Неон"));

    public NameTagsModule() {
        super("Name Tags", "Стильные 3D никнеймы игроков и собственный ник в F5: HP, броня, пинг и дистанция", Category.VISUALS);
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Vec3d camPos = camera.getCameraPos();

        // 1. Other players
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            if (!p.isAlive() || p.isSpectator()) continue;
            if (lineOfSight.get() && !mc.player.canSee(p)) continue;

            double dx = p.getX() - camPos.x;
            double dy = (p.getY() + p.getHeight() + 0.5) - camPos.y;
            double dz = p.getZ() - camPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 64.0 || dist < 0.5) continue;

            matrices.push();
            matrices.translate(dx, dy, dz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float sc = (float) (0.015 * Math.max(1.0, dist * 0.18) * scale.get());
            matrices.scale(-sc, -sc, sc);

            renderTag(matrices, p, (float) dist, consumers);

            matrices.pop();
        }

        // 2. Self nametag in third-person (F5)
        if (selfNametag.get() && !mc.options.getPerspective().isFirstPerson()) {
            PlayerEntity player = mc.player;
            float delta = tickCounter.getTickProgress(false);
            Vec3d playerPos = player.getLerpedPos(delta);

            String name = StreamerModeModule.filter(player.getNameForScoreboard());
            StringBuilder sb = new StringBuilder();
            sb.append(name);

            float hp = player.getHealth() + player.getAbsorptionAmount();
            sb.append(String.format(" §c[%.1f HP]", hp));

            if (ping.get() && mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry != null) {
                    int pVal = entry.getLatency();
                    String pCol = pVal < 60 ? "§a" : pVal < 130 ? "§e" : "§c";
                    sb.append(" ").append(pCol).append(pVal).append("ms");
                }
            }

            String label = sb.toString();
            TextRenderer textRenderer = mc.textRenderer;

            matrices.push();
            double targetY = playerPos.y + player.getHeight() + 0.35;
            matrices.translate(playerPos.x - camPos.x, targetY - camPos.y, playerPos.z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float textScale = 0.025f * scale.get();
            matrices.scale(-textScale, -textScale, textScale);

            int width = textRenderer.getWidth(label);
            int bg = 0;
            if (background.get()) {
                String bs = badgeStyle.get();
                if ("Неон".equals(bs)) bg = 0xAA0A192F;
                else if ("Минимализм".equals(bs)) bg = 0x44000000;
                else bg = 0x88111422;
            }

            textRenderer.draw(label, -width / 2.0f, 0, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.SEE_THROUGH, bg, 0xF000F0);
            textRenderer.draw(label, -width / 2.0f, 0, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);

            matrices.pop();
        }
    }

    private void renderTag(MatrixStack matrices, PlayerEntity p, float dist, VertexConsumerProvider consumers) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = StreamerModeModule.filter(p.getName().getString());
        if (FriendHighlightModule.isFriend(p.getName().getString())) {
            name = "§e★ " + name;
        }
        if (distance.get()) {
            name += " §7" + String.format("%.0fm", dist);
        }

        int pingVal = 0;
        if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            if (entry != null) pingVal = entry.getLatency();
        }
        if (ping.get()) {
            String pCol = pingVal < 60 ? "§a" : pingVal < 130 ? "§e" : "§c";
            name += " " + pCol + pingVal + "ms";
        }

        float hp = p.getHealth() + p.getAbsorptionAmount();
        float maxHp = Math.max(1.0f, p.getMaxHealth() + p.getAbsorptionAmount());
        float ratio = Math.max(0.0f, Math.min(1.0f, hp / maxHp));

        String hpCol = "§c";
        String cm = colorMode.get();
        if ("Градиент здоровья".equals(cm)) {
            hpCol = ratio > 0.75f ? "§a" : ratio > 0.5f ? "§2" : ratio > 0.25f ? "§e" : "§c";
        } else if ("Акцент темы".equals(cm)) {
            hpCol = "§b";
        } else if ("Радуга".equals(cm)) {
            hpCol = "§d";
        } else if ("Белый".equals(cm)) {
            hpCol = "§f";
        }

        String hpStr = " " + hpCol + String.format("%.0f", hp) + "HP";

        // Armor summary badge
        if (armor.get()) {
            ItemStack chest = p.getEquippedStack(EquipmentSlot.CHEST);
            if (!chest.isEmpty()) {
                String itemKey = chest.getItem().toString().toLowerCase();
                if (itemKey.contains("netherite")) hpStr += " §8[Netherite]";
                else if (itemKey.contains("diamond")) hpStr += " §b[Diamond]";
                else if (itemKey.contains("iron")) hpStr += " §f[Iron]";
                else if (itemKey.contains("elytra")) hpStr += " §e[Elytra]";
            }
            ItemStack main = p.getMainHandStack();
            if (!main.isEmpty()) {
                hpStr += " §7[" + main.getName().getString() + "]";
            }
            ItemStack off = p.getOffHandStack();
            if (!off.isEmpty()) {
                if (off.isOf(net.minecraft.item.Items.TOTEM_OF_UNDYING)) {
                    hpStr += " §6[Тотем]";
                } else if (off.isOf(net.minecraft.item.Items.SHIELD)) {
                    hpStr += " §9[Щит]";
                }
            }
        }

        String fullStr = name + hpStr;
        int textW = mc.textRenderer.getWidth(fullStr);
        int bg = 0;
        if (background.get()) {
            String bs = badgeStyle.get();
            if ("Неон".equals(bs)) bg = 0xAA0A192F;
            else if ("Минимализм".equals(bs)) bg = 0x44000000;
            else bg = 0x88111422;
        }

        mc.textRenderer.draw(fullStr, -textW / 2.0f, 0, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.SEE_THROUGH, bg, 0xF000F0);
        mc.textRenderer.draw(fullStr, -textW / 2.0f, 0, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);

        // Visual health bar below tag
        if (healthBar.get()) {
            StringBuilder bar = new StringBuilder();
            int total = 12;
            int filled = Math.max(1, (int) (ratio * total));
            bar.append(hpCol);
            for (int i = 0; i < filled; i++) bar.append("■");
            bar.append("§8");
            for (int i = filled; i < total; i++) bar.append("■");
            String barStr = bar.toString();
            int barW = mc.textRenderer.getWidth(barStr);
            mc.textRenderer.draw(barStr, -barW / 2.0f, 10, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.SEE_THROUGH, bg, 0xF000F0);
            mc.textRenderer.draw(barStr, -barW / 2.0f, 10, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        }
    }
}
