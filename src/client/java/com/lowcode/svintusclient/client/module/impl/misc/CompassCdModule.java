package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.util.SoundPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public final class CompassCdModule extends Module implements TickListener {

    public final BooleanSetting showTimer = add(new BooleanSetting("Таймер кулдауна", true));
    public final BooleanSetting soundOnReady = add(new BooleanSetting("Звук готовности", true));
    public final NumberSetting alertSeconds = add(new NumberSetting("Предупреждение за (сек)", 5.0f, 1.0f, 30.0f, 1.0f));
    public final NumberSetting alertVolume = add(new NumberSetting("Громкость оповещения", 0.75f, 0.1f, 1.0f, 0.05f));
    public final BooleanSetting chatAlert = add(new BooleanSetting("Оповещение в чат", false));
    public final BooleanSetting showInTooltip = add(new BooleanSetting("Показ в подсказке (КД)", true));

    private static final java.util.regex.Pattern COOLDOWN_DELAY = java.util.regex.Pattern.compile("(?i)(?:\"?delay\"?)\\s*:\\s*([0-9]{12,})[lL]?");

    private boolean wasCoolingDown = false;
    private boolean alerted = false;

    public CompassCdModule() {
        super("КД компаса", "Мониторинг серверного времени перезарядки боевого компаса HolyWorld", Category.HOLYWORLD);
    }

    public static long getCooldownRemainingMs(net.minecraft.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        var customData = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return 0L;
        String raw = customData.copyNbt().toString();
        var matcher = COOLDOWN_DELAY.matcher(raw);
        long now = System.currentTimeMillis();
        long minRemaining = Long.MAX_VALUE;
        boolean found = false;
        while (matcher.find()) {
            try {
                long targetTime = Long.parseLong(matcher.group(1));
                if (targetTime > now) {
                    long diff = targetTime - now;
                    if (diff < minRemaining) {
                        minRemaining = diff;
                        found = true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return found ? minRemaining : 0L;
    }

    public void handleTooltip(net.minecraft.item.ItemStack stack, java.util.List<net.minecraft.text.Text> lines) {
        if (!isEnabled() || !showInTooltip.get() || stack == null || stack.isEmpty()) return;
        boolean isCompass = stack.isOf(Items.COMPASS) || stack.getName().getString().toLowerCase().contains("компас");
        if (!isCompass) return;

        long remaining = getCooldownRemainingMs(stack);
        if (remaining > 0) {
            long totalSec = (remaining + 999L) / 1000L;
            long min = totalSec / 60L;
            long sec = totalSec % 60L;
            lines.add(net.minecraft.text.Text.literal(String.format("§c⏳ Перезарядка: §e%02d:%02d", min, sec)));
        } else {
            var customData = stack.get(net.minecraft.component.DataComponentTypes.CUSTOM_DATA);
            if (customData != null && (customData.copyNbt().toString().contains("region-radar") || customData.copyNbt().toString().contains("delay"))) {
                lines.add(net.minecraft.text.Text.literal("§a✨ Компас готов к использованию!"));
            }
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean isCooling = mc.player.getItemCooldownManager().isCoolingDown(Items.COMPASS.getDefaultStack());
        if (isCooling) {
            float progress = mc.player.getItemCooldownManager().getCooldownProgress(Items.COMPASS.getDefaultStack(), 0f);
            float remainingSec = progress * 30.0f;
            if (!alerted && remainingSec <= alertSeconds.get()) {
                alerted = true;
                if (soundOnReady.get()) {
                    SoundPreview.playPreview("AimBooster", alertVolume.get());
                }
            }
        } else {
            alerted = false;
        }

        if (wasCoolingDown && !isCooling) {
            if (soundOnReady.get()) {
                SoundPreview.playPreview("Bell", alertVolume.get());
                if (chatAlert.get() && mc.player != null) {
                    mc.player.sendMessage(net.minecraft.text.Text.literal("§a[HolyWorld] §fБоевой компас готов к использованию!"), true);
                }
            }
        }
        wasCoolingDown = isCooling;
    }

    public String getTimerText() {
        if (!isEnabled() || !showTimer.get()) return "";
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "";
        if (!mc.player.getItemCooldownManager().isCoolingDown(Items.COMPASS.getDefaultStack())) return "§aГотов";
        float progress = mc.player.getItemCooldownManager().getCooldownProgress(Items.COMPASS.getDefaultStack(), 0f);
        return String.format(java.util.Locale.US, "§e%.1fs", progress * 30.0f);
    }
}
