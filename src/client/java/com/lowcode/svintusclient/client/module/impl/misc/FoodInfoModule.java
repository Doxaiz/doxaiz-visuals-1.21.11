package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;

public final class FoodInfoModule extends Module {

    public final BooleanSetting showSaturation = add(new BooleanSetting("Насыщение", true));
    public final BooleanSetting showHungerPreview = add(new BooleanSetting("Превью сытости", true));
    public final BooleanSetting showFoodHealing = add(new BooleanSetting("Прогноз исцеления", true));
    public final BooleanSetting numericValues = add(new BooleanSetting("Числовые значения", true));
    public final ColorSetting saturationColor = add(new ColorSetting("Цвет насыщения", 0xFFFFD700));

    public boolean shouldShowHealing() {
        return isEnabled() && showFoodHealing.get();
    }

    public boolean isNumeric() {
        return isEnabled() && numericValues.get();
    }

    public int getSaturationColor() {
        return saturationColor.getEffectiveColor();
    }

    public FoodInfoModule() {
        super("Сытость", "Отображение показателей насыщения (Saturation) и восполнения голода", Category.UTILITIES);
    }
}
