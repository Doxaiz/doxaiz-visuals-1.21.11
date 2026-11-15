package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class SpawnerContentsModule extends Module implements Render3DListener {

    public final NumberSetting range = add(new NumberSetting("Дальность (блоков)", 24.0f, 4.0f, 64.0f, 4.0f));
    public final BooleanSetting showName = add(new BooleanSetting("Название моба", true));
    public final BooleanSetting showTier = add(new BooleanSetting("Уровень спавнера", true));
    public final BooleanSetting boxHighlight = add(new BooleanSetting("3D контур блока", true));
    public final NumberSetting textScale = add(new NumberSetting("Масштаб текста", 1.0f, 0.5f, 2.0f, 0.1f));

    // Показ ЗСП (Золотой спавнер HolyWorld)
    public final BooleanSetting showTooltip = add(new BooleanSetting("Показ в подсказке (ЗСП)", true));
    public final BooleanSetting showFarmerSword = add(new BooleanSetting("Меч в фармере", true));
    public final BooleanSetting showKills = add(new BooleanSetting("Убийств до исчерпания", true));

    public SpawnerContentsModule() {
        super("Spawner Contents", "Отображает тип моба, уровень над блоками и детальные данные ЗСП на HolyWorld", Category.HOLYWORLD);
    }

    public void handleTooltip(net.minecraft.item.ItemStack stack, java.util.List<net.minecraft.text.Text> lines) {
        if (!isEnabled() || !showTooltip.get() || stack == null || stack.isEmpty()) return;
        boolean isSpawner = stack.isOf(net.minecraft.item.Items.SPAWNER) || stack.getName().getString().toLowerCase().contains("спавнер");
        if (!isSpawner) return;

        var customData = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            net.minecraft.nbt.NbtCompound nbt = customData.copyNbt();
            if (nbt.contains("gs_marker") || nbt.contains("gs_mob_type") || nbt.contains("gs_sword_item")) {
                lines.add(net.minecraft.text.Text.literal("§6✨ [Золотой Спавнер HolyWorld]"));
                if (nbt.contains("gs_mob_type")) {
                    String mob = nbt.getString("gs_mob_type").orElse("");
                    if (!mob.isEmpty()) {
                        lines.add(net.minecraft.text.Text.literal(" §eМоб: §f" + formatMobName(mob)));
                    }
                }
                if (showKills.get() && nbt.contains("gs_kills_remaining")) {
                    long rem = nbt.getLong("gs_kills_remaining").orElse(0L);
                    long total = nbt.contains("gs_kills_total") ? nbt.getLong("gs_kills_total").orElse(0L) : 0L;
                    if (total > 0) {
                        lines.add(net.minecraft.text.Text.literal(" §b⚔ Убийств: §f" + rem + " / " + total));
                    } else {
                        lines.add(net.minecraft.text.Text.literal(" §b⚔ Убийств осталось: §f" + rem));
                    }
                }
                if (nbt.contains("gs_total_eggs")) {
                    lines.add(net.minecraft.text.Text.literal(" §d🥚 Яиц мобов: §f" + nbt.getInt("gs_total_eggs").orElse(0)));
                }
                if (showFarmerSword.get() && nbt.contains("gs_sword_item")) {
                    String sword = nbt.getString("gs_sword_item").orElse("");
                    if (!sword.isEmpty()) {
                        lines.add(net.minecraft.text.Text.literal(" §a🗡 Меч: §f" + formatSwordInfo(sword)));
                    }
                }
            }
        }
    }

    private static String formatMobName(String raw) {
        String key = raw.toLowerCase().replace("minecraft:", "");
        return switch (key) {
            case "zombie" -> "Зомби";
            case "skeleton" -> "Скелет";
            case "blaze" -> "Ифрит";
            case "creeper" -> "Крипер";
            case "spider" -> "Паук";
            case "cave_spider" -> "Пещерный паук";
            case "iron_golem" -> "Железный голем";
            case "enderman" -> "Эндермен";
            case "piglin" -> "Пиглин";
            case "zombified_piglin" -> "Зомби-пиглин";
            case "witch" -> "Ведьма";
            case "cow" -> "Корова";
            case "pig" -> "Свинья";
            case "sheep" -> "Овца";
            default -> raw;
        };
    }

    private static String formatSwordInfo(String raw) {
        if (raw.contains("name")) {
            try {
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
                if (obj.has("name")) {
                    return obj.get("name").getAsString();
                }
            } catch (Exception ignored) {}
        }
        return raw.replace("{", "").replace("}", "").replace("\"", "");
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.get().intValue();
        int chunkRadius = (r >> 4) + 1;
        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;
        Vec3d camPos = camera.getCameraPos();

        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                if (!mc.world.isChunkLoaded(cx, cz)) continue;
                for (BlockEntity be : mc.world.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof MobSpawnerBlockEntity spawner) {
                        BlockPos pos = spawner.getPos();
                        double dx = (pos.getX() + 0.5) - camPos.x;
                        double dy = (pos.getY() + 1.25) - camPos.y;
                        double dz = (pos.getZ() + 0.5) - camPos.z;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq > r * r) continue;

                        Entity entity = spawner.getLogic().getRenderedEntity(mc.world, pos);
                        String mobName = entity != null ? entity.getType().getName().getString() : "Спавнер";

                        matrices.push();
                        matrices.translate(dx, dy, dz);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                        float sc = -0.025f * textScale.get();
                        matrices.scale(sc, sc, -sc);

                        String label = "§6[★]";
                        if (showName.get()) {
                            label += " §e" + mobName;
                        }
                        if (showTier.get()) {
                            label += " §7(I-ур)";
                        }
                        int width = mc.textRenderer.getWidth(label);
                        mc.textRenderer.draw(label, -width / 2.0f, 0, 0xFFFFFFFF, false,
                                matrices.peek().getPositionMatrix(), consumers,
                                net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH,
                                0x88000000, 0xF000F0);

                        matrices.pop();
                    }
                }
            }
        }
    }
}
