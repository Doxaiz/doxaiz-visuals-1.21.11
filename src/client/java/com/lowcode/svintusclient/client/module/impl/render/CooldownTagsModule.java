package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.Render3DListener;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CooldownTagsModule extends Module implements Render3DListener, TickListener {

    public final BooleanSetting showPearl = add(new BooleanSetting("Кулдаун Эндер-перла", true));
    public final BooleanSetting showGapple = add(new BooleanSetting("Кулдаун Золотого яблока", true));
    public final BooleanSetting showChorus = add(new BooleanSetting("Кулдаун Хоруса", true));
    public final BooleanSetting showWindCharge = add(new BooleanSetting("Кулдаун Винд-чарджа (1.21)", true));
    public final BooleanSetting showShield = add(new BooleanSetting("Кулдаун Щита (Disable)", true));
    public final BooleanSetting showTotem = add(new BooleanSetting("Кулдаун Тотема", true));
    public final BooleanSetting showGoatHorn = add(new BooleanSetting("Кулдаун Рога", true));
    public final BooleanSetting showCompass = add(new BooleanSetting("Кулдаун компаса HolyWorld", true));
    public final BooleanSetting showOtherItems = add(new BooleanSetting("Все остальные предметы с КД", true));
    public final BooleanSetting showTimer = add(new BooleanSetting("Таймер в секундах", true));
    public final BooleanSetting showReady = add(new BooleanSetting("Показывать статус готовности (✔)", false));
    public final BooleanSetting selfTag = add(new BooleanSetting("Отображать над собой в F5", true));
    public final BooleanSetting lineOfSight = add(new BooleanSetting("Только в прямой видимости", false));
    public final NumberSetting maxDistance = add(new NumberSetting("Дистанция (м)", 36.0f, 10.0f, 64.0f, 2.0f));
    public final NumberSetting scale = add(new NumberSetting("Масштаб", 1.0f, 0.5f, 2.0f, 0.1f));
    public final BooleanSetting compassSoundAlert = add(new BooleanSetting("Звуковой сигнал готовности компаса", true));

    public record CooldownEntry(String name, String colorPrefix, long expireTimeMs) {
        public boolean isAlive(long now) {
            return now < expireTimeMs;
        }

        public float remainingSeconds(long now) {
            return Math.max(0.0f, (expireTimeMs - now) / 1000.0f);
        }
    }

    private final Map<UUID, Map<String, CooldownEntry>> cooldowns = new HashMap<>();
    private boolean wasCompassCd = false;

    public CooldownTagsModule() {
        super("Cooldown Tags", "Отображает плавающие иконки кулдаунов (все предметы с перезарядкой) над игроками", Category.BATTLE);
    }

    public void addCooldown(UUID playerUuid, String key, String name, String colorPrefix, long durationMs) {
        if (playerUuid == null) return;
        long expireTime = System.currentTimeMillis() + durationMs;
        cooldowns.computeIfAbsent(playerUuid, k -> new HashMap<>())
                 .put(key, new CooldownEntry(name, colorPrefix, expireTime));
    }

    public void onShieldDisabled(LivingEntity entity) {
        if (!isEnabled() || !showShield.get() || !(entity instanceof PlayerEntity player)) return;
        addCooldown(player.getUuid(), "shield", "Щит", "§c", 5000L);
    }

    public void onTotemPop(LivingEntity entity) {
        if (!isEnabled() || !showTotem.get() || !(entity instanceof PlayerEntity player)) return;
        addCooldown(player.getUuid(), "totem", "Тотем", "§e", 15000L);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            cooldowns.clear();
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Detect thrown Ender Pearls
        if (showPearl.get()) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof EnderPearlEntity pearlEntity && pearlEntity.age <= 2) {
                    Entity owner = pearlEntity.getOwner();
                    if (owner instanceof PlayerEntity ple) {
                        addCooldown(ple.getUuid(), "pearl", "Перл", "§c", 15000L);
                    } else {
                        PlayerEntity closest = findClosestPlayer(pearlEntity, 16.0);
                        if (closest != null) {
                            addCooldown(closest.getUuid(), "pearl", "Перл", "§c", 15000L);
                        }
                    }
                }
            }
        }

        // 2. Detect thrown Wind Charges (1.21)
        if (showWindCharge.get()) {
            for (Entity e : mc.world.getEntities()) {
                if (e instanceof AbstractWindChargeEntity windEntity && windEntity.age <= 2) {
                    Entity owner = windEntity.getOwner();
                    if (owner instanceof PlayerEntity ple) {
                        addCooldown(ple.getUuid(), "wind_charge", "Виндчардж", "§f", 1000L);
                    } else {
                        PlayerEntity closest = findClosestPlayer(windEntity, 16.0);
                        if (closest != null) {
                            addCooldown(closest.getUuid(), "wind_charge", "Виндчардж", "§f", 1000L);
                        }
                    }
                }
            }
        }

        // 3. Detect eating / using items (Gapple, Chorus, Horn)
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p.isUsingItem()) {
                ItemStack active = p.getActiveItem();
                if (showGapple.get() && (active.isOf(Items.GOLDEN_APPLE) || active.isOf(Items.ENCHANTED_GOLDEN_APPLE))) {
                    if (p.getItemUseTime() >= 30) {
                        long cd = active.isOf(Items.ENCHANTED_GOLDEN_APPLE) ? 30000L : 20000L;
                        String name = active.isOf(Items.ENCHANTED_GOLDEN_APPLE) ? "Нотч" : "Яблоко";
                        addCooldown(p.getUuid(), "gapple", name, "§6", cd);
                    }
                } else if (showChorus.get() && active.isOf(Items.CHORUS_FRUIT)) {
                    if (p.getItemUseTime() >= 30) {
                        addCooldown(p.getUuid(), "chorus", "Хорус", "§d", 2500L);
                    }
                } else if (showGoatHorn.get() && active.isOf(Items.GOAT_HORN)) {
                    if (p.getItemUseTime() >= 10) {
                        addCooldown(p.getUuid(), "goat_horn", "Рог", "§b", 7000L);
                    }
                }
            }
        }

        // 4. Sync client player's active cooldowns from vanilla ItemCooldownManager
        ItemCooldownManager clientCd = mc.player.getItemCooldownManager();
        UUID selfUuid = mc.player.getUuid();

        syncClientItemCooldown(clientCd, selfUuid, Items.ENDER_PEARL, "pearl", "Перл", "§c", showPearl.get());
        syncClientItemCooldown(clientCd, selfUuid, Items.GOLDEN_APPLE, "gapple", "Яблоко", "§6", showGapple.get());
        syncClientItemCooldown(clientCd, selfUuid, Items.ENCHANTED_GOLDEN_APPLE, "gapple_notch", "Нотч", "§6", showGapple.get());
        syncClientItemCooldown(clientCd, selfUuid, Items.CHORUS_FRUIT, "chorus", "Хорус", "§d", showChorus.get());
        syncClientItemCooldown(clientCd, selfUuid, Items.WIND_CHARGE, "wind_charge", "Виндчардж", "§f", showWindCharge.get());
        syncClientItemCooldown(clientCd, selfUuid, Items.SHIELD, "shield", "Щит", "§c", showShield.get());
        syncClientItemCooldown(clientCd, selfUuid, Items.GOAT_HORN, "goat_horn", "Рог", "§b", showGoatHorn.get());
        syncClientItemCooldown(clientCd, selfUuid, Items.MACE, "mace", "Булава", "§5", showOtherItems.get());

        // HolyWorld Compass
        boolean isCompassOnCd = clientCd.isCoolingDown(Items.COMPASS.getDefaultStack());
        if (showCompass.get() && isCompassOnCd) {
            float prog = clientCd.getCooldownProgress(Items.COMPASS.getDefaultStack(), 0f);
            long remainMs = (long) (prog * 30000.0f);
            if (remainMs > 0) {
                addCooldown(selfUuid, "compass", "Компас", "§b", remainMs);
            }
        }
        if (wasCompassCd && !isCompassOnCd && compassSoundAlert.get()) {
            mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.8f, 1.5f);
        }
        wasCompassCd = isCompassOnCd;

        // 5. Cleanup expired cooldown entries
        Iterator<Map.Entry<UUID, Map<String, CooldownEntry>>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Map<String, CooldownEntry>> entry = it.next();
            entry.getValue().entrySet().removeIf(e -> !e.getValue().isAlive(now));
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
    }

    private void syncClientItemCooldown(ItemCooldownManager cd, UUID uuid, Item item, String key, String name, String colorPrefix, boolean enabled) {
        if (!enabled) return;
        ItemStack stack = item.getDefaultStack();
        if (cd.isCoolingDown(stack)) {
            float progress = cd.getCooldownProgress(stack, 0f);
            long approxRemainMs = (long) (progress * 15000.0f); // dynamic progress projection
            if (approxRemainMs > 100) {
                cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                         .put(key, new CooldownEntry(name, colorPrefix, System.currentTimeMillis() + approxRemainMs));
            }
        }
    }

    private PlayerEntity findClosestPlayer(Entity entity, double maxDist) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;
        PlayerEntity closest = null;
        double minDistSq = maxDist * maxDist;
        for (PlayerEntity p : mc.world.getPlayers()) {
            double dSq = p.squaredDistanceTo(entity);
            if (dSq < minDistSq) {
                minDistSq = dSq;
                closest = p;
            }
        }
        return closest;
    }

    @Override
    public void onRender3D(MatrixStack matrices, VertexConsumerProvider.Immediate consumers, Camera camera, RenderTickCounter tickCounter) {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || camera == null) return;

        Vec3d camPos = camera.getCameraPos();
        long now = System.currentTimeMillis();
        float distLimit = maxDistance.get();
        float userScale = scale.get();

        for (PlayerEntity p : mc.world.getPlayers()) {
            boolean isSelf = p == mc.player;
            if (isSelf && (!selfTag.get() || mc.options.getPerspective().isFirstPerson())) {
                continue;
            }
            if (!p.isAlive() || p.isSpectator()) continue;
            if (lineOfSight.get() && !mc.player.canSee(p)) continue;

            double dx = p.getX() - camPos.x;
            double dy = (p.getY() + p.getHeight() + 0.85) - camPos.y;
            double dz = p.getZ() - camPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > distLimit || dist < 0.5) continue;

            Map<String, CooldownEntry> pCd = cooldowns.get(p.getUuid());
            StringBuilder sb = new StringBuilder();

            if (pCd != null && !pCd.isEmpty()) {
                for (CooldownEntry cd : pCd.values()) {
                    float remainSec = cd.remainingSeconds(now);
                    if (remainSec > 0.05f) {
                        String timeStr = showTimer.get() ? String.format(Locale.US, "%.1fs", remainSec) : "⏳";
                        sb.append(cd.colorPrefix).append("[").append(cd.name).append(" ").append(timeStr).append("]§r ");
                    }
                }
            }

            if (showReady.get() && sb.length() == 0 && !isSelf) {
                sb.append("§a[Готов ✔] ");
            }

            if (sb.length() == 0) continue;

            matrices.push();
            matrices.translate(dx, dy, dz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            float sc = (float) (0.015 * Math.max(1.0, dist * 0.15)) * userScale;
            matrices.scale(-sc, -sc, sc);

            String tag = sb.toString().trim();
            float w = mc.textRenderer.getWidth(tag);
            VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
            mc.textRenderer.draw(tag, -w / 2f, 0f, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vcp,
                    net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0x88111422, 0xF000F0);
            mc.textRenderer.draw(tag, -w / 2f, 0f, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vcp,
                    net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);

            matrices.pop();
        }
    }

    @Override
    protected void onDisable() {
        cooldowns.clear();
    }
}
