package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class InventoryHudModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль слотов",
            "Стекло (Matrix)", "Минимализм", "Компактный", "Неон"));
    public final BooleanSetting showEmptySlots = add(new BooleanSetting("Отображать сетку ячеек", true));
    public final NumberSetting opacity = add(new NumberSetting("Прозрачность стекла", 0.65f, 0.2f, 1.0f, 0.05f));
    public final BooleanSetting showItemCounts = add(new BooleanSetting("Количество предметов", true));
    public final BooleanSetting highlightSelected = add(new BooleanSetting("Подсветка предметов", true));

    public boolean shouldHighlightSelected() {
        return isEnabled() && highlightSelected.get();
    }

    public InventoryHudModule() {
        super("Inventory HUD", "Отображение содержимого инвентаря на экране без открытия меню", Category.INTERFACE);
    }

    @Override
    public void onEnable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.inventory.set(true);
        }
    }

    @Override
    public void onDisable() {
        if (ModuleManager.HUD != null) {
            ModuleManager.HUD.inventory.set(false);
        }
    }
}
