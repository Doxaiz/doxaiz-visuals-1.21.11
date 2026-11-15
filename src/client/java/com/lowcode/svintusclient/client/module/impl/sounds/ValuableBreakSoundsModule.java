package com.lowcode.svintusclient.client.module.impl.sounds;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.util.SoundPreview;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public final class ValuableBreakSoundsModule extends Module {

    public final ModeSetting sound = add(new ModeSetting("Звук разрушения",
            "Metallic", "Колокольчик", "Magic Pok", "Trinket", "8-бит", "AimBooster", "Стекло", "Soup Pop"));
    public final NumberSetting volume = add(new NumberSetting("Громкость", 0.9f, 0.1f, 1.0f, 0.05f));
    public final NumberSetting pitch = add(new NumberSetting("Тон (Pitch)", 1.0f, 0.5f, 1.5f, 0.05f));

    // Block toggles
    public final BooleanSetting diamondBlock = add(new BooleanSetting("Алмазный блок", true));
    public final BooleanSetting emeraldBlock = add(new BooleanSetting("Изумрудный блок", true));
    public final BooleanSetting netheriteBlock = add(new BooleanSetting("Незеритовый блок", true));
    public final BooleanSetting ancientDebris = add(new BooleanSetting("Древние обломки", true));
    public final BooleanSetting goldBlock = add(new BooleanSetting("Золотой блок", true));
    public final BooleanSetting diamondOre = add(new BooleanSetting("Алмазные руды", true));
    public final BooleanSetting otherOres = add(new BooleanSetting("Золотые / Изумрудные руды", false));

    private long lastPlayTime = 0;

    public ValuableBreakSoundsModule() {
        super("Valuable Break Sounds", "Кастомные звуки разрушения ценных руд и блоков (алмазы, незерит, обломки)", Category.SOUNDS);
    }

    public void onBlockBreak(Block block) {
        if (!isEnabled() || block == null) return;

        boolean matches = false;
        if (diamondBlock.get() && block == Blocks.DIAMOND_BLOCK) matches = true;
        else if (emeraldBlock.get() && block == Blocks.EMERALD_BLOCK) matches = true;
        else if (netheriteBlock.get() && block == Blocks.NETHERITE_BLOCK) matches = true;
        else if (ancientDebris.get() && block == Blocks.ANCIENT_DEBRIS) matches = true;
        else if (goldBlock.get() && block == Blocks.GOLD_BLOCK) matches = true;
        else if (diamondOre.get() && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) matches = true;
        else if (otherOres.get() && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE || block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE)) matches = true;

        if (matches) {
            long now = System.currentTimeMillis();
            if (now - lastPlayTime < 50L) return;
            lastPlayTime = now;
            SoundPreview.play(sound.get(), volume.get(), pitch.get());
        }
    }
}
