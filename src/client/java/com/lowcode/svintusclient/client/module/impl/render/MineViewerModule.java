package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

public final class MineViewerModule extends Module {

    public final BooleanSetting showToast = add(new BooleanSetting("Уведомление о добыче", true));
    public final BooleanSetting soundAlert = add(new BooleanSetting("Звук ценной руды", true));
    public final ModeSetting minRarity = add(new ModeSetting("Мин. редкость", "Железо и выше", "Все руды", "Только алмазы и незерит"));

    public MineViewerModule() {
        super("Шахты", "Мониторинг статуса шахт и звуковые уведомления о добыче ценной руды", Category.HOLYWORLD);
    }

    public void onOreMined(Block block) {
        if (!isEnabled() || block == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        boolean isValuable = false;
        String oreName = "";

        if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) {
            isValuable = true;
            oreName = "Алмазная руда";
        } else if (block == Blocks.ANCIENT_DEBRIS) {
            isValuable = true;
            oreName = "Древние обломки";
        } else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) {
            isValuable = true;
            oreName = "Изумрудная руда";
        } else if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) {
            if (!"Только алмазы и незерит".equals(minRarity.get())) {
                isValuable = true;
                oreName = "Золотая руда";
            }
        } else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            if ("Все руды".equals(minRarity.get()) || "Железо и выше".equals(minRarity.get())) {
                isValuable = true;
                oreName = "Железная руда";
            }
        }

        if (isValuable) {
            if (showToast.get()) {
                com.lowcode.svintusclient.client.hud.Notifications.push(oreName, true);
            }
            if (soundAlert.get() && mc.player != null) {
                mc.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f, 1.8f);
            }
        }
    }
}
