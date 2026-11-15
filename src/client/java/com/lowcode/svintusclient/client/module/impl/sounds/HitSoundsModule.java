package com.lowcode.svintusclient.client.module.impl.sounds;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.util.SoundPreview;

public final class HitSoundsModule extends Module {

    public final ModeSetting sound = add(new ModeSetting("Звук попадания",
            "AimBooster", "Metallic", "Rust", "Crime", "Колокольчик", "Bonk", "8-бит", "Пузырь",
            "Стекло", "Magic Pok", "Барабан", "Пластиковый мяч", "Squish", "Trinket", "Кирпич", "Поп", "Moan"));
    public final NumberSetting volume = add(new NumberSetting("Громкость", 0.85f, 0.1f, 1.0f, 0.05f));
    public final NumberSetting pitch = add(new NumberSetting("Тон (Pitch)", 1.0f, 0.5f, 1.5f, 0.05f));
    public final BooleanSetting pitchRandomize = add(new BooleanSetting("Случайный тон", false));
    public final BooleanSetting critOnly = add(new BooleanSetting("Только при крите", false));

    private long lastHitSoundTime = 0;

    public HitSoundsModule() {
        super("Hit Sounds", "Кастомные звуки при нанесении урона и попадании по противнику", Category.SOUNDS);
    }

    public void playHitSound(boolean isCrit) {
        if (!isEnabled()) return;
        if (critOnly.get() && !isCrit) return;

        long now = System.currentTimeMillis();
        if (now - lastHitSoundTime < 45L) return;
        lastHitSoundTime = now;

        float p = pitch.get();
        if (pitchRandomize.get()) {
            p += (float) ((Math.random() - 0.5) * 0.2);
        }
        SoundPreview.play(sound.get(), volume.get(), p);
    }

    public void playHitSound() {
        playHitSound(false);
    }
}
