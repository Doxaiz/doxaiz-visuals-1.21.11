package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class ShulkerPreviewModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль окна", "Стеклянный (Glass)", "Тёмный (Dark)", "Неоновый (Neon)"));
    public final BooleanSetting showEmptySlots = add(new BooleanSetting("Сетка пустых ячеек", true));
    public final BooleanSetting showTitle = add(new BooleanSetting("Заголовок контейнера", true));
    public final BooleanSetting showTotalItems = add(new BooleanSetting("Общее число предметов", true));
    public final NumberSetting previewScale = add(new NumberSetting("Масштаб окна", 1.0f, 0.7f, 1.4f, 0.05f));
    public final BooleanSetting lockKeyHold = add(new BooleanSetting("Показ только по Shift", false));

    public float getScale() {
        return isEnabled() ? previewScale.get() : 1.0f;
    }

    public boolean shouldDisplay(boolean shiftHeld) {
        if (!isEnabled()) return false;
        return !lockKeyHold.get() || shiftHeld;
    }

    public ShulkerPreviewModule() {
        super("Shulker Peek", "Окно предпросмотра содержимого шалкера при наведении курсора", Category.UTILITIES);
        enableByDefault();
    }
}
