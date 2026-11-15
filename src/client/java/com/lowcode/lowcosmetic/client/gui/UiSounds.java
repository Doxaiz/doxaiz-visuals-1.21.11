package com.lowcode.lowcosmetic.client.gui;

import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Util;

public enum UiSounds {
    OPEN("ui.open", 0.55F),
    CLOSE("ui.close", 0.45F),
    CLICK("ui.click", 0.5F),
    HOVER("ui.hover", 0.18F),
    EQUIP("ui.equip", 0.6F),
    UNEQUIP("ui.unequip", 0.55F),
    CATEGORY("ui.category", 0.4F),
    TYPING("ui.typing", 0.25F);

    private final SoundEvent event;
    private final float volume;
    private long lastPlayed;

    UiSounds(String path, float volume) {
        this.event = SoundEvent.of(LowcosmeticClient.id(path));
        this.volume = volume;
    }

    public void play() {
        play(1.0F);
    }

    public void play(float pitch) {
        if (!UiSettings.sounds) {
            return;
        }
        long now = Util.getMeasuringTimeMs();
        if (now - lastPlayed < 35L) {
            return;
        }
        lastPlayed = now;
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(event, pitch, volume));
    }
}
