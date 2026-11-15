package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class NoHurtCamModule extends Module {

    public final NumberSetting intensity = add(new NumberSetting("Интенсивность тряски %", 0.0f, 0.0f, 100.0f, 5.0f));
    public final BooleanSetting hurtFlash = add(new BooleanSetting("Красная вспышка", false));
    public final ColorSetting flashColor = add(new ColorSetting("Цвет вспышки", 0x33FF0000));

    public boolean shouldFlash() {
        return isEnabled() && hurtFlash.get();
    }

    public int getFlashColor() {
        return flashColor.getEffectiveColor();
    }

    public NoHurtCamModule() {
        super("No Hurt Cam", "Отключение или ослабление тряски экрана при получении урона", Category.UTILITIES);
    }
}
