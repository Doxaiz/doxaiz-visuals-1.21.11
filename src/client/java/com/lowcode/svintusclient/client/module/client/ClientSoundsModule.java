package com.lowcode.svintusclient.client.module.client;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.util.SoundPreview;

public final class ClientSoundsModule extends Module {

    public final ModeSetting toggleSound = add(new ModeSetting("Звук переключения",
            "Soup (On/Off)", "Adaptive", "Клик", "Выкл"));
    public final NumberSetting volume = add(new NumberSetting("Громкость звуков", 0.7f, 0.1f, 1.0f, 0.05f));
    public final BooleanSetting guiSounds = add(new BooleanSetting("Звуки открытия меню", true));
    public final BooleanSetting sliderTick = add(new BooleanSetting("Звук слайдеров", true));
    public final BooleanSetting categoryChange = add(new BooleanSetting("Звук смены вкладок", true));

    public ClientSoundsModule() {
        super("Client Sounds", "Кастомные звуки интерфейса, кликов и переключения модулей", Category.SOUNDS);
        enableByDefault();
    }

    public void playToggle(boolean enabled) {
        if (!isEnabled() || toggleSound.is("Выкл")) return;
        String s = toggleSound.get();
        if ("Soup (On/Off)".equals(s)) {
            SoundPreview.playPreview(enabled ? "Soup On" : "Soup Off", volume.get());
        } else if ("Adaptive".equals(s)) {
            SoundPreview.playPreview(enabled ? "Adaptive Enable" : "Adaptive Disable", volume.get());
        } else if ("Клик".equals(s)) {
            SoundPreview.playPreview("Клик", volume.get());
        }
    }

    public void playGui(boolean open) {
        if (!isEnabled() || !guiSounds.get()) return;
        SoundPreview.playPreview(open ? "GUI Open" : "GUI Close", volume.get());
    }

    public void playSliderStep() {
        if (!isEnabled() || !sliderTick.get()) return;
        SoundPreview.playPreview("Slider Step", volume.get() * 0.75f);
    }

    public void playCategoryChange() {
        if (!isEnabled() || !categoryChange.get()) return;
        SoundPreview.playPreview("Blip", volume.get() * 0.85f);
    }
}
