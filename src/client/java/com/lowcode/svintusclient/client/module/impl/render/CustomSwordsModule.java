package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

public final class CustomSwordsModule extends Module {

    public static final String[] WEAPONS = new String[]{
        "Стандарт", "Katana", "Abominable Blade", "Abominable Great Saber", "Abominable Scythe", "Acidic Cleaver", "Amethyst Shuriken",
        "Ancient Royal Great Sword", "Aquatic Sacred Blade", "Arcanethyst", "Ashura's Blade", "Awakened Lichblade",
        "Blood Edge", "Bloody Death", "Bramblethorn", "Brimstone Claymore", "Carian Knight's Sword", "Chrono Blade",
        "Corrupted Mythic Blade", "Creation Splitter", "Crescent Rose", "Cyber Katana", "Cyber Mantis Blade",
        "Cyber Sword", "Cybernetic Chainsaw Blade", "Cybernetic Katana", "Cybernetic Knife", "Dainsleif", "Dark Blade",
        "Dark Cleaver", "Death Knight's Dagger", "Death Knight's Sword", "Demigod's Unholy Blade", "Demigod's Unholy Halberd",
        "Demon Lord's Great Axe", "Demon Lord's Sword", "Demonic Blade", "Demonic Cleaver", "Divine Axe Rhitta",
        "Divine Justice", "Divine Punisher", "Divine Reaper", "Dragon Slaying blade", "Edge Of The Astral Plane",
        "Emberblade", "Enigma", "Epic Sword", "Estoc", "Fallen God's Spear", "Fallen God's Sword", "Floral Longsword",
        "Floral Sabre", "Forest Guardian's Glaive", "Frost Axe", "Frost Blade", "Frost Scythe", "Hearthflame",
        "Hero Sword", "Holy Moonlight Sword", "Hornet's Needle", "Icewhisper", "Jade Halberd",
        "Legendary Sword", "Longsword", "Magi Scythe", "Masamune", "Mjolnir", "Molten Blade", "Molten Sword",
        "Muramasa", "Mystical Spellblade", "Mythic Blade", "Ocean's Rage", "Partisan", "Pharaoh's Treasure",
        "Pheonix Grace", "Plague Longsword", "Power Fuse Hammer", "Power Fuse Sword", "Requiem of the Ninth Abyss",
        "Ribbon Cleaver", "Righteous Relic", "Rivers Of Blood", "Royal Chakram", "Royal Rapier", "Sabre",
        "Scissor Blade", "Sculk Cleaver", "Sculk Scythe", "Sculk Sword", "Sentinel's Will", "Silverine Blade",
        "Soul Claws", "Soul Collector", "Soul Devourer", "Soul Edge", "Soul Harvester", "Soul Stealer",
        "Soulrender", "Star's Edge", "Steel Sword", "Stop Sign", "Storm Bringer", "Storm's Edge", "Sunbreak",
        "Tengen's Blade", "Terra Blade", "Thousand Demon Daggers", "Thunder Bringer", "Thunderbrand", "True Excalibur",
        "Vampiric Needle", "Wakizashi", "Watcher Claymore", "Watching Warglaive", "Waxweaver", "Whisperwind",
        "Wickpiercer", "Wraith Scythe", "Yoru"
    };

    public final ModeSetting swordModel = add(new ModeSetting("Модель меча", WEAPONS));
    public final BooleanSetting selfOnly = add(new BooleanSetting("Только у себя", true));
    public final BooleanSetting bladeGlow = add(new BooleanSetting("Свечение лезвия", true));
    public final ColorSetting glowColor = add(new ColorSetting("Цвет свечения", 0xFF00CEC9));
    public final NumberSetting scale = add(new NumberSetting("Масштаб клинка", 1.0f, 0.6f, 1.5f, 0.05f));

    public CustomSwordsModule() {
        super("Custom Swords", "Кастомный 3D визуал мечей в руках (Катана, Коса, Кинжал, Палаш)", Category.COSMETICS);
    }

    public String getSelectedWeapon() {
        return swordModel.get();
    }

    public boolean hasBladeGlow() {
        return isEnabled() && bladeGlow.get();
    }

    public int getBladeGlowColor() {
        return glowColor.getEffectiveColor();
    }

    public void transformSword(MatrixStack matrices, Arm arm, ItemStack item) {
        if (!isEnabled() || item == null || !item.isIn(ItemTags.SWORDS)) return;

        float s = scale.get();
        if (Math.abs(s - 1.0f) > 0.01f) {
            matrices.scale(s, s, s);
        }

        String m = swordModel.get();
        if (m.contains("Katana") || m.contains("blade") || m.contains("Blade")) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm == Arm.RIGHT ? 12.0f : -12.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm == Arm.RIGHT ? -8.0f : 8.0f));
            matrices.translate(0.0, 0.015, -0.04);
        } else if (m.contains("Scythe") || m.contains("Axe") || m.contains("Cleaver")) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(arm == Arm.RIGHT ? 20.0f : -20.0f));
            matrices.scale(1.1f, 1.1f, 1.1f);
        } else if (m.contains("Dagger") || m.contains("Knife") || m.contains("Shuriken")) {
            matrices.scale(0.85f, 0.85f, 0.85f);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(arm == Arm.RIGHT ? -18.0f : 18.0f));
            matrices.translate(arm == Arm.RIGHT ? 0.03 : -0.03, -0.03, 0.03);
        }
    }
}
