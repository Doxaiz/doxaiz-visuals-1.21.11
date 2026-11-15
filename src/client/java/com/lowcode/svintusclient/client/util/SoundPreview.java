package com.lowcode.svintusclient.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public final class SoundPreview {

    public static float previewVolume = 0.85f;

    private SoundPreview() {
    }

    public static boolean isSoundSetting(String settingName) {
        if (settingName == null) return false;
        String s = settingName.toLowerCase();
        return s.contains("звук") || s.contains("sound") || s.contains("аудио");
    }

    public static SoundEvent resolveSound(String name) {
        if (name == null || name.equalsIgnoreCase("Выкл") || name.equalsIgnoreCase("None")) return null;
        return switch (name) {
            case "Колокольчик", "Bell" -> SoundEvent.of(Identifier.of("doxaiz", "soup.bell"));
            case "Bonk" -> SoundEvent.of(Identifier.of("doxaiz", "soup.bonk"));
            case "8-бит", "8bit" -> SoundEvent.of(Identifier.of("doxaiz", "soup.8bit"));
            case "Пузырь", "Bubble" -> SoundEvent.of(Identifier.of("doxaiz", "soup.bubble"));
            case "AimBooster", "Hit Tick" -> SoundEvent.of(Identifier.of("doxaiz", "soup.aimbooster_hit"));
            case "Стекло", "Glass" -> SoundEvent.of(Identifier.of("doxaiz", "soup.glass"));
            case "Magic Pok" -> SoundEvent.of(Identifier.of("doxaiz", "soup.magic_pok"));
            case "Барабан", "Drum" -> SoundEvent.of(Identifier.of("doxaiz", "soup.drum_bass"));
            case "Пластиковый мяч", "Plastic Ball" -> SoundEvent.of(Identifier.of("doxaiz", "soup.plastic_ball"));
            case "Squish" -> SoundEvent.of(Identifier.of("doxaiz", "soup.squish"));
            case "Trinket" -> SoundEvent.of(Identifier.of("doxaiz", "soup.trinket_hit"));
            case "Кирпич", "Brick" -> SoundEvent.of(Identifier.of("doxaiz", "soup.brick"));
            case "Клик", "Click" -> SoundEvent.of(Identifier.of("doxaiz", "soup.click"));
            case "Blip" -> SoundEvent.of(Identifier.of("doxaiz", "soup.blip"));
            case "Поп", "Soup Pop" -> SoundEvent.of(Identifier.of("doxaiz", "soup.pop"));
            case "Final Pok" -> SoundEvent.of(Identifier.of("doxaiz", "soup.final_pok"));
            case "Headshot Rust" -> SoundEvent.of(Identifier.of("doxaiz", "soup.rust_headshot"));
            case "Neptune" -> SoundEvent.of(Identifier.of("doxaiz", "soup.neptune"));
            case "Abmiss" -> SoundEvent.of(Identifier.of("doxaiz", "soup.abmiss"));
            case "Whii" -> SoundEvent.of(Identifier.of("doxaiz", "soup.whii"));
            case "Critow" -> SoundEvent.of(Identifier.of("doxaiz", "soup.critow"));
            case "Final Blink" -> SoundEvent.of(Identifier.of("doxaiz", "soup.final_blink"));
            case "Soup On" -> SoundEvent.of(Identifier.of("doxaiz", "soup.on"));
            case "Soup Off" -> SoundEvent.of(Identifier.of("doxaiz", "soup.off"));
            case "GUI Open" -> SoundEvent.of(Identifier.of("doxaiz", "soup.gui_open"));
            case "GUI Close" -> SoundEvent.of(Identifier.of("doxaiz", "soup.gui_close"));
            case "Slider Step" -> SoundEvent.of(Identifier.of("doxaiz", "soup.slider_step"));
            case "Adaptive GUI" -> SoundEvent.of(Identifier.of("doxaiz", "adaptive.clickgui"));
            case "Adaptive Enable" -> SoundEvent.of(Identifier.of("doxaiz", "adaptive.module_enable"));
            case "Adaptive Disable" -> SoundEvent.of(Identifier.of("doxaiz", "adaptive.module_disable"));
            case "Shape Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.shape"));
            case "Anime Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.anime"));
            case "Rimuru Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.rimuru"));
            case "Furry Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.furry"));
            case "Mita Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.mita"));
            case "Omni Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.omni"));
            case "Skull Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.skull"));
            case "SweetieFox Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.sweetiefox"));
            case "Gryaznaya Totem" -> SoundEvent.of(Identifier.of("doxaiz", "shape.totem.gryaznaya"));
            case "Metallic" -> SoundEvent.of(Identifier.of("doxaiz", "shape.hitsound.metallic"));
            case "Rust" -> SoundEvent.of(Identifier.of("doxaiz", "shape.hitsound.rust"));
            case "Crime" -> SoundEvent.of(Identifier.of("doxaiz", "shape.hitsound.crime"));
            case "Moan" -> SoundEvent.of(Identifier.of("doxaiz", "shape.hitsound.moan" + (1 + (int)(Math.random() * 4))));
            case "Молния" -> SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER;
            case "Опыт", "Сфера опыта" -> SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP;
            case "Крит" -> SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
            case "Ванильный" -> SoundEvents.ITEM_TOTEM_USE;
            default -> null;
        };
    }

    public static void play(String soundName, float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        SoundEvent event = resolveSound(soundName);
        if (event != null) {
            float vol = Math.max(0.05f, Math.min(1.0f, volume));
            float pit = Math.max(0.5f, Math.min(2.0f, pitch));
            mc.getSoundManager().play(PositionedSoundInstance.ambient(event, pit, vol));
        }
    }

    public static void playPreview(String soundName, float volume) {
        play(soundName, volume, 1.0f);
    }
}
