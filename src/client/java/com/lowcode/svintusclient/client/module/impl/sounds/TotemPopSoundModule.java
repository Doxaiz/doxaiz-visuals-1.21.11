package com.lowcode.svintusclient.client.module.impl.sounds;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.util.SoundPreview;

public final class TotemPopSoundModule extends Module {

    public final ModeSetting sound = add(new ModeSetting("Звук сработки тотема",
            "Soup Pop", "Shape Totem", "Anime Totem", "Rimuru Totem", "Furry Totem",
            "Mita Totem", "Skull Totem", "SweetieFox Totem", "Gryaznaya Totem", "Final Blink", "Magic Pok", "Neptune", "8-бит", "Ванильный"));
    public final NumberSetting volume = add(new NumberSetting("Громкость", 0.9f, 0.1f, 1.0f, 0.05f));
    public final NumberSetting pitch = add(new NumberSetting("Тон (Pitch)", 1.0f, 0.5f, 1.5f, 0.05f));
    public final BooleanSetting pitchRandomize = add(new BooleanSetting("Случайный тон", false));
    public final BooleanSetting popCounterPitch = add(new BooleanSetting("Повышать тон с попом", true));

    private int popCount = 0;
    private long lastPopTime = 0;

    public TotemPopSoundModule() {
        super("Totem Pop Sounds", "Кастомные звуки при срабатывании Тотема бессмертия", Category.SOUNDS);
    }

    public void playPopSound() {
        if (!isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastPopTime < 15000L) {
            popCount++;
        } else {
            popCount = 1;
        }
        lastPopTime = now;

        float p = pitch.get();
        if (popCounterPitch.get()) {
            p = Math.min(1.8f, p + (popCount - 1) * 0.08f);
        } else if (pitchRandomize.get()) {
            p += (float) ((Math.random() - 0.5) * 0.15);
        }
        SoundPreview.play(sound.get(), volume.get(), p);
    }
}
